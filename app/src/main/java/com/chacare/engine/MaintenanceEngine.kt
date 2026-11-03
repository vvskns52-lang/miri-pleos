package com.chacare.engine

import com.chacare.data.MaintenanceRepository
import com.chacare.data.RecordSource
import com.chacare.data.RecordType
import com.chacare.data.ServiceRecord
import com.chacare.domain.AlertRecord
import com.chacare.domain.Answer
import com.chacare.domain.MAX_ASKS
import com.chacare.domain.Part
import com.chacare.domain.Prompt
import com.chacare.domain.SHORT_TRIP_KM
import com.chacare.domain.SensorChangeDetector
import com.chacare.domain.SensorKind
import com.chacare.domain.Shop
import com.chacare.domain.ShopVisitDetector
import com.chacare.domain.UsageStats
import com.chacare.domain.parseAnswer
import com.chacare.domain.parseParts
import com.chacare.domain.planPrompts
import com.chacare.domain.shouldAlert
import com.chacare.domain.toPending
import com.chacare.location.LocationSource
import com.chacare.vehicle.VehicleSignals
import com.chacare.voice.VoiceIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 차량 신호를 운행 기록으로 쌓고, 교체 징후를 감지하고, 정차 중에 음성으로 묻는다.
 * 내부 상태를 여러 코루틴이 공유하므로 반드시 단일 스레드 디스패처의 [CoroutineScope]에서 시작한다.
 */
class MaintenanceEngine(
    private val vehicle: VehicleSignals,
    private val location: LocationSource,
    private val voice: VoiceIO,
    private val repo: MaintenanceRepository,
    private val shops: List<Shop>,
    private val clock: () -> Long,
) {
    private var job: Job? = null
    private var promptJob: Job? = null
    private var promptsDoneThisCycle = false
    private var workScope: CoroutineScope? = null
    private var isParked = false

    private lateinit var sensors: SensorChangeDetector
    private var driving = false
    private var tripStartMillis = 0L
    private var tripKm = 0.0
    private var wiperSince: Long? = null
    private var hvacSince: Long? = null

    fun start(scope: CoroutineScope) {
        vehicle.start()
        location.start()
        job = scope.launch {
            sensors = SensorChangeDetector(repo.sensorSnapshot())
            val shopDetector = ShopVisitDetector(shops, repo.dwellState())
            var lastOdo = repo.lastOdometer()

            launch {
                vehicle.odometerKm.collect { v ->
                    if (v == null) return@collect
                    val prev = lastOdo
                    if (prev != null && v <= prev) return@collect
                    lastOdo = v
                    if (prev != null) {
                        tripKm += v - prev
                        repo.addUsage(UsageStats(drivenKm = v - prev))
                    }
                    repo.saveOdometer(v)
                }
            }
            launch {
                vehicle.brakePressed.distinctUntilChanged().collect { if (it) repo.addUsage(UsageStats(brakePresses = 1)) }
            }
            launch {
                vehicle.wiperOn.distinctUntilChanged().collect { on ->
                    wiperSince = trackMinutes(on, wiperSince) { repo.addUsage(UsageStats(wiperMinutes = it)) }
                }
            }
            launch {
                vehicle.hvacOn.distinctUntilChanged().collect { on ->
                    hvacSince = trackMinutes(on, hvacSince) { repo.addUsage(UsageStats(hvacMinutes = it)) }
                }
            }
            launch {
                vehicle.sensors.collect { (kind, state) ->
                    val recovery = sensors.onSignal(kind, state, clock())
                    repo.saveSensorSnapshot(sensors.snapshot())
                    if (recovery == null) return@collect
                    val pending = recovery.toPending()
                    when {
                        pending != null -> repo.putPending(pending)
                        kind == SensorKind.WASHER_WARNING -> record(Part.WASHER_FLUID, RecordType.REFILLED, RecordSource.SENSOR_VOICE)
                    }
                }
            }
            launch {
                location.locations.collect { (lat, lon) ->
                    val before = shopDetector.state()
                    val visit = shopDetector.onLocation(lat, lon, clock())
                    if (shopDetector.state() != before) repo.saveDwell(shopDetector.state())
                    if (visit != null) repo.setPendingShopVisit(visit)
                }
            }
            workScope = this
            launch {
                vehicle.parked.distinctUntilChanged().collect { parked ->
                    isParked = parked
                    if (parked) onParked(this@launch) else onDriving()
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        workScope = null
        voice.stop()
        vehicle.stop()
        location.stop()
    }

    /** 시동을 새로 건 것으로 본다(데모 패널의 "새 시동"). P단이면 실제 시동처럼 바로 묻는다. */
    fun newIgnitionCycle() {
        val scope = workScope ?: return
        scope.launch {
            promptsDoneThisCycle = false
            if (isParked) onParked(scope)
        }
    }

    private suspend fun trackMinutes(on: Boolean, since: Long?, add: suspend (Double) -> Unit): Long? {
        if (on) return since ?: clock()
        if (since != null) add((clock() - since) / 60_000.0)
        return null
    }

    private fun onDriving() {
        if (promptJob?.isActive == true) {
            promptJob?.cancel()
            voice.stop()
        }
        if (!driving) {
            driving = true
            tripStartMillis = clock()
            tripKm = 0.0
        }
    }

    private suspend fun onParked(scope: CoroutineScope) {
        if (driving) {
            driving = false
            repo.addUsage(
                UsageStats(
                    drivingMinutes = (clock() - tripStartMillis) / 60_000.0,
                    totalTrips = 1,
                    shortTrips = if (tripKm < SHORT_TRIP_KM) 1 else 0,
                ),
            )
        }
        if (promptsDoneThisCycle || promptJob?.isActive == true) return
        promptJob = scope.launch {
            runPrompts()
            promptsDoneThisCycle = true
        }
    }

    private suspend fun runPrompts() {
        val now = clock()
        val odo = repo.lastOdometer() ?: 0.0
        val alerts = repo.estimates().first()
            .filter { shouldAlert(it.status, repo.lastAlert(it.part), now, odo) }
            .map { it.part }
        val plan = planPrompts(
            repo.pendingShopVisit(), repo.pendings(), alerts,
            tireLow = sensors.isLow(SensorKind.TIRE_PRESSURE_WARNING),
            washerLow = sensors.isLow(SensorKind.WASHER_WARNING),
        )
        for (prompt in plan) {
            voice.speak(prompt.text)
            when (prompt) {
                is Prompt.AskShop -> {
                    val parts = parseParts(voice.listen())
                    parts.forEach {
                        record(it, RecordType.REPLACED, RecordSource.SHOP_VOICE)
                        repo.removePending(it)
                    }
                    repo.setPendingShopVisit(null)
                }
                is Prompt.AskSensor -> {
                    val part = prompt.pending.part
                    if (repo.pendings().none { it.part == part }) continue // 정비소 답변에서 이미 처리됨
                    when (parseAnswer(voice.listen())) {
                        Answer.REPLACED -> { record(part, RecordType.REPLACED, RecordSource.SENSOR_VOICE); repo.removePending(part) }
                        Answer.REFILLED -> { record(part, RecordType.REFILLED, RecordSource.SENSOR_VOICE); repo.removePending(part) }
                        Answer.NO -> repo.removePending(part)
                        Answer.UNKNOWN ->
                            if (prompt.pending.askCount + 1 >= MAX_ASKS) repo.removePending(part) else repo.bumpAsk(part)
                    }
                }
                is Prompt.Alert -> repo.saveAlert(AlertRecord(prompt.part, now, odo))
                Prompt.TireLow, Prompt.WasherLow -> Unit
            }
        }
    }

    private suspend fun record(part: Part, type: RecordType, source: RecordSource) {
        repo.addRecord(ServiceRecord(part = part, type = type, date = today(), odometerKm = repo.lastOdometer() ?: 0.0, source = source))
    }

    private fun today(): LocalDate = Instant.ofEpochMilli(clock()).atZone(ZoneId.systemDefault()).toLocalDate()
}

package com.chacare.engine

import com.chacare.data.MaintenanceRepository
import com.chacare.data.RecordSource
import com.chacare.data.RecordType
import com.chacare.data.ServiceRecord
import com.chacare.domain.AlertRecord
import com.chacare.domain.Answer
import com.chacare.domain.IntervalKind
import com.chacare.domain.MAX_ASKS
import com.chacare.domain.Part
import com.chacare.domain.Prompt
import com.chacare.domain.SHORT_TRIP_KM
import com.chacare.domain.SensorChangeDetector
import com.chacare.domain.SensorKind
import com.chacare.domain.Shop
import com.chacare.domain.ShopVisitDetector
import com.chacare.domain.UsageStats
import com.chacare.domain.WarningLight
import com.chacare.domain.activeWarnings
import com.chacare.domain.announceWarning
import com.chacare.domain.newlyLit
import kotlinx.coroutines.flow.combine
import com.chacare.data.CustomEstimate
import com.chacare.domain.CustomAnswer
import com.chacare.domain.answerQuery
import com.chacare.domain.bundleOf
import com.chacare.domain.displayKm
import com.chacare.domain.displayMonths
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.chacare.domain.mentions
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

/** "물어보기" 진행 상태(화면 표시용). */
sealed interface QaState {
    data object Listening : QaState
    data class Answered(val question: String?, val answer: String) : QaState
    /** 앱이 먼저 말한 안내·질문과, 질문이었다면 들은 대답(못 들었으면 빈 문자열). */
    data class Said(val text: String, val reply: String? = null) : QaState
}

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
    private val qaState: MutableStateFlow<QaState?> = MutableStateFlow(null),
    private val warningState: MutableStateFlow<List<WarningLight>> = MutableStateFlow(emptyList()),
    private val clock: () -> Long,
) {
    val qa: StateFlow<QaState?> = qaState
    /** 지금 안내 중인 경고등(급한 순). */
    val warnings: StateFlow<List<WarningLight>> = warningState
    private var announcedWarnings = emptySet<WarningLight>()
    private var warningJob: Job? = null
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
            launch {
                combine(vehicle.warningLights, vehicle.parked) { lit, parked -> activeWarnings(lit, parked) }
                    .distinctUntilChanged()
                    .collect { active -> onWarnings(active, this@launch) }
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

    /**
     * 운전자가 묻고 앱이 답한다. 말로만 주고받으므로 주행 중에도 쓸 수 있다.
     * [typed]가 있으면 듣기를 건너뛴다(데모 패널의 예시 질문).
     */
    fun ask(typed: String? = null) {
        val scope = workScope ?: return
        scope.launch {
            if (promptJob?.isActive == true) return@launch // 정차 안내 중에는 끼어들지 않는다
            val question = if (typed != null) typed else {
                qaState.value = QaState.Listening
                voice.speak("무엇이 궁금하세요?")
                voice.listen()
            }
            val customs = repo.customEstimates().first().map { CustomAnswer(it.part.name, customPhrase(it)) }
            val answer = answerQuery(
                question, repo.estimates().first(), repo.specs().first(), customs, repo.costs().first(), warningState.value,
            )
            qaState.value = QaState.Answered(question, answer)
            voice.speak(answer)
        }
    }

    /**
     * 새로 켜진 경고등은 주행 중이어도 바로 말한다(화면을 보지 않아도 되게). 정비 안내가 진행 중이면 끊고 먼저 말한다.
     * 꺼진 경고등은 알린 목록에서 빼서, 다시 켜지면 다시 알린다.
     */
    private suspend fun onWarnings(active: List<WarningLight>, scope: CoroutineScope) {
        warningState.value = active
        val fresh = newlyLit(active, announcedWarnings)
        announcedWarnings = announcedWarnings.intersect(active.toSet()) + fresh
        if (fresh.isEmpty()) return
        repo.logWarnings(fresh, today(), repo.lastOdometer() ?: 0.0)
        if (promptJob?.isActive == true) {
            promptJob?.cancel()
            voice.stop()
        }
        val previous = warningJob
        warningJob = scope.launch {
            previous?.join()
            fresh.forEach {
                val text = announceWarning(it)
                qaState.value = QaState.Said(text)
                voice.speak(text)
            }
        }
    }

    private fun customPhrase(c: CustomEstimate): String = when {
        c.status == null -> c.last?.let { "마지막으로 ${it.date}에 교체했어요" } ?: "아직 교체 기록이 없어요"
        c.remainingRatio == null -> "교체 기록이 없어서 남은 수명을 알 수 없어요"
        (c.remainingKm ?: 1.0) <= 0.0 || (c.remainingMonths ?: 1.0) <= 0.0 -> "교체 시기가 지났어요"
        else -> "교체까지 " + listOfNotNull(c.remainingKm?.let(::displayKm), c.remainingMonths?.let(::displayMonths)).joinToString(", ") + " 남았어요"
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
            warningJob?.join() // 경고등 안내를 다 말한 뒤에 정비 안내를 시작한다
            runPrompts()
            promptsDoneThisCycle = true
        }
    }

    private suspend fun runPrompts() {
        val now = clock()
        val odo = repo.lastOdometer() ?: 0.0
        val estimates = repo.estimates().first()
        val due = estimates.filter { shouldAlert(it.status, repo.lastAlert(it.part), now, odo) }
        val plan = planPrompts(
            repo.pendingShopVisit(), repo.pendings(), due.map { it.part },
            tireLow = sensors.isLow(SensorKind.TIRE_PRESSURE_WARNING),
            washerLow = sensors.isLow(SensorKind.WASHER_WARNING),
            inspectOnly = due.filter { it.kind == IntervalKind.INSPECT }.map { it.part }.toSet(),
            severeNotice = repo.severeToAnnounce(),
            bundleExtras = bundleOf(estimates).map { it.part },
        )
        // 화면에도 같은 말을 띄워 소리를 놓쳐도 알 수 있게 한다.
        suspend fun hear(text: String): String? = voice.listen().also { qaState.value = QaState.Said(text, it.orEmpty()) }
        for (prompt in plan) {
            qaState.value = QaState.Said(prompt.text)
            voice.speak(prompt.text)
            when (prompt) {
                is Prompt.AskShop -> {
                    val answer = hear(prompt.text)
                    parseParts(answer).forEach {
                        record(it, RecordType.REPLACED, RecordSource.SHOP_VOICE)
                        repo.removePending(it)
                    }
                    repo.customParts().filter { mentions(it.name, answer) }.forEach {
                        repo.addCustomRecord(it.id, today(), repo.lastOdometer() ?: 0.0, "정비소 방문 · 음성 답변")
                    }
                    repo.setPendingShopVisit(null)
                }
                is Prompt.AskSensor -> {
                    val part = prompt.pending.part
                    if (repo.pendings().none { it.part == part }) continue // 정비소 답변에서 이미 처리됨
                    when (parseAnswer(hear(prompt.text))) {
                        Answer.REPLACED -> { record(part, RecordType.REPLACED, RecordSource.SENSOR_VOICE); repo.removePending(part) }
                        Answer.REFILLED -> { record(part, RecordType.REFILLED, RecordSource.SENSOR_VOICE); repo.removePending(part) }
                        Answer.NO -> repo.removePending(part)
                        Answer.UNKNOWN ->
                            if (prompt.pending.askCount + 1 >= MAX_ASKS) repo.removePending(part) else repo.bumpAsk(part)
                    }
                }
                is Prompt.Alert -> repo.saveAlert(AlertRecord(prompt.part, now, odo))
                is Prompt.SevereNotice -> repo.markSevereAnnounced(prompt.reasons)
                is Prompt.BundleTip -> Unit
                Prompt.TireLow, Prompt.WasherLow -> Unit
            }
        }
    }

    private suspend fun record(part: Part, type: RecordType, source: RecordSource) {
        repo.addRecord(ServiceRecord(part = part, type = type, date = today(), odometerKm = repo.lastOdometer() ?: 0.0, source = source))
    }

    private fun today(): LocalDate = Instant.ofEpochMilli(clock()).atZone(ZoneId.systemDefault()).toLocalDate()
}

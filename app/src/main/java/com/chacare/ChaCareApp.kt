package com.chacare

import android.app.Application
import com.chacare.data.MaintenanceRepository
import com.chacare.engine.MaintenanceEngine
import com.chacare.engine.QaState
import com.chacare.domain.WarningLight
import com.chacare.location.DEMO_HOME_LAT
import com.chacare.location.DEMO_HOME_LON
import com.chacare.location.DEMO_SHOPS
import com.chacare.location.DemoLocationSource
import com.chacare.location.PleosLocationSource
import com.chacare.vehicle.DemoVehicleSignals
import com.chacare.vehicle.PleosVehicleSignals
import com.chacare.vehicle.VehicleSignals
import com.chacare.voice.PleosVoiceIO
import com.chacare.voice.ScriptedVoiceIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

class ChaCareApp : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
    }
}

/** 앱 전체가 함께 쓰는 객체들. 데모 모드를 켜고 끄면 엔진을 가짜/실제 차량으로 바꿔 다시 시작한다. */
@OptIn(ExperimentalCoroutinesApi::class)
class AppGraph(private val app: Application) {
    /** 데모 패널의 "30분 경과" 등으로 앞당긴 시간. */
    private val demoOffsetMillis = MutableStateFlow(0L)
    val clock: () -> Long = { System.currentTimeMillis() + demoOffsetMillis.value }

    val repo = MaintenanceRepository(app.filesDir.resolve("chacare.json")) {
        Instant.ofEpochMilli(clock()).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    val demoVehicle = DemoVehicleSignals(startOdometerKm = 30_000.0, parked = true)
    val demoLocation = DemoLocationSource().apply { moveTo(DEMO_HOME_LAT, DEMO_HOME_LON) }
    private val realVehicle by lazy { PleosVehicleSignals(app) }
    private val realLocation by lazy { PleosLocationSource(app) }
    /** 시연 패널에서 고른 "다음 대답"(마이크 없는 에뮬레이터용). */
    private val nextAnswerState = MutableStateFlow<String?>(null)
    val nextAnswer: StateFlow<String?> = nextAnswerState
    fun setNextAnswer(text: String?) { nextAnswerState.value = text }
    private val voice by lazy { ScriptedVoiceIO(PleosVoiceIO(app), nextAnswerState) }

    /** 엔진 내부 상태는 한 스레드에서만 다룬다. */
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val currentVehicle = MutableStateFlow<VehicleSignals>(demoVehicle)
    private var engine: MaintenanceEngine? = null
    private var watchJob: Job? = null

    /** 화면의 주행 중 잠금용. */
    val parked: StateFlow<Boolean> = currentVehicle.flatMapLatest { it.parked }.distinctUntilChanged()
        .stateIn(appScope, SharingStarted.Eagerly, true)

    fun startEngine() {
        if (watchJob != null) return
        watchJob = appScope.launch {
            repo.demoMode.distinctUntilChanged().collect { demo -> restart(demo) }
        }
    }

    fun stopEngine() {
        watchJob?.cancel()
        watchJob = null
        engine?.stop()
        engine = null
    }

    private suspend fun restart(demo: Boolean) {
        engine?.stop()
        // 가짜 차 주행거리가 저장된 값과 어긋나면 그 차이가 통째로 운행 기록에 쌓이므로 맞춰 둔다.
        // 에뮬레이터의 실제 차량 값은 0km라, 0이면 데모 기본값으로 시작하고 저장해 둔다.
        if (demo) {
            val start = repo.lastOdometer()?.takeIf { it > 0 } ?: 30_000.0
            demoVehicle.setOdometer(start)
            repo.saveOdometer(start)
        }
        val vehicle = if (demo) demoVehicle else realVehicle
        val location = if (demo) demoLocation else realLocation
        currentVehicle.value = vehicle
        engine = MaintenanceEngine(vehicle, location, voice, repo, DEMO_SHOPS, qaState, warningState, clock).also { it.start(engineScope) }
    }

    // --- 물어보기 ---

    /** 엔진이 바뀌어도(데모 전환) 화면이 같은 상태를 보도록 여기서 들고 있는다. */
    private val qaState = MutableStateFlow<QaState?>(null)
    val qa: StateFlow<QaState?> = qaState

    // --- 경고등 ---

    private val warningState = MutableStateFlow<List<WarningLight>>(emptyList())
    val warnings: StateFlow<List<WarningLight>> = warningState

    fun ask(typed: String? = null) {
        engine?.ask(typed)
    }

    fun dismissQa() {
        qaState.value = null
    }

    // --- 데모 패널 ---

    fun advanceTime(minutes: Long) {
        demoOffsetMillis.value += minutes * 60_000L
        // 정비소 체류 시간은 위치가 들어올 때 계산되므로 같은 자리를 한 번 더 알린다.
        demoLocation.locations.replayCache.lastOrNull()?.let { (lat, lon) -> demoLocation.moveTo(lat, lon) }
    }

    fun today() = Instant.ofEpochMilli(clock()).atZone(ZoneId.systemDefault()).toLocalDate()

    fun newIgnitionCycle() {
        engine?.newIgnitionCycle()
    }
}

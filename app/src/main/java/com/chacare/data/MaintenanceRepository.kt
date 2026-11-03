package com.chacare.data

import com.chacare.domain.AlertRecord
import com.chacare.domain.DwellState
import com.chacare.domain.LastService
import com.chacare.domain.LifeEstimate
import com.chacare.domain.LifeStatus
import com.chacare.domain.Part
import com.chacare.domain.PendingConfirmation
import com.chacare.domain.Powertrain
import com.chacare.domain.SensorKind
import com.chacare.domain.ShopVisit
import com.chacare.domain.SignalState
import com.chacare.domain.UsageStats
import com.chacare.domain.correctionFactor
import com.chacare.domain.estimate
import com.chacare.domain.specsFor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate

enum class RecordType { REPLACED, REFILLED }

enum class RecordSource { SENSOR_VOICE, SHOP_VOICE, MANUAL, ONBOARDING }

@Serializable
data class ServiceRecord(
    val id: Long = 0,
    val part: Part,
    val type: RecordType,
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    val odometerKm: Double,
    val source: RecordSource,
)

/** 앱이 기억하는 전부. 데이터가 작아서 JSON 파일 하나로 충분하다. */
@Serializable
private data class StoreData(
    val nextId: Long = 1,
    val records: List<ServiceRecord> = emptyList(),
    val usageTotal: UsageStats = UsageStats(),
    val usageAtReplace: Map<Part, UsageStats> = emptyMap(),
    val pendings: List<PendingConfirmation> = emptyList(),
    val dwell: DwellState? = null,
    val pendingShopVisit: ShopVisit? = null,
    val sensorSnapshot: Map<SensorKind, SignalState> = emptyMap(),
    val alerts: Map<Part, AlertRecord> = emptyMap(),
    val lastOdometer: Double? = null,
    val powertrain: Powertrain? = null,
    val onboarded: Boolean = false,
    val demoMode: Boolean = false,
)

private object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeLong(value.toEpochDay())
    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.ofEpochDay(decoder.decodeLong())
}

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

class MaintenanceRepository(private val file: File, private val today: () -> LocalDate = LocalDate::now) {
    private val mutex = Mutex()
    private val state = MutableStateFlow(load())

    private fun load(): StoreData =
        runCatching { json.decodeFromString<StoreData>(file.readText()) }.getOrDefault(StoreData())

    private suspend fun update(change: (StoreData) -> StoreData) = mutex.withLock {
        val next = change(state.value)
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(json.encodeToString(StoreData.serializer(), next))
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
        state.value = next
    }

    // --- 교체·보충 기록 ---

    fun records(part: Part): Flow<List<ServiceRecord>> =
        state.map { s -> s.records.filter { it.part == part }.sortedByDescending { it.date } }

    suspend fun addRecord(r: ServiceRecord) = update { s ->
        val usageAtReplace = if (r.type == RecordType.REPLACED) s.usageAtReplace + (r.part to s.usageTotal) else s.usageAtReplace
        s.copy(nextId = s.nextId + 1, records = s.records + r.copy(id = s.nextId), usageAtReplace = usageAtReplace)
    }

    suspend fun updateRecord(r: ServiceRecord) = update { s -> s.copy(records = s.records.map { if (it.id == r.id) r else it }) }

    suspend fun deleteRecord(id: Long) = update { s -> s.copy(records = s.records.filterNot { it.id == id }) }

    /** 보충(REFILLED)은 수명을 되돌리지 않는다. */
    suspend fun lastReplacement(part: Part): LastService? = lastReplacement(state.value, part)

    private fun lastReplacement(s: StoreData, part: Part): LastService? =
        s.records.filter { it.part == part && it.type == RecordType.REPLACED }
            .maxWithOrNull(compareBy({ it.date }, { it.odometerKm }))
            ?.let { LastService(it.date, it.odometerKm) }

    // --- 운행 누적 ---

    suspend fun addUsage(delta: UsageStats) = update { s -> s.copy(usageTotal = s.usageTotal + delta) }

    suspend fun usageSince(part: Part): UsageStats = usageSince(state.value, part)

    private fun usageSince(s: StoreData, part: Part): UsageStats = s.usageTotal - (s.usageAtReplace[part] ?: UsageStats())

    // --- 확인 대기(방식 ①) ---

    suspend fun pendings(): List<PendingConfirmation> = state.value.pendings

    suspend fun putPending(p: PendingConfirmation) = update { s -> s.copy(pendings = s.pendings.filterNot { it.part == p.part } + p) }

    suspend fun removePending(part: Part) = update { s -> s.copy(pendings = s.pendings.filterNot { it.part == part }) }

    suspend fun bumpAsk(part: Part) = update { s ->
        s.copy(pendings = s.pendings.map { if (it.part == part) it.copy(askCount = it.askCount + 1) else it })
    }

    // --- 정비소 방문(방식 ②) ---

    suspend fun dwellState(): DwellState? = state.value.dwell

    suspend fun saveDwell(d: DwellState?) = update { it.copy(dwell = d) }

    suspend fun pendingShopVisit(): ShopVisit? = state.value.pendingShopVisit

    suspend fun setPendingShopVisit(v: ShopVisit?) = update { it.copy(pendingShopVisit = v) }

    // --- 센서·알림·주행거리 ---

    suspend fun sensorSnapshot(): Map<SensorKind, SignalState> = state.value.sensorSnapshot

    suspend fun saveSensorSnapshot(m: Map<SensorKind, SignalState>) = update { it.copy(sensorSnapshot = m) }

    suspend fun lastAlert(part: Part): AlertRecord? = state.value.alerts[part]

    suspend fun saveAlert(a: AlertRecord) = update { s -> s.copy(alerts = s.alerts + (a.part to a)) }

    suspend fun lastOdometer(): Double? = state.value.lastOdometer

    suspend fun saveOdometer(km: Double) = update { it.copy(lastOdometer = km) }

    val odometer: Flow<Double?> = state.map { it.lastOdometer }

    // --- 설정 ---

    suspend fun powertrain(): Powertrain? = state.value.powertrain

    suspend fun setPowertrain(p: Powertrain) = update { it.copy(powertrain = p) }

    suspend fun onboarded(): Boolean = state.value.onboarded

    suspend fun setOnboarded() = update { it.copy(onboarded = true) }

    val demoMode: Flow<Boolean> = state.map { it.demoMode }

    suspend fun setDemoMode(on: Boolean) = update { it.copy(demoMode = on) }

    // --- 화면용 계산 ---

    fun factor(part: Part): Flow<Double> = state.map { correctionFactor(part, usageSince(it, part)) }

    /** 급한 순: 교체 필요 → 점검 권장 → 좋음, 같은 단계에서는 이력 모름 → 남은 비율 적은 순. */
    fun estimates(): Flow<List<LifeEstimate>> = state.map { s ->
        val odo = s.lastOdometer ?: 0.0
        val washerLow = s.sensorSnapshot[SensorKind.WASHER_WARNING] == SignalState.LOW
        specsFor(s.powertrain ?: Powertrain.ICE).map { spec ->
            estimate(
                spec, lastReplacement(s, spec.part), odo, today(),
                correctionFactor(spec.part, usageSince(s, spec.part)),
                sensorWarning = spec.part == Part.WASHER_FLUID && washerLow,
            )
        }.sortedWith(compareBy({ urgency(it.status) }, { it.remainingRatio ?: Double.NEGATIVE_INFINITY }))
    }

    private fun urgency(s: LifeStatus) = when (s) {
        LifeStatus.REPLACE -> 0
        LifeStatus.CHECK -> 1
        LifeStatus.GOOD -> 2
    }
}

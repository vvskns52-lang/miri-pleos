package com.chacare.domain

import kotlinx.serialization.Serializable

/** 경고가 켜졌거나 양이 부족하면 LOW. */
enum class SignalState { LOW, NORMAL, UNKNOWN }

@Serializable
data class PendingConfirmation(val part: Part, val detectedAtMillis: Long, val askCount: Int = 0)

/** 부족 → 정상으로 돌아온 순간. */
data class Recovery(val kind: SensorKind, val part: Part, val atMillis: Long)

/** 타이어(공기 보충)와 워셔액(보충형)은 운전자에게 묻지 않는다. */
fun Recovery.toPending(): PendingConfirmation? = when (kind) {
    SensorKind.TIRE_PRESSURE_WARNING, SensorKind.WASHER_WARNING -> null
    else -> PendingConfirmation(part, atMillis)
}

fun SensorKind.part(): Part = DEFAULT_SPECS.single { it.sensor == this }.part

/** 설계서 4.3 ①: 센서 값이 LOW → NORMAL로 바뀌면 교체·보충했을 가능성이 있다. */
class SensorChangeDetector(initial: Map<SensorKind, SignalState> = emptyMap()) {
    private val states = initial.toMutableMap()

    fun onSignal(kind: SensorKind, state: SignalState, nowMillis: Long): Recovery? {
        if (state == SignalState.UNKNOWN) return null
        val previous = states.put(kind, state)
        return if (previous == SignalState.LOW && state == SignalState.NORMAL) Recovery(kind, kind.part(), nowMillis) else null
    }

    fun isLow(kind: SensorKind): Boolean = states[kind] == SignalState.LOW

    fun snapshot(): Map<SensorKind, SignalState> = states.toMap()
}

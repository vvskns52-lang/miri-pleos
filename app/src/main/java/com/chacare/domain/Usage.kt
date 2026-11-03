package com.chacare.domain

import kotlinx.serialization.Serializable

/** 마지막 교체 이후 누적된 운행 기록. */
@Serializable
data class UsageStats(
    val drivenKm: Double = 0.0,
    val drivingMinutes: Double = 0.0,
    val brakePresses: Int = 0,
    val shortTrips: Int = 0,
    val totalTrips: Int = 0,
    val wiperMinutes: Double = 0.0,
    val hvacMinutes: Double = 0.0,
) {
    operator fun plus(o: UsageStats) = UsageStats(
        drivenKm + o.drivenKm, drivingMinutes + o.drivingMinutes, brakePresses + o.brakePresses,
        shortTrips + o.shortTrips, totalTrips + o.totalTrips, wiperMinutes + o.wiperMinutes, hvacMinutes + o.hvacMinutes,
    )

    operator fun minus(o: UsageStats) = UsageStats(
        drivenKm - o.drivenKm, drivingMinutes - o.drivingMinutes, brakePresses - o.brakePresses,
        shortTrips - o.shortTrips, totalTrips - o.totalTrips, wiperMinutes - o.wiperMinutes, hvacMinutes - o.hvacMinutes,
    )
}

const val SHORT_TRIP_KM = 8.0
private const val MIN_SAMPLE_KM = 100.0
private const val BASELINE_BRAKES_PER_KM = 1.0
private const val WIPER_HEAVY_RATIO = 0.2

/**
 * 운행 습관에 따른 소모 속도 배수(설계서 5.1). 1.0보다 크면 빨리 닳는다.
 * 표본이 부족하거나 계산할 수 없으면 1.0.
 */
fun correctionFactor(part: Part, usage: UsageStats): Double {
    if (usage.drivenKm < MIN_SAMPLE_KM) return 1.0
    val raw = when (part) {
        Part.BRAKE_PAD -> usage.brakePresses / usage.drivenKm / BASELINE_BRAKES_PER_KM
        Part.ENGINE_OIL, Part.TRANSMISSION_OIL ->
            ratio(usage.shortTrips.toDouble(), usage.totalTrips.toDouble())?.let { 1.0 + 0.5 * it }
        Part.CABIN_FILTER -> ratio(usage.hvacMinutes, usage.drivingMinutes)?.let { 0.8 + 0.7 * it }
        Part.WIPER_BLADE -> ratio(usage.wiperMinutes, usage.drivingMinutes)?.let { 0.8 + 0.7 * it / WIPER_HEAVY_RATIO }
        else -> null
    } ?: return 1.0
    return raw.coerceIn(0.8, 1.5)
}

private fun ratio(a: Double, b: Double): Double? = if (b <= 0.0) null else a / b

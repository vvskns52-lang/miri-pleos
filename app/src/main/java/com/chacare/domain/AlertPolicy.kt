package com.chacare.domain

import kotlinx.serialization.Serializable

@Serializable
data class AlertRecord(val part: Part, val atMillis: Long, val odometerKm: Double)

private const val REPEAT_AFTER_MILLIS = 7 * 86_400_000L
private const val REPEAT_AFTER_KM = 500.0

/** "교체 필요"만 알리고, 같은 소모품은 7일 또는 500km가 지나야 다시 알린다(설계서 5.5). */
fun shouldAlert(status: LifeStatus, last: AlertRecord?, nowMillis: Long, odometerKm: Double): Boolean {
    if (status != LifeStatus.REPLACE) return false
    if (last == null) return true
    return nowMillis - last.atMillis >= REPEAT_AFTER_MILLIS || odometerKm - last.odometerKm >= REPEAT_AFTER_KM
}

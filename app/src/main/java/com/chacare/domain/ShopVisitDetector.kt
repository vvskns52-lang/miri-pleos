package com.chacare.domain

import kotlinx.serialization.Serializable
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class Shop(val id: String, val name: String, val lat: Double, val lon: Double)

/** 재시작(시동 off/on)을 견디도록 저장소에 그대로 저장한다. */
@Serializable
data class DwellState(val shopId: String, val enteredAtMillis: Long, val lastSeenMillis: Long, val emitted: Boolean)

@Serializable
data class ShopVisit(val shopId: String, val enteredAtMillis: Long)

private const val EARTH_RADIUS_M = 6_371_000.0

fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    return 2 * EARTH_RADIUS_M * asin(sqrt(a))
}

/**
 * 설계서 4.3 ②: 등록 정비소 반경 안에 일정 시간 머물면 방문으로 본다.
 * 위치가 끊겼다가(시동 off) 같은 정비소에서 다시 들어오면 체류가 이어진다.
 */
class ShopVisitDetector(
    private val shops: List<Shop>,
    initial: DwellState? = null,
    private val radiusM: Double = 100.0,
    private val dwellMillis: Long = 30 * 60_000L,
) {
    private var state: DwellState? = initial

    fun onLocation(lat: Double, lon: Double, nowMillis: Long): ShopVisit? {
        val shop = shops.firstOrNull { distanceMeters(lat, lon, it.lat, it.lon) <= radiusM }
        if (shop == null) {
            state = null
            return null
        }
        val current = state?.takeIf { it.shopId == shop.id } ?: DwellState(shop.id, nowMillis, nowMillis, false)
        val reached = !current.emitted && nowMillis - current.enteredAtMillis >= dwellMillis
        state = current.copy(lastSeenMillis = nowMillis, emitted = current.emitted || reached)
        return if (reached) ShopVisit(shop.id, current.enteredAtMillis) else null
    }

    fun state(): DwellState? = state
}

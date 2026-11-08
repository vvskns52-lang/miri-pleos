package com.chacare.domain

/**
 * 취급설명서 「가혹 조건」 중 차량 데이터로 알 수 있는 것.
 * 나머지(모래·먼지, 험로, 산길, 택시·상용, 한랭지, 견인, 비추천 오일)는 차가 알 수 없어 다루지 않는다.
 */
enum class SevereReason(val label: String) {
    SHORT_TRIPS("짧은 거리 반복 주행"),
    CONGESTION("교통 체증·잦은 공회전"),
    STOP_AND_GO("정지와 출발의 잦은 반복"),
}

/** 이 가혹 조건 중 하나라도 해당하면 [interval]을 쓴다(매뉴얼 표의 "해당 조건" 번호를 옮긴 것). */
data class SevereInterval(val interval: Interval, val reasons: Set<SevereReason>)

// 판정 기준은 매뉴얼에 수치가 없어 앱이 정한 값이다(설계서 5.14).
private const val MIN_KM = 200.0
private const val MIN_TRIPS = 10
private const val MIN_MINUTES = 60.0
private const val SHORT_TRIP_RATIO = 0.5
private const val CONGESTION_KMH = 20.0
private const val STOP_AND_GO_BRAKES_PER_KM = 2.0

/** 누적 운행 기록으로 가혹 조건을 판정한다. 기록이 적으면 판정하지 않는다. */
fun detectSevere(u: UsageStats): Set<SevereReason> {
    if (u.drivenKm < MIN_KM) return emptySet()
    return buildSet {
        if (u.totalTrips >= MIN_TRIPS && u.shortTrips.toDouble() / u.totalTrips >= SHORT_TRIP_RATIO) add(SevereReason.SHORT_TRIPS)
        if (u.drivingMinutes >= MIN_MINUTES && u.drivenKm / (u.drivingMinutes / 60.0) < CONGESTION_KMH) add(SevereReason.CONGESTION)
        if (u.brakePresses / u.drivenKm >= STOP_AND_GO_BRAKES_PER_KM) add(SevereReason.STOP_AND_GO)
    }
}

/** 화면에 보여줄 판정 근거 숫자. */
fun severeEvidence(r: SevereReason, u: UsageStats): String = when (r) {
    SevereReason.SHORT_TRIPS -> "8km 미만 주행 ${(100.0 * u.shortTrips / u.totalTrips.coerceAtLeast(1)).toInt()}%"
    SevereReason.CONGESTION -> "평균 시속 ${(u.drivenKm / (u.drivingMinutes / 60.0).coerceAtLeast(0.01)).toInt()}km"
    SevereReason.STOP_AND_GO -> "1km당 브레이크 %.1f회".format(u.brakePresses / u.drivenKm.coerceAtLeast(0.01))
}

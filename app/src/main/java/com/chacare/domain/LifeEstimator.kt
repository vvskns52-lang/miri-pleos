package com.chacare.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.roundToLong

enum class LifeStatus { GOOD, CHECK, REPLACE }

data class LastService(val date: LocalDate, val odometerKm: Double)

data class LifeEstimate(
    val part: Part,
    val status: LifeStatus,
    val remainingRatio: Double?,
    val remainingKm: Double?,
    val remainingMonths: Double?,
    val kind: IntervalKind = IntervalKind.REPLACE,
    val note: String? = null,
    val severeBy: Set<SevereReason> = emptySet(),
)

private const val DAYS_PER_MONTH = 30.4375
private const val EPS = 1e-9

/**
 * 남은 수명 비율 = 거리·기간 중 더 많이 쓴 쪽 기준. [factor]는 운행 보정 배수.
 * 교체 이력을 모르면 "점검 권장"으로 시작(설계서 5.3).
 */
fun estimate(
    spec: PartSpec,
    last: LastService?,
    nowOdometerKm: Double,
    today: LocalDate,
    factor: Double,
    sensorWarning: Boolean = false,
): LifeEstimate {
    val life = lifeOf(spec.intervalKm, spec.intervalMonths, last, nowOdometerKm, today, factor)
        ?: return LifeEstimate(spec.part, if (sensorWarning) LifeStatus.REPLACE else LifeStatus.GOOD, null, null, null, spec.kind, spec.note, spec.severeBy)
    return LifeEstimate(spec.part, life.status, life.remainingRatio, life.remainingKm, life.remainingMonths, spec.kind, spec.note, spec.severeBy)
}

data class Life(val status: LifeStatus, val remainingRatio: Double?, val remainingKm: Double?, val remainingMonths: Double?)

/** 주기가 하나도 없으면 null(기록만 하는 항목). 교체 이력을 모르면 "점검 권장". */
fun lifeOf(
    intervalKm: Int?,
    intervalMonths: Int?,
    last: LastService?,
    nowOdometerKm: Double,
    today: LocalDate,
    factor: Double,
): Life? {
    if (intervalKm == null && intervalMonths == null) return null
    if (last == null) return Life(LifeStatus.CHECK, null, null, null)

    val usedKm = max(0.0, nowOdometerKm - last.odometerKm) * factor
    val usedMonths = max(0L, ChronoUnit.DAYS.between(last.date, today)) / DAYS_PER_MONTH * factor

    val kmRatio = intervalKm?.let { (it - usedKm) / it }
    val monthRatio = intervalMonths?.let { (it - usedMonths) / it }
    val ratio = listOfNotNull(kmRatio, monthRatio).min()

    val status = when {
        ratio > 0.30 + EPS -> LifeStatus.GOOD
        ratio > 0.10 + EPS -> LifeStatus.CHECK
        else -> LifeStatus.REPLACE
    }
    val remainingKm = intervalKm?.let { max(0.0, it - usedKm) / factor }
    val remainingMonths = intervalMonths?.let { max(0.0, it - usedMonths) / factor }
    return Life(status, ratio, remainingKm, remainingMonths)
}

/** 권장 주기를 "15,000km 또는 12개월"처럼 읽는다. */
fun intervalText(km: Int?, months: Int?): String = listOfNotNull(
    km?.let { "%,dkm".format(it) },
    months?.let { "${it}개월" },
).joinToString(" 또는 ").ifEmpty { "주기 없음" }

/** 가짜 정밀함을 피하려고 천 km 단위로만 보여준다(설계서 5.2). */
fun displayKm(remainingKm: Double?): String = when {
    remainingKm == null -> "정보 없음"
    remainingKm <= 0.0 -> "교체 시기 지남"
    remainingKm < 1000.0 -> "1,000km 미만"
    else -> "약 %,dkm".format((remainingKm / 1000.0).roundToLong() * 1000)
}

fun displayMonths(remainingMonths: Double?): String = when {
    remainingMonths == null -> "정보 없음"
    remainingMonths <= 0.0 -> "교체 시기 지남"
    remainingMonths < 0.5 -> "1개월 미만"
    else -> "약 ${remainingMonths.roundToInt()}개월"
}

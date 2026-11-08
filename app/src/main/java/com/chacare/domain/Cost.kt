package com.chacare.domain

import java.time.LocalDate
import java.time.YearMonth

/** 비용을 적은 교체·보충 기록 하나. [name]은 부품 이름(직접 추가한 부품 포함). */
data class CostEntry(val date: LocalDate, val name: String, val won: Long)

data class CostSummary(
    val thisMonth: Long,
    val thisYear: Long,
    val last12Months: Long,
    /** 이번 달을 포함한 최근 12개월, 오래된 달부터. 쓴 돈이 없는 달은 0. */
    val monthly: List<Pair<YearMonth, Long>>,
    /** 올해 부품별 합계, 큰 순. */
    val byItemThisYear: List<Pair<String, Long>>,
    /** 최근 기록부터. */
    val recent: List<CostEntry>,
)

fun summarizeCosts(entries: List<CostEntry>, today: LocalDate): CostSummary {
    val now = YearMonth.from(today)
    val months = (11 downTo 0).map { now.minusMonths(it.toLong()) }
    val byMonth = entries.groupBy { YearMonth.from(it.date) }.mapValues { (_, v) -> v.sumOf { it.won } }
    val thisYear = entries.filter { it.date.year == today.year }
    return CostSummary(
        thisMonth = byMonth[now] ?: 0,
        thisYear = thisYear.sumOf { it.won },
        last12Months = months.sumOf { byMonth[it] ?: 0 },
        monthly = months.map { it to (byMonth[it] ?: 0) },
        byItemThisYear = thisYear.groupBy { it.name }.map { (k, v) -> k to v.sumOf { it.won } }.sortedByDescending { it.second },
        recent = entries.sortedByDescending { it.date },
    )
}

fun wonText(won: Long): String = "%,d원".format(won)

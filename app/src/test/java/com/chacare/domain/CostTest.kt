package com.chacare.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CostTest {
    private val today = LocalDate.of(2026, 11, 8)
    private val entries = listOf(
        CostEntry(LocalDate.of(2026, 11, 2), "엔진오일", 80_000),
        CostEntry(LocalDate.of(2026, 11, 2), "에어 필터", 20_000),
        CostEntry(LocalDate.of(2026, 3, 15), "엔진오일", 75_000),
        CostEntry(LocalDate.of(2026, 1, 10), "타이어", 600_000),
        CostEntry(LocalDate.of(2025, 12, 1), "와이퍼", 30_000),
        CostEntry(LocalDate.of(2025, 10, 1), "배터리", 150_000), // 12개월 밖
    )
    private val s = summarizeCosts(entries, today)

    @Test fun monthAndYearTotals() {
        assertEquals(100_000, s.thisMonth)
        assertEquals(775_000, s.thisYear)
        assertEquals(805_000, s.last12Months)
    }

    @Test fun monthlyBarsCoverLast12MonthsOldestFirst() {
        assertEquals(12, s.monthly.size)
        assertEquals(YearMonth.of(2025, 12) to 30_000L, s.monthly.first())
        assertEquals(YearMonth.of(2026, 11) to 100_000L, s.monthly.last())
        assertEquals(0L, s.monthly.first { it.first == YearMonth.of(2026, 2) }.second)
    }

    @Test fun byItemThisYearLargestFirst() =
        assertEquals(listOf("타이어" to 600_000L, "엔진오일" to 155_000L, "에어 필터" to 20_000L), s.byItemThisYear)

    @Test fun recentNewestFirst() = assertEquals("와이퍼", s.recent[4].name)

    @Test fun empty() {
        val e = summarizeCosts(emptyList(), today)
        assertEquals(0, e.thisYear)
        assertEquals(12, e.monthly.size)
    }

    @Test fun wonFormat() = assertEquals("1,234,000원", wonText(1_234_000))
}

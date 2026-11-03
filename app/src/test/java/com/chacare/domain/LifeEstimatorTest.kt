package com.chacare.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class LifeEstimatorTest {
    private val oil = DEFAULT_SPECS.single { it.part == Part.ENGINE_OIL }
    private val d0 = LocalDate.of(2026, 1, 1)
    private fun at(km: Double) = LastService(d0, km)

    @Test fun fresh() = assertEquals(LifeStatus.GOOD, estimate(oil, at(0.0), 1000.0, d0.plusMonths(1), 1.0).status)

    @Test fun checkBand() {
        val e = estimate(oil, at(0.0), 8000.0, d0.plusMonths(1), 1.0)
        assertEquals(LifeStatus.CHECK, e.status)
        assertEquals(0.2, e.remainingRatio!!, 1e-9)
    }

    @Test fun boundary10IsReplace() = assertEquals(LifeStatus.REPLACE, estimate(oil, at(0.0), 9000.0, d0.plusMonths(1), 1.0).status)
    @Test fun boundary30IsCheck() = assertEquals(LifeStatus.CHECK, estimate(oil, at(0.0), 7000.0, d0.plusMonths(1), 1.0).status)
    @Test fun timeWinsOverKm() = assertEquals(LifeStatus.REPLACE, estimate(oil, at(0.0), 100.0, d0.plusMonths(12), 1.0).status)
    @Test fun factorAccelerates() = assertEquals(LifeStatus.CHECK, estimate(oil, at(0.0), 5000.0, d0.plusMonths(1), 1.5).status)

    @Test fun unknownHistory() {
        val e = estimate(oil, null, 5000.0, d0, 1.0)
        assertEquals(LifeStatus.CHECK, e.status)
        assertNull(e.remainingKm)
        assertEquals("정보 없음", displayKm(e.remainingKm))
    }

    @Test fun overdueNotNegative() = assertEquals(0.0, estimate(oil, at(0.0), 15000.0, d0.plusMonths(1), 1.0).remainingKm!!, 1e-9)
    @Test fun odometerBelowLastServiceNoCrash() = assertEquals(LifeStatus.GOOD, estimate(oil, at(5000.0), 4000.0, d0, 1.0).status)

    @Test fun washer() {
        val w = DEFAULT_SPECS.single { it.part == Part.WASHER_FLUID }
        assertEquals(LifeStatus.REPLACE, estimate(w, null, 0.0, d0, 1.0, sensorWarning = true).status)
        assertEquals(LifeStatus.GOOD, estimate(w, null, 0.0, d0, 1.0).status)
    }

    @Test fun rounding() {
        assertEquals("약 2,000km", displayKm(2400.0))
        assertEquals("약 3,000km", displayKm(2600.0))
        assertEquals("1,000km 미만", displayKm(400.0))
        assertEquals("교체 시기 지남", displayKm(0.0))
        assertEquals("약 5개월", displayMonths(4.6))
        assertEquals("정보 없음", displayMonths(null))
    }
}

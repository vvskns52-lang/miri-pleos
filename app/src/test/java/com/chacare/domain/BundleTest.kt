package com.chacare.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BundleTest {
    private fun e(
        part: Part, status: LifeStatus, ratio: Double? = 0.5, km: Double? = 10_000.0, months: Double? = null,
        kind: IntervalKind = IntervalKind.REPLACE,
    ) = LifeEstimate(part, status, ratio, km, months, kind)

    @Test fun overdueAndSoonGoTogether() {
        val list = listOf(
            e(Part.ENGINE_OIL, LifeStatus.REPLACE, 0.0, 0.0),
            e(Part.ENGINE_AIR_FILTER, LifeStatus.CHECK, 0.2, 2_500.0),
            e(Part.BRAKE_PAD, LifeStatus.GOOD, 0.4, 4_000.0, kind = IntervalKind.INSPECT),
            e(Part.WIPER_BLADE, LifeStatus.GOOD, 0.9, 18_000.0, 1.5),
        )
        assertEquals(listOf(Part.ENGINE_OIL, Part.ENGINE_AIR_FILTER, Part.WIPER_BLADE), bundleOf(list).map { it.part })
    }

    @Test fun singleItemIsNotABundle() =
        assertTrue(bundleOf(listOf(e(Part.ENGINE_OIL, LifeStatus.REPLACE, 0.0, 0.0), e(Part.TIRE, LifeStatus.GOOD))).isEmpty())

    @Test fun unknownHistoryAndNoIntervalAreSkipped() {
        val list = listOf(
            e(Part.ENGINE_OIL, LifeStatus.REPLACE, 0.0, 0.0),
            e(Part.SPARK_PLUG, LifeStatus.CHECK, null, null),
            e(Part.TRANSMISSION_OIL, LifeStatus.GOOD, null, null, kind = IntervalKind.NONE),
            e(Part.WASHER_FLUID, LifeStatus.REPLACE, null, null, kind = IntervalKind.NONE),
        )
        assertTrue(bundleOf(list).isEmpty())
    }

    @Test fun tipNamesOnlyTheExtras() {
        val tip = Prompt.BundleTip(listOf(Part.ENGINE_AIR_FILTER, Part.WIPER_BLADE))
        assertEquals("정비소에 가실 때 에어 필터, 와이퍼도 함께 하시면 한 번에 끝낼 수 있어요.", tip.text)
    }

    @Test fun plannerAddsTipAfterAlertsOnly() {
        val withAlert = planPrompts(null, emptyList(), listOf(Part.ENGINE_OIL), false, false, bundleExtras = listOf(Part.ENGINE_AIR_FILTER))
        assertEquals(listOf(Prompt.Alert::class, Prompt.BundleTip::class), withAlert.map { it::class })
        val noAlert = planPrompts(null, emptyList(), emptyList(), false, false, bundleExtras = listOf(Part.ENGINE_AIR_FILTER))
        assertTrue(noAlert.isEmpty())
    }
}

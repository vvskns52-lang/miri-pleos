package com.chacare.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptPlannerTest {
    private val day = 86_400_000L
    private val last = AlertRecord(Part.BRAKE_PAD, 0, 0.0)

    @Test fun firstAlert() = assertTrue(shouldAlert(LifeStatus.REPLACE, null, 0, 0.0))
    @Test fun checkNeverAlerts() = assertFalse(shouldAlert(LifeStatus.CHECK, null, 0, 0.0))
    @Test fun suppressedWithin() = assertFalse(shouldAlert(LifeStatus.REPLACE, last, 6 * day, 499.0))
    @Test fun after7days() = assertTrue(shouldAlert(LifeStatus.REPLACE, last, 7 * day, 10.0))
    @Test fun after500km() = assertTrue(shouldAlert(LifeStatus.REPLACE, last, day, 500.0))

    @Test fun order() {
        val p = planPrompts(
            ShopVisit("s1", 0), listOf(PendingConfirmation(Part.ENGINE_OIL, 0)), listOf(Part.BRAKE_PAD),
            tireLow = true, washerLow = false,
        )
        assertEquals(
            listOf(Prompt.TireLow::class, Prompt.AskShop::class, Prompt.AskSensor::class, Prompt.Alert::class),
            p.map { it::class },
        )
        assertEquals("오늘 정비소에서 뭘 교체하셨어요?", p[1].text)
        assertEquals("엔진오일 교체하셨나요? 교체, 보충, 아니요로 답해 주세요.", p[2].text)
        assertEquals("브레이크 패드 교체 시기가 됐어요.", p[3].text)
    }

    @Test fun droppedAfterTwoAsks() = assertTrue(
        planPrompts(null, listOf(PendingConfirmation(Part.ENGINE_OIL, 0, askCount = 2)), emptyList(), false, false).isEmpty(),
    )
}

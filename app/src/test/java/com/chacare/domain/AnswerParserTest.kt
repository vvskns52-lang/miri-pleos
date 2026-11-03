package com.chacare.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AnswerParserTest {
    @Test fun answers() {
        assertEquals(Answer.REPLACED, parseAnswer("응 교체했어"))
        assertEquals(Answer.REFILLED, parseAnswer("아니 보충만 했어"))
        assertEquals(Answer.NO, parseAnswer("교체 안 했어"))
        assertEquals(Answer.UNKNOWN, parseAnswer("오늘 날씨 좋다"))
        assertEquals(Answer.UNKNOWN, parseAnswer(""))
        assertEquals(Answer.UNKNOWN, parseAnswer(null))
    }

    @Test fun parts() {
        assertEquals(setOf(Part.ENGINE_OIL, Part.WIPER_BLADE), parseParts("엔진오일이랑 와이퍼"))
        assertEquals(setOf(Part.TRANSMISSION_OIL), parseParts("미션 오일 갈았어"))
        assertEquals(setOf(Part.BRAKE_FLUID, Part.BRAKE_PAD), parseParts("브레이크 오일이랑 패드"))
        assertEquals(setOf(Part.ENGINE_OIL), parseParts("오일 교환했어"))
        assertEquals(emptySet<Part>(), parseParts("아무것도 안 했어"))
        assertEquals(emptySet<Part>(), parseParts(null))
    }

    @Test fun addedParts() {
        assertEquals(setOf(Part.CABIN_FILTER, Part.ENGINE_AIR_FILTER), parseParts("에어컨 필터랑 에어필터"))
        assertEquals(setOf(Part.CABIN_FILTER), parseParts("필터 갈았어"))
        assertEquals(setOf(Part.ENGINE_AIR_FILTER), parseParts("에어클리너 필터 교체"))
        assertEquals(setOf(Part.SPARK_PLUG, Part.COOLANT), parseParts("점화플러그랑 부동액"))
        assertEquals(setOf(Part.BATTERY, Part.DRIVE_BELT), parseParts("배터리하고 벨트"))
    }
}

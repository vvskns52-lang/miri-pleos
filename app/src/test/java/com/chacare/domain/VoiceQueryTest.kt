package com.chacare.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceQueryTest {
    private val oil = LifeEstimate(Part.ENGINE_OIL, LifeStatus.CHECK, 0.25, 3_600.0, 4.4)
    private val pad = LifeEstimate(Part.BRAKE_PAD, LifeStatus.REPLACE, 0.0, 0.0, null, IntervalKind.INSPECT)
    private val plug = LifeEstimate(Part.SPARK_PLUG, LifeStatus.CHECK, null, null, null)
    private val atf = LifeEstimate(Part.TRANSMISSION_OIL, LifeStatus.GOOD, null, null, null, IntervalKind.NONE, "무점검·무교체")
    private val tire = LifeEstimate(Part.TIRE, LifeStatus.GOOD, 0.8, 8_000.0, null, IntervalKind.INSPECT)
    private val all = listOf(oil, pad, plug, atf, tire)
    private val specs = specsFor(Powertrain.ICE, findModel("avante"))
    private fun ask(q: String?) = answerQuery(q, all, specs, emptyList())

    @Test fun remainingForOnePart() =
        assertEquals("엔진오일은 교체까지 약 4,000km, 약 4개월 남았어요.", ask("엔진오일 언제 갈아야 돼?"))

    @Test fun inspectOverdue() =
        assertEquals("브레이크 패드는 점검 시기가 지났어요. 가까운 정비소에서 점검받으세요.", ask("브레이크 패드 어때"))

    @Test fun unknownHistory() =
        assertEquals("점화플러그는 교체 기록이 없어서 남은 수명을 알 수 없어요. 마지막 교체 기록을 남겨 주세요.", ask("점화플러그는?"))

    @Test fun noInterval() = assertEquals("변속기 오일은 무점검·무교체예요.", ask("미션오일 갈아야 해?"))

    @Test fun intervalQuestion() =
        assertEquals("엔진오일 권장 교체 주기는 15,000km 또는 12개월이에요.", ask("엔진오일 교체 주기가 뭐야"))

    @Test fun twoPartsAnswered() = assertEquals(
        "엔진오일은 교체까지 약 4,000km, 약 4개월 남았어요. 타이어는 점검까지 약 8,000km 남았어요.",
        ask("엔진오일이랑 타이어"),
    )

    @Test fun overview() =
        assertEquals("지금 챙길 소모품은 브레이크 패드 점검이에요. 곧 시기가 되는 건 엔진오일이에요.", ask("뭐 갈아야 돼?"))

    @Test fun overviewAllGood() = assertEquals(
        "지금은 모든 소모품이 괜찮아요.",
        answerQuery("상태 어때", listOf(tire), specs, emptyList()),
    )

    @Test fun customPartByName() = assertEquals(
        "블랙박스 메모리는 마지막으로 2026-05-01에 교체했어요.",
        answerQuery("블랙박스 메모리 언제 바꿨지", all, specs, listOf(CustomAnswer("블랙박스 메모리", "마지막으로 2026-05-01에 교체했어요"))),
    )

    private val costs = summarizeCosts(
        listOf(
            CostEntry(java.time.LocalDate.of(2026, 11, 2), "엔진오일", 80_000),
            CostEntry(java.time.LocalDate.of(2026, 3, 15), "엔진오일", 75_000),
            CostEntry(java.time.LocalDate.of(2026, 1, 10), "타이어", 600_000),
        ),
        java.time.LocalDate.of(2026, 11, 8),
    )
    private fun askCost(q: String) = answerQuery(q, all, specs, emptyList(), costs)

    @Test fun costOverview() =
        assertEquals("이번 달 정비 비용은 80,000원, 올해는 755,000원이에요.", askCost("정비비 얼마나 들었어?"))

    @Test fun costThisYear() = assertEquals("올해 정비 비용은 755,000원이에요.", askCost("올해 정비 비용"))

    @Test fun costForPart() = assertEquals("엔진오일에 지금까지 쓴 비용은 155,000원이에요.", askCost("엔진오일 비용 얼마 썼어"))

    @Test fun costForPartWithoutRecords() = assertEquals("브레이크 패드는 적어 둔 비용이 없어요.", askCost("브레이크 패드 비용"))

    @Test fun noCostsYet() = assertEquals(
        "아직 적어 둔 정비 비용이 없어요. 교체 기록에 비용을 적어 주세요.",
        answerQuery("정비비 얼마야", all, specs, emptyList(), summarizeCosts(emptyList(), java.time.LocalDate.of(2026, 11, 8))),
    )

    @Test fun howMuchLeftIsNotCost() = assertEquals("엔진오일은 교체까지 약 4,000km, 약 4개월 남았어요.", askCost("엔진오일 얼마 남았어"))

    @Test fun didNotUnderstand() {
        val expected = "잘 못 알아들었어요. '엔진오일 언제 갈아?'처럼 물어봐 주세요."
        assertEquals(expected, ask("오늘 날씨 좋다"))
        assertEquals(expected, ask(null))
    }
}

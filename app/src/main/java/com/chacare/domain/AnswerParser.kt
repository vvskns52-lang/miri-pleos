package com.chacare.domain

enum class Answer { REPLACED, REFILLED, NO, UNKNOWN }

private val REFILL_WORDS = listOf("보충", "채웠", "넣었")
private val NO_WORDS = listOf("아니", "안했", "없어")
private val REPLACED_WORDS = listOf("교체", "갈았", "바꿨", "응", "네", "예", "했어")

private fun normalize(text: String?): String = text.orEmpty().filterNot { it.isWhitespace() }

/** "교체 안 했어"가 교체로 읽히지 않도록 보충 → 부정 → 긍정 순서로 판정한다. */
fun parseAnswer(text: String?): Answer {
    val t = normalize(text)
    if (t.isEmpty()) return Answer.UNKNOWN
    return when {
        REFILL_WORDS.any { it in t } -> Answer.REFILLED
        NO_WORDS.any { it in t } -> Answer.NO
        REPLACED_WORDS.any { it in t } -> Answer.REPLACED
        else -> Answer.UNKNOWN
    }
}

private val PART_WORDS: List<Pair<Part, List<String>>> = listOf(
    Part.TRANSMISSION_OIL to listOf("미션오일", "변속기"),
    Part.BRAKE_FLUID to listOf("브레이크액", "브레이크오일"),
    Part.BRAKE_PAD to listOf("패드"),
    Part.CABIN_FILTER to listOf("필터"),
    Part.WIPER_BLADE to listOf("와이퍼"),
    Part.WASHER_FLUID to listOf("워셔"),
    Part.TIRE to listOf("타이어"),
    Part.ENGINE_OIL to listOf("엔진오일", "엔진"),
)

/** 그냥 "오일"은 다른 오일 부품이 언급되지 않았을 때만 엔진오일로 본다. */
fun parseParts(text: String?): Set<Part> {
    val t = normalize(text)
    val found = PART_WORDS.filter { (_, words) -> words.any { it in t } }.map { it.first }.toMutableSet()
    val otherOil = Part.TRANSMISSION_OIL in found || Part.BRAKE_FLUID in found
    if ("오일" in t && !otherOil) found += Part.ENGINE_OIL
    return found
}

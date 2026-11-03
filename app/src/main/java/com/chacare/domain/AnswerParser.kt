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
    Part.CABIN_FILTER to listOf("에어컨필터", "캐빈필터", "실내필터"),
    Part.ENGINE_AIR_FILTER to listOf("에어필터", "에어클리너", "엔진필터", "공기필터"),
    Part.WIPER_BLADE to listOf("와이퍼"),
    Part.WASHER_FLUID to listOf("워셔"),
    Part.TIRE to listOf("타이어"),
    Part.SPARK_PLUG to listOf("플러그"),
    Part.COOLANT to listOf("냉각수", "부동액"),
    Part.BATTERY to listOf("배터리", "밧데리"),
    Part.DRIVE_BELT to listOf("벨트"),
    Part.ENGINE_OIL to listOf("엔진오일", "엔진"),
)

/** 사용자가 직접 추가한 부품 이름이 답변에 나왔는지(띄어쓰기 무시). */
fun mentions(name: String, text: String?): Boolean {
    val n = normalize(name)
    return n.isNotEmpty() && n in normalize(text)
}

/**
 * 그냥 "오일"은 다른 오일 부품이 없을 때만 엔진오일로, 그냥 "필터"는 다른 필터가 없을 때만 에어컨 필터로 본다.
 * "엔진필터"가 엔진오일로 잡히지 않도록 엔진 필터가 있으면 "엔진"만으로는 엔진오일로 보지 않는다.
 */
fun parseParts(text: String?): Set<Part> {
    val t = normalize(text)
    val found = PART_WORDS.filter { (_, words) -> words.any { it in t } }.map { it.first }.toMutableSet()
    if (Part.ENGINE_AIR_FILTER in found && "엔진오일" !in t) found -= Part.ENGINE_OIL
    val otherOil = Part.TRANSMISSION_OIL in found || Part.BRAKE_FLUID in found
    if ("오일" in t && !otherOil) found += Part.ENGINE_OIL
    val anyFilter = Part.CABIN_FILTER in found || Part.ENGINE_AIR_FILTER in found
    if ("필터" in t && !anyFilter) found += Part.CABIN_FILTER
    return found
}

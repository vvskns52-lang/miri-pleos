package com.chacare.domain

/** 직접 추가한 부품: 이름과, "~은 ○○." 뒤에 붙일 상태 문장. */
data class CustomAnswer(val name: String, val phrase: String)

private const val NOT_UNDERSTOOD = "잘 못 알아들었어요. '엔진오일 언제 갈아?'처럼 물어봐 주세요."
private val OVERVIEW_WORDS = listOf("뭐", "무엇", "갈아야", "교체할", "점검할", "상태", "괜찮", "전체", "다 알려")

/** 마지막 글자에 받침이 있는지(한글이 아니면 받침 없음으로 본다). */
private fun hasBatchim(word: String): Boolean {
    val c = word.trimEnd().lastOrNull() ?: return false
    if (c !in '가'..'힣') return c.isDigit() && c in "0136789" // 영·일·삼·육·칠·팔·구
    return (c - '가') % 28 != 0
}

private fun topic(name: String) = name + if (hasBatchim(name)) "은" else "는"
private fun copula(word: String) = word + if (hasBatchim(word)) "이에요" else "예요"

private fun partAnswer(e: LifeEstimate?, part: Part, spec: PartSpec?, askingInterval: Boolean): String {
    val name = part.koreanName
    if (e == null) return "${topic(name)} 이 차에 해당하지 않아요."
    val verb = if (e.kind == IntervalKind.INSPECT) "점검" else "교체"
    if (askingInterval && spec != null) {
        return if (spec.kind == IntervalKind.NONE) "${topic(name)} ${copula(spokenNote(spec.note))}."
        else "$name 권장 $verb 주기는 ${copula(intervalText(spec.intervalKm, spec.intervalMonths))}."
    }
    return when {
        e.kind == IntervalKind.NONE -> "${topic(name)} ${copula(spokenNote(e.note))}."
        e.remainingRatio == null -> "${topic(name)} $verb 기록이 없어서 남은 수명을 알 수 없어요. 마지막 $verb 기록을 남겨 주세요."
        (e.remainingKm ?: 1.0) <= 0.0 || (e.remainingMonths ?: 1.0) <= 0.0 ->
            "${topic(name)} $verb 시기가 지났어요. 가까운 정비소에서 ${if (verb == "점검") "점검받으세요" else "교체하세요"}."
        else -> "${topic(name)} ${verb}까지 " +
            listOfNotNull(e.remainingKm?.let(::displayKm), e.remainingMonths?.let(::displayMonths)).joinToString(", ") + " 남았어요."
    }
}

/** 음성으로 읽기 좋게 괄호 설명은 뺀다. */
private fun spokenNote(note: String?) = (note ?: "정해진 교체 주기가 없어요").substringBefore(" (").trim()

private fun overview(estimates: List<LifeEstimate>): String {
    val tracked = estimates.filter { it.kind != IntervalKind.NONE && it.remainingRatio != null }
    val now = tracked.filter { it.status == LifeStatus.REPLACE }
        .map { "${it.part.koreanName} ${if (it.kind == IntervalKind.INSPECT) "점검" else "교체"}" }
    val soon = tracked.filter { it.status == LifeStatus.CHECK }.map { it.part.koreanName }
    if (now.isEmpty() && soon.isEmpty()) return "지금은 모든 소모품이 괜찮아요."
    return listOfNotNull(
        now.takeIf { it.isNotEmpty() }?.let { "지금 챙길 소모품은 ${copula(it.joinToString(", "))}." },
        soon.takeIf { it.isNotEmpty() }?.let { "곧 시기가 되는 건 ${copula(it.joinToString(", "))}." },
    ).joinToString(" ")
}

/**
 * 운전자가 말로 물은 것에 답한다.
 * 부품 이름이 있으면 그 부품의 남은 수명(또는 "주기"를 물으면 권장 주기), 없으면 전체 요약.
 */
fun answerQuery(text: String?, estimates: List<LifeEstimate>, specs: List<PartSpec>, customs: List<CustomAnswer>): String {
    if (text.isNullOrBlank()) return NOT_UNDERSTOOD
    val parts = parseParts(text).sortedBy { it.ordinal }
    val customHits = customs.filter { mentions(it.name, text) }
    // 직접 추가한 부품 이름이 기본 부품 단어("필터" 등)를 포함하면 기본 부품으로 잘못 잡히지 않게 한다.
    val partsToAnswer = if (customHits.isNotEmpty()) parts.filter { p -> customHits.none { p.koreanName in it.name } } else parts
    val askingInterval = "주기" in text
    val answers = partsToAnswer.map { p ->
        partAnswer(estimates.firstOrNull { it.part == p }, p, specs.firstOrNull { it.part == p }, askingInterval)
    } + customHits.map { "${topic(it.name)} ${it.phrase}." }
    if (answers.isNotEmpty()) return answers.joinToString(" ")
    if (OVERVIEW_WORDS.any { it in text }) return overview(estimates)
    return NOT_UNDERSTOOD
}

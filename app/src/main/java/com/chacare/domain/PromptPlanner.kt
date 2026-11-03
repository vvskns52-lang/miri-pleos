package com.chacare.domain

/** 같은 질문에 이만큼 답이 없으면 더 묻지 않는다. */
const val MAX_ASKS = 2

/** 정차 중에 운전자에게 말하거나 묻는 한 마디. */
sealed class Prompt {
    abstract val text: String

    data class AskShop(val visit: ShopVisit) : Prompt() {
        override val text = "오늘 정비소에서 뭘 교체하셨어요?"
    }

    data class AskSensor(val pending: PendingConfirmation) : Prompt() {
        override val text = "${pending.part.koreanName} 교체하셨나요? 교체, 보충, 아니요로 답해 주세요."
    }

    /** 취급설명서가 "점검"만 정한 항목(패드·12V 배터리 등)은 점검 시기로 알린다. */
    data class Alert(val part: Part, val inspect: Boolean = false) : Prompt() {
        override val text = "${part.koreanName} ${if (inspect) "점검" else "교체"} 시기가 됐어요."
    }

    data object TireLow : Prompt() {
        override val text = "타이어 공기압이 낮아요. 확인해 주세요."
    }

    data object WasherLow : Prompt() {
        override val text = "워셔액이 부족해요."
    }
}

/** 안전 경고 → 정비소 질문(여러 부품을 한 번에) → 센서 확인 → 교체 시기 알림 순. */
fun planPrompts(
    shop: ShopVisit?,
    pendings: List<PendingConfirmation>,
    alerts: List<Part>,
    tireLow: Boolean,
    washerLow: Boolean,
    inspectOnly: Set<Part> = emptySet(),
): List<Prompt> = buildList {
    if (tireLow) add(Prompt.TireLow)
    if (washerLow) add(Prompt.WasherLow)
    if (shop != null) add(Prompt.AskShop(shop))
    pendings.filter { it.askCount < MAX_ASKS }.forEach { add(Prompt.AskSensor(it)) }
    alerts.forEach { add(Prompt.Alert(it, inspect = it in inspectOnly)) }
}

package com.chacare.domain

enum class Urgency(val label: String) {
    STOP_NOW("즉시 정차"),
    SHOP_SOON("빨리 점검"),
    CHECK("확인 필요"),
}

/**
 * 계기판 경고등 중 정비와 관련된 것(설계서 5.18). 순서는 급한 순.
 * [cause]는 "~은 [cause] 켜져요."로 읽히게 쓰고, [action]은 켜졌을 때 음성으로 그대로 읽는다.
 * [aliases]는 띄어쓰기 없이 소문자로 적는다.
 */
enum class WarningLight(
    val koreanName: String,
    val urgency: Urgency,
    val cause: String,
    val detail: String,
    val action: String,
    val part: Part? = null,
    val aliases: List<String>,
) {
    OIL_PRESSURE(
        "엔진오일 압력 경고등", Urgency.STOP_NOW, "엔진오일 압력이 낮을 때",
        "엔진오일이 부족하거나 오일 펌프·오일 계통에 이상이 있으면 켜져요. 이대로 달리면 엔진이 크게 손상될 수 있어요.",
        "안전한 곳에 차를 세우고 시동을 끄세요. 엔진오일 양을 확인하고, 양이 정상인데도 켜지면 운행하지 말고 정비소에 연락하세요.",
        Part.ENGINE_OIL, listOf("엔진오일압력", "오일압력", "엔진오일경고등", "오일경고등"),
    ),
    ENGINE_TEMP(
        "엔진 과열 경고등", Urgency.STOP_NOW, "엔진 온도가 너무 높을 때",
        "냉각수가 부족하거나 냉각팬·라디에이터 같은 냉각 장치에 이상이 있으면 켜져요.",
        "안전한 곳에 차를 세우고 시동을 끈 뒤 엔진이 식을 때까지 기다리세요. 뜨거울 때 냉각수 캡을 열면 위험해요.",
        Part.COOLANT, listOf("과열", "냉각수경고등", "온도경고등"),
    ),
    BRAKE(
        "브레이크 경고등", Urgency.STOP_NOW, "주차 브레이크가 채워져 있거나 브레이크액이 부족할 때",
        "주차 브레이크를 풀었는데도 켜져 있으면 브레이크액이 부족하거나 브레이크 장치에 이상이 있는 거예요. P단에서는 주차 브레이크 때문에 켜질 수 있어 안내하지 않아요.",
        "주차 브레이크가 풀렸는지 확인하세요. 풀었는데도 켜져 있으면 안전한 곳에 세우고 정비소에 연락하세요.",
        Part.BRAKE_FLUID, listOf("브레이크경고등"),
    ),
    CHARGING(
        "충전 경고등", Urgency.SHOP_SOON, "발전기나 충전 장치에 이상이 있을 때",
        "배터리가 충전되지 않고 있어요. 배터리에 남은 전기만으로 달리는 상태라 오래 가지 못해요.",
        "에어컨·열선 같은 전기 장치를 끄고 가까운 정비소로 바로 가세요. 시동을 끄면 다시 걸리지 않을 수 있어요.",
        Part.BATTERY, listOf("충전경고등", "배터리경고등"),
    ),
    CHECK_ENGINE(
        "엔진 점검 경고등", Urgency.SHOP_SOON, "엔진 제어나 배기가스 장치에 이상이 감지됐을 때",
        "엔진 전자 제어나 배출가스 관련 장치에서 이상이 감지됐어요. 깜빡이면 엔진 실화처럼 급한 이상일 수 있어요.",
        "운행은 할 수 있지만 빨리 정비소에서 점검받으세요. 깜빡이면 속도를 줄이고 바로 점검받으세요.",
        aliases = listOf("엔진경고등", "엔진점검", "엔진체크"),
    ),
    AIRBAG(
        "에어백 경고등", Urgency.SHOP_SOON, "에어백 장치에 이상이 있을 때",
        "사고가 나도 에어백이 펴지지 않을 수 있어요.",
        "빨리 정비소에서 점검받으세요.",
        aliases = listOf("에어백"),
    ),
    STEERING(
        "조향 장치 경고등", Urgency.SHOP_SOON, "전동식 파워 스티어링에 이상이 있을 때",
        "핸들이 평소보다 무거워질 수 있어요.",
        "핸들을 조심해서 돌리고 빨리 정비소에서 점검받으세요.",
        aliases = listOf("조향", "스티어링", "핸들경고등", "mdps"),
    ),
    ABS(
        "ABS 경고등", Urgency.SHOP_SOON, "바퀴 잠김 방지 장치에 이상이 있을 때",
        "일반 브레이크는 작동하지만, 급제동할 때 바퀴가 잠겨 미끄러질 수 있어요.",
        "급제동을 피하고 빨리 정비소에서 점검받으세요. 브레이크 경고등도 함께 켜지면 즉시 차를 세우세요.",
        aliases = listOf("abs", "에이비에스"),
    ),
    ESC(
        "ESC 경고등", Urgency.SHOP_SOON, "차체 자세 제어 장치에 이상이 있을 때",
        "미끄러운 길에서 차가 덜 안정적일 수 있어요. 주행 중 잠깐 깜빡이는 건 ESC가 작동 중이라는 뜻이라 정상이에요.",
        "미끄러운 길에서는 속도를 줄이고 빨리 정비소에서 점검받으세요.",
        aliases = listOf("esc", "차체자세"),
    ),
    EPB(
        "전자식 주차 브레이크 경고등", Urgency.SHOP_SOON, "전자식 주차 브레이크에 이상이 있을 때",
        "주차 브레이크가 채워지거나 풀리지 않을 수 있어요.",
        "주차할 때는 평지에 세우고 빨리 정비소에서 점검받으세요.",
        aliases = listOf("주차브레이크", "epb"),
    ),
    EV_SYSTEM(
        "전기차 시스템 경고등", Urgency.SHOP_SOON, "구동 모터나 고전압 배터리 같은 전기차 시스템에 이상이 있을 때",
        "전기차 구동 장치에서 이상이 감지됐어요. 출력이 제한될 수 있어요.",
        "운행을 줄이고 빨리 정비소에서 점검받으세요.",
        aliases = listOf("전기차시스템", "ev시스템"),
    ),
    HYBRID_SYSTEM(
        "하이브리드 시스템 경고등", Urgency.SHOP_SOON, "하이브리드 구동 장치에 이상이 있을 때",
        "모터나 고전압 배터리 같은 하이브리드 장치에서 이상이 감지됐어요.",
        "운행을 줄이고 빨리 정비소에서 점검받으세요.",
        aliases = listOf("하이브리드시스템", "하이브리드경고등"),
    ),
    TPMS(
        "타이어 공기압 경고등", Urgency.CHECK, "타이어 공기압이 낮을 때",
        "한 개 이상의 타이어 공기압이 낮아요. 1분쯤 깜빡이다 계속 켜져 있으면 공기압 감지 장치 이상이에요.",
        "가까운 주유소나 정비소에서 공기압을 확인하고 채우세요. 그때까지 급제동과 급회전은 피하세요.",
        Part.TIRE, listOf("타이어경고등", "공기압경고등"),
    ),
    DPF(
        "배기가스 필터 경고등", Urgency.CHECK, "디젤 매연 필터에 매연이 많이 쌓였을 때",
        "디젤 차의 매연 필터(DPF)는 일정 속도 이상으로 한동안 달리면 스스로 청소돼요.",
        "취급설명서 안내처럼 일정 속도 이상으로 한동안 주행하면 꺼져요. 계속 켜져 있으면 점검받으세요.",
        aliases = listOf("dpf", "매연", "배기가스필터"),
    ),
    MASTER(
        "마스터 경고등", Urgency.CHECK, "확인이 필요한 경고 메시지가 있을 때",
        "계기판에 함께 뜬 메시지가 무엇인지 알려 주는 표시예요.",
        "계기판 메시지를 확인하세요.",
        aliases = listOf("마스터경고등", "느낌표"),
    ),
}

/** 지금 안내할 경고등(급한 순). P단의 브레이크 경고등은 주차 브레이크 표시일 수 있어 뺀다. */
fun activeWarnings(lit: Set<WarningLight>, parked: Boolean): List<WarningLight> =
    lit.filterNot { parked && it == WarningLight.BRAKE }.sortedBy { it.ordinal }

/** 이번에 새로 켜져서 아직 말하지 않은 경고등. */
fun newlyLit(active: List<WarningLight>, announced: Set<WarningLight>): List<WarningLight> = active.filterNot { it in announced }

fun announceWarning(light: WarningLight): String = "${subject(light.koreanName)} 켜졌어요. ${light.action}"

private fun normalized(text: String) = text.lowercase().filterNot { it.isWhitespace() }

/** 질문에 이름이 나온 경고등(띄어쓰기 무시). */
fun findWarningLights(text: String?): List<WarningLight> {
    val t = normalized(text.orEmpty())
    if (t.isEmpty()) return emptyList()
    return WarningLight.entries.filter { l -> normalized(l.koreanName) in t || l.aliases.any { it in t } }
}

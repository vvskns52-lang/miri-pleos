package com.chacare.domain

/**
 * 차종별 권장 주기 — 현대·기아·제네시스 공식 한국어 취급설명서 「서비스 점검 주기 · 통상 조건」 표 기준.
 *
 * 출처(2026-11 확인, 차종별 최신 연식):
 *  - https://ownersmanual.hyundai.com  (KOREA · 한국어)
 *  - https://ownersmanual.kia.com      (Korea · 한국어)
 *  - https://ownersmanual.genesis.com  (KOREA · 한국어)
 *
 * 모든 차종 표에 공통인 항목(브레이크액·에어클리너·냉각수·패드 등)은 [COMMON_ENGINE]/[COMMON_EV]에,
 * 차종마다 다른 항목(엔진오일·점화플러그·공조 필터·벨트·배터리)은 [MODELS]의 각 매뉴얼에 적는다.
 * 매뉴얼이 개정되면 이 파일의 값만 고치면 된다.
 */
enum class IntervalKind { REPLACE, INSPECT, NONE }

data class Interval(
    val km: Int?,
    val months: Int?,
    val kind: IntervalKind = IntervalKind.REPLACE,
    val note: String? = null,
)

enum class Brand(val label: String) { HYUNDAI("현대"), KIA("기아"), GENESIS("제네시스") }

/** 취급설명서 하나(차종 코드·연식)와 그 표에서 읽은 차종별 값. */
data class ManualRef(val code: String, val year: Int, val specific: Map<Part, Interval>) {
    val label get() = "$code $year"
}

data class VehicleModel(
    val id: String,
    val brand: Brand,
    val name: String,
    val manuals: Map<Powertrain, ManualRef>,
) {
    val powertrains: Set<Powertrain> get() = manuals.keys
}

private fun replace(km: Int?, months: Int? = null, note: String? = null) = Interval(km, months, IntervalKind.REPLACE, note)
private fun inspect(km: Int?, months: Int? = null, note: String? = null) = Interval(km, months, IntervalKind.INSPECT, note)
private fun none(note: String) = Interval(null, null, IntervalKind.NONE, note)

private val CABIN_12M = replace(null, 12)
private val CABIN_KIA = replace(15_000, 12)
private val CABIN_12M_INSPECT = inspect(null, 12)
private val BELT_INSPECT = inspect(90_000, 72, "최초 9만km 또는 72개월 점검, 이후 매 3만km 또는 24개월마다 점검")
private val BATTERY_DAILY = none("교체 주기 없음 · 상태를 수시로 점검")
private val BATTERY_12K = inspect(12_000, 12)
private val BATTERY_10K = inspect(10_000, 12)
private fun hsgBelt(km: Int) = replace(km, 48, "하이브리드 스타트&제너레이터(HSG) 구동벨트")
private val NOT_IN_MANUAL = "취급설명서에 주기 없음"

/** 엔진 차(가솔린·하이브리드) 취급설명서에 공통으로 나오는 값. */
private val COMMON_ENGINE: Map<Part, Interval> = linkedMapOf(
    Part.ENGINE_AIR_FILTER to replace(40_000, note = "매 1만km 점검"),
    Part.BRAKE_FLUID to replace(50_000),
    Part.BRAKE_PAD to inspect(10_000, note = "상태에 따라 교체"),
    Part.COOLANT to replace(40_000, 24, "최초 교체는 20만km 또는 10년, 이후 매 4만km 또는 2년"),
    Part.TRANSMISSION_OIL to none("무점검·무교체 (가혹 조건은 10만km마다 교체)"),
    Part.TIRE to inspect(10_000, note = "타이어 위치 교환 주기 · 마모 한계선에 닿으면 교체"),
    Part.WIPER_BLADE to replace(20_000, 12, "$NOT_IN_MANUAL · 기아 렌터카 정비 안내 기준"),
    Part.WASHER_FLUID to none("부족 경고가 뜨면 보충"),
)

/** 전기차 취급설명서에 공통으로 나오는 값. */
private val COMMON_EV: Map<Part, Interval> = linkedMapOf(
    Part.BRAKE_FLUID to replace(50_000),
    Part.BRAKE_PAD to inspect(10_000, note = "상태에 따라 교체"),
    Part.COOLANT to replace(40_000, 24, "최초 교환은 20만km 또는 10년, 이후 매 4만km 또는 2년"),
    Part.REDUCER_OIL to inspect(60_000, note = "통상 조건은 점검만 · 가혹 조건은 12만km마다 교체"),
    Part.TIRE to inspect(10_000, note = "타이어 위치 교환 주기 · 마모 한계선에 닿으면 교체"),
    Part.WIPER_BLADE to replace(20_000, 12, "$NOT_IN_MANUAL · 기아 렌터카 정비 안내 기준"),
    Part.WASHER_FLUID to none("부족 경고가 뜨면 보충"),
)

/** 차종을 모를 때: 공통값 + 차종별 항목은 여러 매뉴얼 중 짧은 쪽(보수적으로). */
private val GENERIC_ENGINE: Map<Part, Interval> = linkedMapOf(
    Part.ENGINE_OIL to replace(10_000, 12),
    Part.SPARK_PLUG to replace(70_000),
    Part.CABIN_FILTER to CABIN_12M,
    Part.DRIVE_BELT to BELT_INSPECT,
    Part.BATTERY to BATTERY_12K,
)
private val GENERIC_EV: Map<Part, Interval> = linkedMapOf(
    Part.CABIN_FILTER to CABIN_12M,
    Part.BATTERY to BATTERY_10K,
)

private fun gas(code: String, year: Int, oilKm: Int, plugKm: Int, cabin: Interval = CABIN_12M, battery: Interval = BATTERY_DAILY, belt: Interval = BELT_INSPECT) =
    ManualRef(code, year, linkedMapOf(
        Part.ENGINE_OIL to replace(oilKm, 12), Part.SPARK_PLUG to replace(plugKm),
        Part.CABIN_FILTER to cabin, Part.DRIVE_BELT to belt, Part.BATTERY to battery,
    ))

private fun ev(code: String, year: Int, battery: Interval = BATTERY_10K, cabin: Interval = CABIN_12M) =
    ManualRef(code, year, linkedMapOf(Part.CABIN_FILTER to cabin, Part.BATTERY to battery))

private val ICE = Powertrain.ICE
private val HEV = Powertrain.HYBRID
private val EV = Powertrain.EV

val MODELS: List<VehicleModel> = listOf(
    // 현대
    VehicleModel("casper", Brand.HYUNDAI, "캐스퍼", mapOf(
        ICE to gas("AX", 2027, 15_000, 160_000, belt = none("일상 점검 · 교체 주기 없음")),
        EV to ev("AXEV", 2027))),
    VehicleModel("avante", Brand.HYUNDAI, "아반떼", mapOf(
        ICE to gas("CN8", 2027, 15_000, 150_000),
        HEV to gas("CN8KH", 2027, 15_000, 75_000, belt = hsgBelt(105_000)))),
    VehicleModel("sonata", Brand.HYUNDAI, "쏘나타", mapOf(
        ICE to gas("DN8", 2026, 15_000, 165_000),
        HEV to gas("DN8HEV", 2026, 15_000, 165_000, belt = hsgBelt(105_000)))),
    VehicleModel("grandeur", Brand.HYUNDAI, "그랜저", mapOf(
        ICE to gas("GN7", 2027, 15_000, 160_000),
        HEV to gas("GN7HEV", 2027, 10_000, 75_000))),
    VehicleModel("kona", Brand.HYUNDAI, "코나", mapOf(
        ICE to gas("SX2", 2027, 10_000, 75_000),
        HEV to gas("SX2HEV", 2027, 15_000, 75_000, belt = hsgBelt(105_000)),
        EV to ev("SX2EV", 2025))),
    VehicleModel("tucson", Brand.HYUNDAI, "투싼", mapOf(
        ICE to gas("NX5", 2027, 10_000, 75_000, cabin = CABIN_12M_INSPECT, battery = BATTERY_12K),
        HEV to gas("NX5KP", 2027, 10_000, 75_000, belt = replace(100_000, 48, "워터펌프 구동벨트")))),
    VehicleModel("santafe", Brand.HYUNDAI, "싼타페", mapOf(
        ICE to gas("MX5", 2026, 10_000, 70_000, battery = BATTERY_12K),
        HEV to gas("MX5HEV", 2026, 10_000, 75_000, battery = BATTERY_12K, belt = hsgBelt(100_000)))),
    VehicleModel("palisade", Brand.HYUNDAI, "팰리세이드", mapOf(
        ICE to gas("LX3", 2027, 10_000, 160_000, cabin = CABIN_12M_INSPECT, battery = BATTERY_12K),
        HEV to gas("LX3HEV", 2027, 10_000, 160_000, cabin = CABIN_12M_INSPECT))),
    VehicleModel("staria", Brand.HYUNDAI, "스타리아", mapOf(
        ICE to gas("US4", 2026, 15_000, 160_000, cabin = CABIN_12M_INSPECT),
        HEV to gas("US4HEV", 2026, 10_000, 75_000, belt = hsgBelt(100_000)))),
    VehicleModel("ioniq5", Brand.HYUNDAI, "아이오닉 5", mapOf(EV to ev("NE1", 2027, battery = BATTERY_12K))),
    VehicleModel("ioniq6", Brand.HYUNDAI, "아이오닉 6", mapOf(EV to ev("CE1", 2025))),
    VehicleModel("ioniq9", Brand.HYUNDAI, "아이오닉 9", mapOf(EV to ev("ME", 2027, cabin = replace(15_000, 12)))),
    // 기아
    VehicleModel("morning", Brand.KIA, "모닝", mapOf(ICE to gas("JA", 2027, 15_000, 150_000, cabin = CABIN_KIA))),
    VehicleModel("ray", Brand.KIA, "레이", mapOf(
        ICE to gas("TAM", 2027, 15_000, 160_000, cabin = CABIN_KIA),
        EV to ev("TAMEV", 2027, battery = BATTERY_12K, cabin = CABIN_KIA))),
    VehicleModel("k5", Brand.KIA, "K5", mapOf(
        ICE to gas("DL3", 2027, 15_000, 150_000, cabin = CABIN_KIA),
        HEV to gas("DL3KH", 2027, 15_000, 150_000, cabin = CABIN_KIA, belt = hsgBelt(105_000)))),
    VehicleModel("k8", Brand.KIA, "K8", mapOf(
        ICE to gas("GL3", 2027, 15_000, 150_000, cabin = CABIN_KIA),
        HEV to gas("GL3KH", 2027, 10_000, 70_000, cabin = CABIN_KIA, belt = hsgBelt(100_000)))),
    VehicleModel("seltos", Brand.KIA, "셀토스", mapOf(ICE to gas("SP2", 2026, 10_000, 70_000, cabin = CABIN_KIA))),
    VehicleModel("sportage", Brand.KIA, "스포티지", mapOf(
        ICE to gas("NQ5", 2027, 10_000, 70_000, cabin = CABIN_KIA),
        HEV to gas("NQ5KH", 2027, 10_000, 70_000, cabin = CABIN_KIA, belt = hsgBelt(100_000)))),
    VehicleModel("sorento", Brand.KIA, "쏘렌토", mapOf(
        ICE to gas("MQ4", 2027, 10_000, 70_000, cabin = CABIN_KIA),
        HEV to gas("MQ4KH", 2027, 10_000, 70_000, cabin = CABIN_KIA, belt = hsgBelt(100_000)))),
    VehicleModel("carnival", Brand.KIA, "카니발", mapOf(
        ICE to gas("KA4", 2027, 15_000, 150_000, cabin = CABIN_KIA),
        HEV to gas("KA4KH", 2027, 10_000, 70_000, cabin = CABIN_KIA, belt = hsgBelt(100_000)))),
    VehicleModel("ev3", Brand.KIA, "EV3", mapOf(EV to ev("SV1", 2027, battery = BATTERY_12K, cabin = CABIN_KIA))),
    VehicleModel("ev4", Brand.KIA, "EV4", mapOf(EV to ev("CT1", 2027, battery = BATTERY_12K, cabin = CABIN_KIA))),
    VehicleModel("ev6", Brand.KIA, "EV6", mapOf(EV to ev("CV1", 2027, battery = BATTERY_12K, cabin = CABIN_KIA))),
    VehicleModel("ev9", Brand.KIA, "EV9", mapOf(EV to ev("MV1", 2027, battery = BATTERY_12K, cabin = CABIN_KIA))),
    // 제네시스
    VehicleModel("g70", Brand.GENESIS, "G70", mapOf(ICE to gas("IK", 2026, 10_000, 80_000))),
    VehicleModel("g80", Brand.GENESIS, "G80", mapOf(
        ICE to gas("RG3", 2027, 10_000, 70_000, battery = BATTERY_12K),
        EV to ev("RG3EV", 2027))),
    VehicleModel("g90", Brand.GENESIS, "G90", mapOf(ICE to gas("RS4", 2027, 10_000, 80_000))),
    VehicleModel("gv60", Brand.GENESIS, "GV60", mapOf(EV to ev("JW", 2027))),
    VehicleModel("gv70", Brand.GENESIS, "GV70", mapOf(
        ICE to gas("JK", 2027, 10_000, 80_000),
        EV to ev("JKEV", 2027))),
    VehicleModel("gv80", Brand.GENESIS, "GV80", mapOf(ICE to gas("JX", 2026, 10_000, 80_000, battery = BATTERY_12K))),
)

fun findModel(id: String?): VehicleModel? = MODELS.firstOrNull { it.id == id }

/** 차종 매뉴얼 값이 우선, 없으면 공통값, 차종을 모르면 보수적 기본값. */
fun intervalsFor(powertrain: Powertrain, model: VehicleModel?): Map<Part, Interval> {
    val common = if (powertrain == Powertrain.EV) COMMON_EV else COMMON_ENGINE
    val specific = model?.manuals?.get(powertrain)?.specific
        ?: if (powertrain == Powertrain.EV) GENERIC_EV else GENERIC_ENGINE
    return common + specific
}

/** 상세 화면에 보여줄 근거 한 줄. */
fun intervalSource(part: Part, model: VehicleModel?, powertrain: Powertrain): String {
    val brand = model?.brand?.label ?: "현대·기아·제네시스"
    val interval = intervalsFor(powertrain, model)[part]
    val manual = model?.manuals?.get(powertrain)
    val base = when {
        interval?.note?.startsWith(NOT_IN_MANUAL) == true -> interval.note
        manual == null -> "목록에 없는 차 · 현대·기아·제네시스 취급설명서 중 보수적인 값"
        else -> "$brand ${model.name} 취급설명서 (${manual.label} · 통상 조건)"
    }
    val note = interval?.note?.takeUnless { it.startsWith(NOT_IN_MANUAL) }
    return listOfNotNull(base, note).joinToString(" · ")
}

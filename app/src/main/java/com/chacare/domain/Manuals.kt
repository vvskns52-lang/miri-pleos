package com.chacare.domain

/**
 * 차종별 권장 교환 주기.
 *
 * 출처(2026-11 확인):
 *  - 현대자동차그룹 공식 「자동차 소모품의 모든 것」
 *    https://www.hyundaimotorgroup.com/ko/story/CONT0000000000136795
 *  - 기아 공식 정비 안내 https://rental.kia.com/m/customerService/carServiceGuide
 *
 * 현대·기아·제네시스는 공식 주기가 차종보다 동력원(가솔린·하이브리드·전기)에 따라 정해져서
 * 기본값은 [BASELINE]에 두고, 특정 차종 매뉴얼이 다를 때만 [VehicleModel.overrides]에 적는다.
 * 매뉴얼이 바뀌면 이 파일의 값만 고치면 된다.
 */
data class Interval(val km: Int?, val months: Int?)

enum class Brand(val label: String) { HYUNDAI("현대"), KIA("기아"), GENESIS("제네시스") }

data class VehicleModel(
    val id: String,
    val brand: Brand,
    val name: String,
    val powertrains: Set<Powertrain>,
    val overrides: Map<Powertrain, Map<Part, Interval>> = emptyMap(),
)

private val ENGINE_CAR: Map<Part, Interval> = linkedMapOf(
    Part.ENGINE_OIL to Interval(15_000, 12),
    Part.ENGINE_AIR_FILTER to Interval(15_000, 12),
    Part.CABIN_FILTER to Interval(20_000, 12),
    Part.BRAKE_FLUID to Interval(50_000, null),
    Part.BRAKE_PAD to Interval(50_000, null),
    Part.SPARK_PLUG to Interval(160_000, null),
    Part.COOLANT to Interval(40_000, 24),
    Part.TRANSMISSION_OIL to Interval(100_000, null),
    Part.DRIVE_BELT to Interval(90_000, 72),
    Part.BATTERY to Interval(80_000, null),
    Part.WIPER_BLADE to Interval(20_000, 12),
    Part.TIRE to Interval(50_000, null),
    Part.WASHER_FLUID to Interval(null, null),
)

private val ELECTRIC_CAR: Map<Part, Interval> = linkedMapOf(
    Part.CABIN_FILTER to Interval(20_000, 12),
    Part.BRAKE_FLUID to Interval(50_000, null),
    Part.BRAKE_PAD to Interval(50_000, null),
    Part.COOLANT to Interval(60_000, null),
    Part.REDUCER_OIL to Interval(100_000, null),
    Part.BATTERY to Interval(80_000, null),
    Part.WIPER_BLADE to Interval(20_000, 12),
    Part.TIRE to Interval(50_000, null),
    Part.WASHER_FLUID to Interval(null, null),
)

val BASELINE: Map<Powertrain, Map<Part, Interval>> = mapOf(
    Powertrain.ICE to ENGINE_CAR,
    Powertrain.HYBRID to ENGINE_CAR,
    Powertrain.EV to ELECTRIC_CAR,
)

/** 상세 화면에 보여줄 근거 한 줄. */
fun intervalSource(part: Part): String = when (part) {
    Part.ENGINE_OIL -> "현대자동차그룹 공식 안내 · 일반 조건 (가혹 조건은 7,500km 또는 6개월)"
    Part.ENGINE_AIR_FILTER -> "현대자동차그룹 공식 안내 · 엔진오일 교체 때 함께 교체 권장"
    Part.SPARK_PLUG -> "현대자동차그룹 공식 안내"
    Part.BRAKE_FLUID -> "현대자동차그룹 공식 안내"
    Part.COOLANT -> "현대자동차그룹 공식 안내 · 최초 20만km 또는 10년, 이후 4만km 또는 2년 (전기차는 기아 공식 정비 안내 6만km 점검)"
    Part.TRANSMISSION_OIL -> "현대자동차그룹 공식 안내 · 일반 조건 무교환, 가혹 조건 10만km"
    Part.DRIVE_BELT -> "현대자동차그룹 공식 안내 · 9만km 또는 72개월에 첫 점검, 이후 3만km 또는 24개월마다 확인"
    Part.CABIN_FILTER, Part.WIPER_BLADE -> "기아 공식 정비 안내"
    Part.BRAKE_PAD -> "기아 공식 정비 안내 · 5만km 점검 후 교환"
    Part.BATTERY -> "기아 공식 정비 안내 · 8만km 또는 방전 3회 이상 시 점검 후 교환"
    Part.REDUCER_OIL -> "기아 공식 정비 안내 · 전기차 전용"
    Part.TIRE -> "일반 권장 주기 · 마모 한계선에 닿으면 바로 교체"
    Part.WASHER_FLUID -> "부족 경고가 뜨면 보충"
}

private val ENGINE = setOf(Powertrain.ICE, Powertrain.HYBRID)
private val GAS = setOf(Powertrain.ICE)
private val ELECTRIC = setOf(Powertrain.EV)

val MODELS: List<VehicleModel> = listOf(
    VehicleModel("casper", Brand.HYUNDAI, "캐스퍼", GAS + ELECTRIC),
    VehicleModel("avante", Brand.HYUNDAI, "아반떼", ENGINE),
    VehicleModel("sonata", Brand.HYUNDAI, "쏘나타", ENGINE),
    VehicleModel("grandeur", Brand.HYUNDAI, "그랜저", ENGINE),
    VehicleModel("kona", Brand.HYUNDAI, "코나", ENGINE + ELECTRIC),
    VehicleModel("tucson", Brand.HYUNDAI, "투싼", ENGINE),
    VehicleModel("santafe", Brand.HYUNDAI, "싼타페", ENGINE),
    VehicleModel("palisade", Brand.HYUNDAI, "팰리세이드", ENGINE),
    VehicleModel("staria", Brand.HYUNDAI, "스타리아", ENGINE),
    VehicleModel("ioniq5", Brand.HYUNDAI, "아이오닉 5", ELECTRIC),
    VehicleModel("ioniq6", Brand.HYUNDAI, "아이오닉 6", ELECTRIC),
    VehicleModel("ioniq9", Brand.HYUNDAI, "아이오닉 9", ELECTRIC),
    VehicleModel("morning", Brand.KIA, "모닝", GAS),
    VehicleModel("ray", Brand.KIA, "레이", GAS + ELECTRIC),
    VehicleModel("k5", Brand.KIA, "K5", ENGINE),
    VehicleModel("k8", Brand.KIA, "K8", ENGINE),
    VehicleModel("seltos", Brand.KIA, "셀토스", GAS),
    VehicleModel("sportage", Brand.KIA, "스포티지", ENGINE),
    VehicleModel("sorento", Brand.KIA, "쏘렌토", ENGINE),
    VehicleModel("carnival", Brand.KIA, "카니발", ENGINE),
    VehicleModel("ev3", Brand.KIA, "EV3", ELECTRIC),
    VehicleModel("ev4", Brand.KIA, "EV4", ELECTRIC),
    VehicleModel("ev6", Brand.KIA, "EV6", ELECTRIC),
    VehicleModel("ev9", Brand.KIA, "EV9", ELECTRIC),
    VehicleModel("g70", Brand.GENESIS, "G70", GAS),
    VehicleModel("g80", Brand.GENESIS, "G80", GAS + ELECTRIC),
    VehicleModel("g90", Brand.GENESIS, "G90", GAS),
    VehicleModel("gv60", Brand.GENESIS, "GV60", ELECTRIC),
    VehicleModel("gv70", Brand.GENESIS, "GV70", GAS + ELECTRIC),
    VehicleModel("gv80", Brand.GENESIS, "GV80", GAS),
)

fun findModel(id: String?): VehicleModel? = MODELS.firstOrNull { it.id == id }

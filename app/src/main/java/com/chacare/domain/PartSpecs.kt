package com.chacare.domain

/** 거리·기간 주기 중 먼저 오는 쪽이 기준. null이면 그 기준을 쓰지 않는다. */
data class PartSpec(
    val part: Part,
    val intervalKm: Int?,
    val intervalMonths: Int?,
    val sensor: SensorKind?,
    val powertrains: Set<Powertrain>,
)

private val ALL = setOf(Powertrain.ICE, Powertrain.EV)
private val ICE_ONLY = setOf(Powertrain.ICE)

/** 일반 국산 승용차 권장값 기준(설계서 5.1). 차종 매뉴얼로 검증 필요. */
val DEFAULT_SPECS: List<PartSpec> = listOf(
    PartSpec(Part.ENGINE_OIL, 10_000, 12, SensorKind.OIL_LEVEL, ICE_ONLY),
    PartSpec(Part.BRAKE_FLUID, 40_000, 24, SensorKind.BRAKE_FLUID_LEVEL, ALL),
    PartSpec(Part.BRAKE_PAD, 40_000, null, null, ALL),
    PartSpec(Part.TRANSMISSION_OIL, 100_000, null, SensorKind.TRANSMISSION_OIL_LEVEL, ICE_ONLY),
    PartSpec(Part.CABIN_FILTER, 15_000, 12, null, ALL),
    PartSpec(Part.WIPER_BLADE, null, 12, null, ALL),
    PartSpec(Part.WASHER_FLUID, null, null, SensorKind.WASHER_WARNING, ALL),
    PartSpec(Part.TIRE, 50_000, null, SensorKind.TIRE_PRESSURE_WARNING, ALL),
    PartSpec(Part.SPARK_PLUG, 100_000, null, null, ICE_ONLY),
    PartSpec(Part.ENGINE_AIR_FILTER, 40_000, null, null, ICE_ONLY),
    // 최초 10년/20만km 이후 2년/4만km가 일반적이지만, 이력을 모르는 경우가 많아 보수적으로 잡는다.
    PartSpec(Part.COOLANT, 40_000, 24, null, ALL),
    PartSpec(Part.BATTERY, null, 48, null, ALL),
    PartSpec(Part.DRIVE_BELT, 80_000, null, null, ICE_ONLY),
)

fun specsFor(powertrain: Powertrain): List<PartSpec> =
    DEFAULT_SPECS.filter { powertrain in it.powertrains }

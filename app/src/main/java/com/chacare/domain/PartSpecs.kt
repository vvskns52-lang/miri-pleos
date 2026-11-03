package com.chacare.domain

/** 거리·기간 주기 중 먼저 오는 쪽이 기준. null이면 그 기준을 쓰지 않는다. */
data class PartSpec(
    val part: Part,
    val intervalKm: Int?,
    val intervalMonths: Int?,
    val sensor: SensorKind?,
)

fun Part.sensor(): SensorKind? = when (this) {
    Part.ENGINE_OIL -> SensorKind.OIL_LEVEL
    Part.BRAKE_FLUID -> SensorKind.BRAKE_FLUID_LEVEL
    Part.TRANSMISSION_OIL -> SensorKind.TRANSMISSION_OIL_LEVEL
    Part.WASHER_FLUID -> SensorKind.WASHER_WARNING
    Part.TIRE -> SensorKind.TIRE_PRESSURE_WARNING
    else -> null
}

/** 차종 매뉴얼 기준 권장 주기. 차종별 값이 없으면 동력원 기본값(Manuals.kt)을 쓴다. */
fun specsFor(powertrain: Powertrain, model: VehicleModel? = null): List<PartSpec> {
    val intervals = BASELINE.getValue(powertrain) + model?.overrides?.get(powertrain).orEmpty()
    return intervals.map { (part, i) -> PartSpec(part, i.km, i.months, part.sensor()) }
}

/** 가솔린 기본 스펙. 센서 → 부품 대응 등 동력원과 무관한 조회에 쓴다. */
val DEFAULT_SPECS: List<PartSpec> get() = specsFor(Powertrain.ICE)

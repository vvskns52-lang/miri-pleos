package com.chacare.domain

/**
 * 거리·기간 주기 중 먼저 오는 쪽이 기준. null이면 그 기준을 쓰지 않는다.
 * [kind]가 INSPECT면 "점검 시기", NONE이면 주기 없이 상태만 본다.
 * [severeBy]가 비어 있지 않으면 그 가혹 조건 때문에 짧아진 주기다.
 */
data class PartSpec(
    val part: Part,
    val intervalKm: Int?,
    val intervalMonths: Int?,
    val sensor: SensorKind?,
    val kind: IntervalKind = IntervalKind.REPLACE,
    val note: String? = null,
    val severeBy: Set<SevereReason> = emptySet(),
)

fun Part.sensor(): SensorKind? = when (this) {
    Part.ENGINE_OIL -> SensorKind.OIL_LEVEL
    Part.BRAKE_FLUID -> SensorKind.BRAKE_FLUID_LEVEL
    Part.TRANSMISSION_OIL -> SensorKind.TRANSMISSION_OIL_LEVEL
    Part.WASHER_FLUID -> SensorKind.WASHER_WARNING
    Part.TIRE -> SensorKind.TIRE_PRESSURE_WARNING
    else -> null
}

/** 차종 취급설명서 기준 권장 주기(Manuals.kt). [detected] 가혹 조건이 있으면 해당 항목은 가혹 조건 주기. */
fun specsFor(powertrain: Powertrain, model: VehicleModel? = null, detected: Set<SevereReason> = emptySet()): List<PartSpec> =
    intervalsFor(powertrain, model, detected).map { (part, applied) ->
        val i = applied.interval
        val none = i.kind == IntervalKind.NONE
        PartSpec(part, i.km.takeUnless { none }, i.months.takeUnless { none }, part.sensor(), i.kind, i.note, applied.severeBy)
    }

/** 센서 → 부품 대응 등 동력원·차종과 무관한 조회에 쓴다. */
val DEFAULT_SPECS: List<PartSpec> get() = specsFor(Powertrain.ICE)

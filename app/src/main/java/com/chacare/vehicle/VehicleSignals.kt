package com.chacare.vehicle

import com.chacare.domain.Powertrain
import com.chacare.domain.SensorKind
import com.chacare.domain.SignalState
import kotlinx.coroutines.flow.Flow

/** 앱이 차에서 읽는 값. 지원되지 않는 값의 Flow는 아무것도 내보내지 않는다. */
interface VehicleSignals {
    val odometerKm: Flow<Double?>
    val brakePressed: Flow<Boolean>
    val wiperOn: Flow<Boolean>
    val hvacOn: Flow<Boolean>
    val parked: Flow<Boolean>
    val sensors: Flow<Pair<SensorKind, SignalState>>
    suspend fun powertrain(): Powertrain?
    fun start()
    fun stop()
}

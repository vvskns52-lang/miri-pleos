package com.chacare.vehicle

import com.chacare.domain.Powertrain
import com.chacare.domain.SensorKind
import com.chacare.domain.SignalState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/** 데모 패널과 테스트에서 차량 값을 직접 넣는 가짜 차(설계서 5.10). */
class DemoVehicleSignals(startOdometerKm: Double? = null, parked: Boolean = true) : VehicleSignals {
    override val odometerKm = MutableStateFlow(startOdometerKm)
    override val brakePressed = MutableSharedFlow<Boolean>(extraBufferCapacity = 4096)
    override val wiperOn = MutableStateFlow(false)
    override val hvacOn = MutableStateFlow(false)
    override val parked = MutableStateFlow(parked)
    override val sensors = MutableSharedFlow<Pair<SensorKind, SignalState>>(extraBufferCapacity = 64)

    override suspend fun powertrain(): Powertrain? = Powertrain.ICE
    override fun start() = Unit
    override fun stop() = Unit

    fun setOdometer(km: Double?) { odometerKm.value = km }
    fun addKm(km: Double) { odometerKm.value = (odometerKm.value ?: 0.0) + km }
    fun pressBrake(times: Int) = repeat(times) { brakePressed.tryEmit(true); brakePressed.tryEmit(false) }
    fun setWiper(on: Boolean) { wiperOn.value = on }
    fun setHvac(on: Boolean) { hvacOn.value = on }
    fun setParked(on: Boolean) { parked.value = on }
    fun emitSensor(kind: SensorKind, state: SignalState) { sensors.tryEmit(kind to state) }
}

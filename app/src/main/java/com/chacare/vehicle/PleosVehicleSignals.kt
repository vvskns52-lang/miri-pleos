package com.chacare.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.area.WheelArea
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.area.WheelValue
import ai.pleos.playground.vehicle.constant.area.WindowValue
import ai.pleos.playground.vehicle.constant.control.OilLevelState
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.constant.control.WiperState
import ai.pleos.playground.vehicle.listener.BrakePedalStatusListener
import ai.pleos.playground.vehicle.listener.DistanceDrivenListener
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import ai.pleos.playground.vehicle.listener.HvacPowerStateListener
import ai.pleos.playground.vehicle.listener.LowWasherFluidWaringListener
import ai.pleos.playground.vehicle.listener.OilLevelStateListener
import ai.pleos.playground.vehicle.listener.TirePressureListener
import ai.pleos.playground.vehicle.listener.WiperStateListener
import android.content.Context
import android.util.Log
import com.chacare.domain.Powertrain
import com.chacare.domain.SensorKind
import com.chacare.domain.SignalState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

private const val TAG = "ChaCare"

/**
 * Pleos Vehicle SDK로 읽는 실제 차량 값. 콜백은 IPC 스레드에서 오므로 Flow로만 넘긴다.
 * 지원되지 않는 값은 onFailed 로그만 남기고 해당 Flow는 비어 있다(설계서 6장).
 */
class PleosVehicleSignals(context: Context) : VehicleSignals {
    private val vehicle = Vehicle(context.applicationContext)

    override val odometerKm = MutableStateFlow<Double?>(null)
    override val brakePressed = MutableSharedFlow<Boolean>(extraBufferCapacity = 256)
    override val wiperOn = MutableStateFlow(false)
    override val hvacOn = MutableStateFlow(false)
    override val parked = MutableStateFlow(true)
    override val sensors = MutableSharedFlow<Pair<SensorKind, SignalState>>(extraBufferCapacity = 64)

    private val front = WindowArea(WindowValue.FRONT_WINDSHIELD)
    private val driverSeat = SeatArea(SeatValue.ROW_1_LEFT)
    private val wheels = listOf(WheelValue.LEFT_FRONT, WheelValue.RIGHT_FRONT, WheelValue.LEFT_REAR, WheelValue.RIGHT_REAR)
        .map { WheelArea(it) }
    private val lowTireThreshold = mutableMapOf<WheelArea, Float>()
    private val lowTireWheels = mutableSetOf<WheelArea>()

    private fun failed(what: String): (Exception) -> Unit = { e -> Log.w(TAG, "$what unavailable: ${e.message}") }

    private val odometerListener = object : DistanceDrivenListener {
        override fun onDistanceDrivenUpdated(value: Float?) { odometerKm.value = value?.toDouble() }
        override fun onFailed(e: Exception) = failed("odometer")(e)
    }
    private val brakeListener = object : BrakePedalStatusListener {
        override fun onBrakePedalStatusUpdated(pressed: Boolean?) { if (pressed != null) brakePressed.tryEmit(pressed) }
        override fun onFailed(e: Exception) = failed("brake")(e)
    }
    private val wiperListener = object : WiperStateListener {
        override fun onWiperStateUpdated(state: WiperState?) { wiperOn.value = state == WiperState.ON }
        override fun onFailed(e: Exception) = failed("wiper")(e)
    }
    private val hvacListener = object : HvacPowerStateListener {
        override fun onHvacPowerStateUpdated(on: Boolean?) { hvacOn.value = on == true }
        override fun onFailed(e: Exception) = failed("hvac")(e)
    }
    private val gearListener = object : GearSelectionListener {
        override fun onSelected(gear: VehicleGear?) { parked.value = gear == VehicleGear.GEAR_PARK }
        override fun onFailed(e: Exception) = failed("gear")(e)
    }
    private val engineOilListener = levelListener(SensorKind.OIL_LEVEL)
    private val brakeFluidListener = levelListener(SensorKind.BRAKE_FLUID_LEVEL)
    private val transmissionOilListener = levelListener(SensorKind.TRANSMISSION_OIL_LEVEL)
    private val washerListener = object : LowWasherFluidWaringListener {
        override fun onLowWasherFluidWaringUpdated(low: Boolean?) {
            sensors.tryEmit(SensorKind.WASHER_WARNING to boolState(low))
        }
        override fun onFailed(e: Exception) = failed("washer")(e)
    }
    private val tireListeners = wheels.associateWith { wheel ->
        object : TirePressureListener {
            override fun onTirePressureUpdated(pressure: Float?) = onTirePressure(wheel, pressure)
            override fun onFailed(e: Exception) = failed("tire")(e)
        }
    }

    private fun levelListener(kind: SensorKind) = object : OilLevelStateListener {
        override fun onOilLevelUpdated(state: OilLevelState?) { sensors.tryEmit(kind to oilState(state)) }
        override fun onFailed(e: Exception) = failed(kind.name)(e)
    }

    private fun oilState(s: OilLevelState?) = when (s) {
        OilLevelState.LOW, OilLevelState.CRITICALLY_LOW -> SignalState.LOW
        OilLevelState.NORMAL, OilLevelState.HIGH -> SignalState.NORMAL
        else -> SignalState.UNKNOWN
    }

    private fun boolState(low: Boolean?) = when (low) {
        true -> SignalState.LOW
        false -> SignalState.NORMAL
        null -> SignalState.UNKNOWN
    }

    /** 네 바퀴 중 하나라도 저압 기준 미만이면 경고. */
    @Synchronized
    private fun onTirePressure(wheel: WheelArea, pressure: Float?) {
        val threshold = lowTireThreshold[wheel] ?: return
        if (pressure == null) return
        if (pressure < threshold) lowTireWheels += wheel else lowTireWheels -= wheel
        sensors.tryEmit(SensorKind.TIRE_PRESSURE_WARNING to if (lowTireWheels.isEmpty()) SignalState.NORMAL else SignalState.LOW)
    }

    override suspend fun powertrain(): Powertrain? = null // 연료 종류는 Vehicle SDK에 없어 첫 실행 설정에서 고른다.

    override fun start() {
        vehicle.initialize()
        with(vehicle.getOdometer()) {
            getCurrentDistanceDriven({ odometerKm.value = it?.toDouble() }, failed("odometer"))
            registerDistanceDriven(odometerListener)
        }
        vehicle.getBrake().registerBrakePedalStatus(brakeListener)
        with(vehicle.getWiper()) {
            getCurrentWiperState(front, { wiperOn.value = it == WiperState.ON }, failed("wiper"))
            registerWiperState(front, wiperListener)
        }
        vehicle.getHVAC().registerHvacPowerState(driverSeat, hvacListener)
        with(vehicle.getGear()) {
            getCurrentGearSelectionState({ parked.value = it == VehicleGear.GEAR_PARK }, failed("gear"))
            registerGearSelection(gearListener)
        }
        with(vehicle.getSafety()) {
            getEngineOilLevelState({ sensors.tryEmit(SensorKind.OIL_LEVEL to oilState(it)) }, failed("engine oil"))
            getBrakeFluidLevelState({ sensors.tryEmit(SensorKind.BRAKE_FLUID_LEVEL to oilState(it)) }, failed("brake fluid"))
            getTransmissionOilLevelState({ sensors.tryEmit(SensorKind.TRANSMISSION_OIL_LEVEL to oilState(it)) }, failed("transmission oil"))
            hasLowWasherFluidWarning({ sensors.tryEmit(SensorKind.WASHER_WARNING to boolState(it)) }, failed("washer"))
            registerEngineOilLevelState(engineOilListener)
            registerBrakeFluidLevelState(brakeFluidListener)
            registerTransmissionOilLevelState(transmissionOilListener)
            registerLowWasherFluidWaringState(washerListener)
        }
        val tire = vehicle.getTire()
        for ((wheel, listener) in tireListeners) {
            tire.getLowTirePressure(wheel, { t ->
                if (t != null) synchronized(this) { lowTireThreshold[wheel] = t }
                tire.registerTirePressure(wheel, listener)
            }, failed("tire"))
        }
    }

    override fun stop() {
        runCatching {
            vehicle.getOdometer().unregisterDistanceDriven(odometerListener)
            vehicle.getBrake().unregisterBrakePedalStatus(brakeListener)
            vehicle.getWiper().unregisterWiperState(front, wiperListener)
            vehicle.getHVAC().unregisterHvacPowerState(driverSeat, hvacListener)
            vehicle.getGear().unregisterGearSelection(gearListener)
            with(vehicle.getSafety()) {
                unregisterEngineOilLevelState(engineOilListener)
                unregisterBrakeFluidLevelState(brakeFluidListener)
                unregisterTransmissionOilLevelState(transmissionOilListener)
                unregisterLowWasherFluidWaringState(washerListener)
            }
            tireListeners.forEach { (wheel, l) -> vehicle.getTire().unregisterTirePressure(wheel, l) }
        }
        vehicle.release()
    }
}

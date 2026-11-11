package com.chacare

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.chacare.domain.SensorKind
import com.chacare.domain.SignalState
import com.chacare.domain.UsageStats
import com.chacare.domain.WarningLight
import com.chacare.location.DEMO_HOME_LAT
import com.chacare.location.DEMO_HOME_LON
import com.chacare.location.DEMO_SHOPS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 시연 영상 녹화용(디버그 빌드 전용): 화면에 시연 패널을 띄우지 않고 adb로 데모 차량을 움직인다.
 * 예: adb shell am broadcast -a com.chacare.DEMO -p com.chacare --es cmd warn --es arg OIL_PRESSURE
 */
class DemoCommandReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val graph = (context.applicationContext as ChaCareApp).graph
        val car = graph.demoVehicle
        val cmd = intent.getStringExtra("cmd")
        val arg = intent.getStringExtra("arg")
        Log.i("ChaCareDemo", "cmd=$cmd arg=$arg")
        scope.launch {
            // 출발 → 주행 → 정차를 한 번 흉내 낸다(엔진이 주행 1건으로 센다).
            suspend fun trip(km: Double, minutes: Long, brakes: Int) {
                car.setParked(false); delay(250)
                car.addKm(km); car.pressBrake(brakes)
                graph.advanceTime(minutes); delay(250)
                car.setParked(true); delay(250)
            }
            when (cmd) {
                "demo_on" -> graph.repo.setDemoMode(true)
                "drive" -> car.setParked(false)
                "park" -> car.setParked(true)
                "ignition" -> graph.newIgnitionCycle()
                "add_km" -> car.addKm(arg?.toDoubleOrNull() ?: 0.0)
                "short_trips" -> { repeat(12) { trip(3.0, 6, 3) }; trip(250.0, 180, 20) }
                // 화면에 "주행 중" 띠가 13번 깜빡이지 않게, 같은 운행 기록을 한 번에 넣는다.
                "short_trips_quiet" -> {
                    car.addKm(286.0)
                    graph.repo.addUsage(UsageStats(drivingMinutes = 252.0, brakePresses = 56, shortTrips = 12, totalTrips = 13))
                }
                "warn" -> car.toggleWarning(WarningLight.valueOf(arg!!))
                "warn_clear" -> car.setWarnings(emptySet())
                "sensor_recover" -> {
                    val kind = SensorKind.valueOf(arg!!)
                    car.emitSensor(kind, SignalState.LOW); delay(200); car.emitSensor(kind, SignalState.NORMAL)
                }
                "answer" -> graph.setNextAnswer(arg)
                "shop_arrive" -> graph.demoLocation.moveTo(DEMO_SHOPS[0].lat, DEMO_SHOPS[0].lon)
                "shop_leave" -> graph.demoLocation.moveTo(DEMO_HOME_LAT, DEMO_HOME_LON)
                "time" -> graph.advanceTime(arg?.toLongOrNull() ?: 0)
                "ask" -> graph.ask(arg)
                "qa_close" -> graph.dismissQa()
                else -> Log.w("ChaCareDemo", "unknown cmd $cmd")
            }
        }
    }

    private companion object {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }
}

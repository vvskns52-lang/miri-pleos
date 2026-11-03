package com.chacare.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.chacare.AppGraph
import com.chacare.ChaCareApp
import com.chacare.data.RecordSource
import com.chacare.data.RecordType
import com.chacare.data.ServiceRecord
import com.chacare.domain.Part
import com.chacare.domain.Powertrain
import com.chacare.domain.SignalState
import com.chacare.engine.MaintenanceService
import com.chacare.location.DEMO_HOME_LAT
import com.chacare.location.DEMO_HOME_LON
import com.chacare.location.DEMO_SHOPS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private sealed interface Screen {
    data object Dashboard : Screen
    data class Detail(val part: Part) : Screen
    data class Edit(val part: Part) : Screen
    data object Demo : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val graph = (application as ChaCareApp).graph
        setContent {
            MaterialTheme(colorScheme = ChaCareColors) {
                // Surface가 글자색(onBackground)을 정해 준다. Box만 쓰면 글자가 검은색이 된다.
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    PermissionsThenStart { MaintenanceService.start(this@MainActivity) }
                    App(graph)
                }
            }
        }
    }
}

@Composable
private fun PermissionsThenStart(onReady: () -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { onReady() }
    LaunchedEffect(Unit) {
        val wanted = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        launcher.launch(wanted.toTypedArray())
    }
}

@Composable
private fun App(graph: AppGraph) {
    val repo = graph.repo
    val scope = rememberCoroutineScope()
    val estimates by remember { repo.estimates() }.collectAsState(initial = emptyList())
    val odometer by repo.odometer.collectAsState(initial = null)
    val demoMode by repo.demoMode.collectAsState(initial = false)
    val parked by graph.parked.collectAsState()
    var onboarded by remember { mutableStateOf<Boolean?>(null) }
    var knownPowertrain by remember { mutableStateOf<Powertrain?>(null) }
    var screen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

    LaunchedEffect(Unit) {
        knownPowertrain = repo.powertrain()
        onboarded = repo.onboarded()
    }

    Column(Modifier.fillMaxSize()) {
        if (!parked && screen != Screen.Demo) DrivingBanner()
        when (onboarded) {
            null -> Unit
            false -> OnboardingScreen(knownPowertrain) { pt, kmAgo ->
                scope.launch {
                    finishOnboarding(graph, pt, kmAgo)
                    onboarded = true
                }
            }
            true -> when (val s = screen) {
                Screen.Dashboard -> DashboardScreen(
                    estimates, odometer, parked,
                    onOpen = { screen = Screen.Detail(it) },
                    onSecretTap = { screen = Screen.Demo },
                )
                is Screen.Detail -> {
                    val records by remember(s.part) { repo.records(s.part) }.collectAsState(initial = emptyList())
                    val factor by remember(s.part) { repo.factor(s.part) }.collectAsState(initial = 1.0)
                    estimates.firstOrNull { it.part == s.part }?.let { e ->
                        PartDetailScreen(e, records, factor, parked, onBack = { screen = Screen.Dashboard }, onEdit = { screen = Screen.Edit(s.part) })
                    }
                }
                is Screen.Edit -> {
                    val records by remember(s.part) { repo.records(s.part) }.collectAsState(initial = emptyList())
                    RecordEditScreen(
                        s.part, records, odometer ?: 0.0, graph.today(), parked,
                        onBack = { screen = Screen.Detail(s.part) },
                        onSave = { r -> scope.launch { if (r.id == 0L) repo.addRecord(r) else repo.updateRecord(r) } },
                        onDelete = { id -> scope.launch { repo.deleteRecord(id) } },
                    )
                }
                Screen.Demo -> DemoPanelScreen(demoMode, parked, demoActions(graph, scope), onBack = { screen = Screen.Dashboard })
            }
        }
    }
}

private suspend fun finishOnboarding(graph: AppGraph, powertrain: Powertrain, kmAgo: Map<Part, Int?>) {
    val repo = graph.repo
    repo.setPowertrain(powertrain)
    val odo = repo.lastOdometer() ?: 0.0
    val today = graph.today()
    for ((part, km) in kmAgo) {
        if (km == null) continue
        // 한 달에 약 1,250km(연 15,000km) 탄다고 보고 날짜를 거꾸로 잡는다.
        val date = today.minusDays((km / 1_250.0 * 30.4).toLong())
        repo.addRecord(ServiceRecord(part = part, type = RecordType.REPLACED, date = date, odometerKm = odo - km, source = RecordSource.ONBOARDING))
    }
    repo.setOnboarded()
}

private fun demoActions(graph: AppGraph, scope: CoroutineScope): DemoActions {
    val car = graph.demoVehicle
    // 상태 값을 켰다 끄는 동작은 엔진이 켜진 상태를 먼저 보도록 잠깐 기다린다.
    fun timed(set: (Boolean) -> Unit, minutes: Long) = scope.launch {
        set(true); delay(300)
        graph.advanceTime(minutes); delay(300)
        set(false)
    }
    return DemoActions(
        setDemoMode = { on -> scope.launch { graph.repo.setDemoMode(on) } },
        addKm = car::addKm,
        pressBrake = car::pressBrake,
        runWiper = { timed(car::setWiper, it) },
        runHvac = { timed(car::setHvac, it) },
        sensorRecovered = { kind ->
            scope.launch { car.emitSensor(kind, SignalState.LOW); delay(200); car.emitSensor(kind, SignalState.NORMAL) }
        },
        sensorLow = { car.emitSensor(it, SignalState.LOW) },
        arriveAtShop = { graph.demoLocation.moveTo(DEMO_SHOPS[0].lat, DEMO_SHOPS[0].lon) },
        leaveShop = { graph.demoLocation.moveTo(DEMO_HOME_LAT, DEMO_HOME_LON) },
        passTime = graph::advanceTime,
        setParked = car::setParked,
        newIgnition = graph::newIgnitionCycle,
    )
}

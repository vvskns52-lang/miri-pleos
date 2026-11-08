package com.chacare.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chacare.domain.Brand
import com.chacare.domain.VehicleModel
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

private const val UI_SCALE = 1.3f
private const val FONT_SCALE = 1.15f

private sealed interface Screen {
    data object Dashboard : Screen
    data class Detail(val part: Part) : Screen
    data class Edit(val part: Part) : Screen
    data object Demo : Screen
    data object Vehicle : Screen
    data object CustomAdd : Screen
    data class CustomDetail(val id: Long) : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val graph = (application as ChaCareApp).graph
        setContent {
            // Pleos 화면은 밀도 1.0(1dp = 1px)이라 그대로면 모든 요소가 작다. 팔 길이 거리에서 읽히도록
            // 화면 전체를 키우고, 글자는 한 번 더 키운다.
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density * UI_SCALE, density.fontScale * FONT_SCALE),
            ) {
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
    val customs by remember { repo.customEstimates() }.collectAsState(initial = emptyList())
    val vehicle by repo.vehicle.collectAsState(initial = null)
    val odometer by repo.odometer.collectAsState(initial = null)
    val severe by repo.severe.collectAsState(initial = null)
    val demoMode by repo.demoMode.collectAsState(initial = false)
    val parked by graph.parked.collectAsState()
    var onboarded by remember { mutableStateOf<Boolean?>(null) }
    var screen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

    val qa by graph.qa.collectAsState()

    LaunchedEffect(Unit) { onboarded = repo.onboarded() }

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize()) {
        if (!parked && screen != Screen.Demo) DrivingBanner()
        when (onboarded) {
            null -> Unit
            false -> OnboardingScreen { model, pt, kmAgo ->
                scope.launch {
                    finishOnboarding(graph, model, pt, kmAgo)
                    onboarded = true
                }
            }
            true -> when (val s = screen) {
                Screen.Dashboard -> DashboardScreen(
                    estimates, customs, vehicle, odometer, severe, parked,
                    onSevereOff = { off -> scope.launch { repo.setSevereOff(off) } },
                    onOpen = { screen = Screen.Detail(it) },
                    onOpenCustom = { screen = Screen.CustomDetail(it) },
                    onAddCustom = { screen = Screen.CustomAdd },
                    onChangeVehicle = { screen = Screen.Vehicle },
                    onAsk = { graph.ask() },
                    onSecretTap = { screen = Screen.Demo },
                )
                is Screen.Detail -> {
                    val records by remember(s.part) { repo.records(s.part) }.collectAsState(initial = emptyList())
                    val factor by remember(s.part) { repo.factor(s.part) }.collectAsState(initial = 1.0)
                    val spec by remember(s.part) { repo.spec(s.part) }.collectAsState(initial = null)
                    estimates.firstOrNull { it.part == s.part }?.let { e ->
                        PartDetailScreen(e, spec, vehicle, records, factor, parked, onBack = { screen = Screen.Dashboard }, onEdit = { screen = Screen.Edit(s.part) })
                    }
                }
                Screen.Vehicle -> VehicleChangeScreen(
                    current = vehicle, parked = parked,
                    onCancel = { screen = Screen.Dashboard },
                    onSave = { model, pt -> scope.launch { repo.setVehicle(model?.id, pt); screen = Screen.Dashboard } },
                )
                Screen.CustomAdd -> CustomAddScreen(
                    odometer ?: 0.0, graph.today(), parked,
                    onBack = { screen = Screen.Dashboard },
                    onSave = { n ->
                        scope.launch {
                            val id = repo.addCustomPart(n.name, n.intervalKm, n.intervalMonths)
                            if (n.lastDate != null) repo.addCustomRecord(id, n.lastDate, n.lastKm ?: 0.0, n.memo)
                            screen = Screen.CustomDetail(id)
                        }
                    },
                )
                is Screen.CustomDetail -> {
                    val records by remember(s.id) { repo.customRecords(s.id) }.collectAsState(initial = emptyList())
                    customs.firstOrNull { it.part.id == s.id }?.let { c ->
                        CustomDetailScreen(
                            c, records, odometer ?: 0.0, graph.today(), parked,
                            onBack = { screen = Screen.Dashboard },
                            onAddRecord = { d, km, memo -> scope.launch { repo.addCustomRecord(s.id, d, km, memo) } },
                            onDeleteRecord = { id -> scope.launch { repo.deleteCustomRecord(id) } },
                            onDeletePart = { scope.launch { repo.deleteCustomPart(s.id); screen = Screen.Dashboard } },
                        )
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
    qa?.let { QaPanel(it, onClose = graph::dismissQa, modifier = Modifier.align(Alignment.BottomCenter)) }
    }
}

/** 차종을 다시 고르면 교환 주기만 바뀌고 교체 기록은 그대로 남는다. */
@Composable
private fun VehicleChangeScreen(
    current: Pair<VehicleModel?, Powertrain>?,
    parked: Boolean,
    onCancel: () -> Unit,
    onSave: (VehicleModel?, Powertrain) -> Unit,
) {
    var choice by remember(current) {
        mutableStateOf(
            current?.let { (m, p) -> VehicleChoice(brand = m?.brand ?: Brand.HYUNDAI, model = m, other = m == null, powertrain = p) }
                ?: VehicleChoice(),
        )
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 28.dp)) {
        Column(Modifier.weight(1f)) { VehicleSelect(choice) { choice = it } }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel) { Text("취소", fontSize = 18.sp) }
            Button(
                onClick = { choice.powertrain?.let { onSave(choice.model, it) } },
                enabled = parked && choice.complete,
            ) { Text("저장", fontSize = 20.sp) }
        }
    }
}

private suspend fun finishOnboarding(graph: AppGraph, model: VehicleModel?, powertrain: Powertrain, kmAgo: Map<Part, Int?>) {
    val repo = graph.repo
    repo.setVehicle(model?.id, powertrain)
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
    // 출발 → 주행 → 정차를 한 번 흉내 낸다(엔진이 주행 1건으로 센다).
    suspend fun trip(km: Double, minutes: Long, brakes: Int) {
        car.setParked(false); delay(250)
        car.addKm(km); car.pressBrake(brakes)
        graph.advanceTime(minutes); delay(250)
        car.setParked(true); delay(250)
    }
    return DemoActions(
        ask = graph::ask,
        shortTrips = { scope.launch { repeat(12) { trip(3.0, 6, 3) }; trip(250.0, 180, 20) } },
        congestion = { scope.launch { trip(230.0, 1_020, 60) } },
        stopAndGo = { scope.launch { trip(220.0, 300, 500) } },
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

package com.chacare.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chacare.domain.SensorKind

/** 발표·시연용 숨김 화면(설계서 5.10). 대시보드 제목을 5번 누르면 열린다. */
class DemoActions(
    val setDemoMode: (Boolean) -> Unit,
    val addKm: (Double) -> Unit,
    val pressBrake: (Int) -> Unit,
    val runWiper: (Long) -> Unit,
    val runHvac: (Long) -> Unit,
    val sensorRecovered: (SensorKind) -> Unit,
    val sensorLow: (SensorKind) -> Unit,
    val arriveAtShop: () -> Unit,
    val leaveShop: () -> Unit,
    val passTime: (Long) -> Unit,
    val setParked: (Boolean) -> Unit,
    val newIgnition: () -> Unit,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DemoPanelScreen(demoMode: Boolean, parked: Boolean, actions: DemoActions, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onBack) { Text("← 대시보드", fontSize = 18.sp) }
            Spacer(Modifier.width(20.dp))
            Text("시뮬레이션 모드", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = CheckColor)
            Spacer(Modifier.weight(1f))
            Text("데모 차량 사용", fontSize = 18.sp)
            Spacer(Modifier.width(8.dp))
            Switch(checked = demoMode, onCheckedChange = actions.setDemoMode)
        }
        if (!demoMode) {
            Spacer(Modifier.height(12.dp))
            Text("지금은 실제 차량(Pleos SDK) 값을 쓰고 있어요. 아래 버튼은 데모 차량을 켜야 동작해요.", fontSize = 18.sp)
        }

        Section("주행") {
            Button({ actions.setParked(!parked) }) { Text(if (parked) "출발하기 (주행)" else "정차하기 (P)", fontSize = 18.sp) }
            FilledTonalButton({ actions.newIgnition() }) { Text("새 시동", fontSize = 18.sp) }
            FilledTonalButton({ actions.addKm(1_000.0) }) { Text("+1,000 km", fontSize = 18.sp) }
            FilledTonalButton({ actions.addKm(5_000.0) }) { Text("+5,000 km", fontSize = 18.sp) }
            FilledTonalButton({ actions.pressBrake(100) }) { Text("브레이크 100회", fontSize = 18.sp) }
            FilledTonalButton({ actions.runWiper(30) }) { Text("와이퍼 30분", fontSize = 18.sp) }
            FilledTonalButton({ actions.runHvac(60) }) { Text("에어컨 60분", fontSize = 18.sp) }
        }
        Section("센서: 부족 → 정상 (교체·보충 감지)") {
            SENSOR_LABELS.forEach { (kind, label) -> FilledTonalButton({ actions.sensorRecovered(kind) }) { Text(label, fontSize = 18.sp) } }
        }
        Section("센서: 부족 경고 켜기") {
            SENSOR_LABELS.forEach { (kind, label) -> OutlinedButton({ actions.sensorLow(kind) }) { Text("$label 부족", fontSize = 18.sp) } }
        }
        Section("정비소 방문") {
            FilledTonalButton({ actions.arriveAtShop() }) { Text("정비소 도착", fontSize = 18.sp) }
            FilledTonalButton({ actions.passTime(30) }) { Text("30분 경과", fontSize = 18.sp) }
            OutlinedButton({ actions.leaveShop() }) { Text("정비소 떠나기", fontSize = 18.sp) }
        }
    }
}

private val SENSOR_LABELS = listOf(
    SensorKind.OIL_LEVEL to "엔진오일",
    SensorKind.BRAKE_FLUID_LEVEL to "브레이크액",
    SensorKind.TRANSMISSION_OIL_LEVEL to "변속기 오일",
    SensorKind.WASHER_WARNING to "워셔액",
    SensorKind.TIRE_PRESSURE_WARNING to "타이어 공기압",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Spacer(Modifier.height(24.dp))
    Text(title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(10.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
}

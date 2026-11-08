package com.chacare.ui

import com.chacare.domain.intervalText
import com.chacare.domain.wonText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chacare.data.RecordType
import com.chacare.data.ServiceRecord
import com.chacare.domain.LifeEstimate
import com.chacare.domain.IntervalKind
import com.chacare.domain.PartSpec
import com.chacare.domain.Powertrain
import com.chacare.domain.VehicleModel
import com.chacare.domain.description
import com.chacare.domain.intervalSource

@Composable
fun PartDetailScreen(
    estimate: LifeEstimate,
    spec: PartSpec?,
    vehicle: Pair<VehicleModel?, Powertrain>?,
    records: List<ServiceRecord>,
    factor: Double,
    parked: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    val last = records.firstOrNull { it.type == RecordType.REPLACED }
    Column(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onBack, enabled = parked) { Text("← 목록", fontSize = 18.sp) }
            Spacer(Modifier.weight(1f))
            Button(onClick = onEdit, enabled = parked) { Text("기록 고치기", fontSize = 18.sp) }
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(Modifier.width(420.dp).card().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                PartImage(estimate.part, 300.dp)
                Spacer(Modifier.height(16.dp))
                Text(estimate.part.koreanName, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(12.dp))
                StatusPill(estimate.status, estimate.kind)
            }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                Text(estimate.part.description, fontSize = 19.sp, color = TextPrimary, lineHeight = 28.sp)
                Spacer(Modifier.height(12.dp))
                if (spec != null) {
                    val severe = if (spec.severeBy.isEmpty()) "" else "  (가혹 조건)"
                    Text(
                        when (spec.kind) {
                            IntervalKind.REPLACE -> "권장 교체 주기  ${intervalText(spec.intervalKm, spec.intervalMonths)}$severe"
                            IntervalKind.INSPECT -> "권장 점검 주기  ${intervalText(spec.intervalKm, spec.intervalMonths)}$severe"
                            IntervalKind.NONE -> "정해진 교체 주기 없음"
                        },
                        fontSize = 19.sp, fontWeight = FontWeight.SemiBold,
                        color = if (spec.severeBy.isEmpty()) TextPrimary else CheckColor,
                    )
                    vehicle?.let { (model, pt) ->
                        Text(intervalSource(estimate.part, model, pt, spec.severeBy), fontSize = 16.sp, color = TextSecondary)
                    }
                    if (spec.severeBy.isNotEmpty()) {
                        Text("가혹 조건 근거: " + spec.severeBy.joinToString(", ") { it.label }, fontSize = 16.sp, color = TextSecondary)
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text("남은 수명", fontSize = 18.sp, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                Text(remainingText(estimate), fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(Modifier.height(16.dp))
                LifeGauge(estimate.remainingRatio, estimate.status)
                factorReason(estimate.part, factor)?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, fontSize = 18.sp, color = Accent)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    if (last == null) "마지막 교체: 기록 없음"
                    else "마지막 교체: ${last.date} · %,d km".format(last.odometerKm.coerceAtLeast(0.0).toLong()),
                    fontSize = 18.sp, color = TextSecondary,
                )
                Spacer(Modifier.height(28.dp))
                Text("이력", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(12.dp))
                if (records.isEmpty()) Text("아직 기록이 없어요", fontSize = 18.sp, color = TextSecondary)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(records, key = { it.id }) { RecordRow(it) }
                }
            }
        }
    }
}

@Composable
fun RecordRow(r: ServiceRecord, trailing: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().card(16.dp).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(r.type.label(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.width(64.dp))
        Text("${r.date} · %,d km".format(r.odometerKm.coerceAtLeast(0.0).toLong()), fontSize = 18.sp, color = TextPrimary)
        r.cost?.let {
            Spacer(Modifier.width(16.dp))
            Text(wonText(it), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
        Spacer(Modifier.weight(1f))
        Text(r.source.label(), fontSize = 16.sp, color = TextSecondary)
        trailing()
    }
}

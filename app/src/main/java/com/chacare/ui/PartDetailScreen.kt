package com.chacare.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chacare.data.RecordType
import com.chacare.data.ServiceRecord
import com.chacare.domain.LifeEstimate

@Composable
fun PartDetailScreen(
    estimate: LifeEstimate,
    records: List<ServiceRecord>,
    factor: Double,
    parked: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    val last = records.firstOrNull { it.type == RecordType.REPLACED }
    Column(Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onBack, enabled = parked) { Text("← 목록", fontSize = 18.sp) }
            Spacer(Modifier.width(20.dp))
            Text(estimate.part.koreanName, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(16.dp))
            Text(estimate.status.label(), color = estimate.status.color(), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Button(onClick = onEdit, enabled = parked) { Text("기록 고치기", fontSize = 18.sp) }
        }
        Spacer(Modifier.height(24.dp))
        LifeGauge(estimate.remainingRatio, estimate.status)
        Spacer(Modifier.height(12.dp))
        Text(remainingText(estimate), fontSize = 22.sp)
        factorReason(estimate.part, factor)?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            if (last == null) "마지막 교체: 기록 없음"
            else "마지막 교체: ${last.date} · %,d km".format(last.odometerKm.toLong()),
            fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(24.dp))
        Text("이력", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        if (records.isEmpty()) Text("아직 기록이 없어요", fontSize = 18.sp)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(records, key = { it.id }) { RecordRow(it) }
        }
    }
}

@Composable
fun RecordRow(r: ServiceRecord, trailing: @Composable () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(r.type.label(), fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(64.dp))
        Text("${r.date} · %,d km".format(r.odometerKm.toLong()), fontSize = 18.sp)
        Spacer(Modifier.weight(1f))
        Text(r.source.label(), fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        trailing()
    }
}

package com.chacare.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chacare.data.RecordSource
import com.chacare.data.RecordType
import com.chacare.data.ServiceRecord
import com.chacare.domain.Part
import java.time.LocalDate

/** 음성 인식이 틀렸거나 답을 놓쳤을 때 기록을 고치는 최소한의 화면(설계서 5.3). */
@Composable
fun RecordEditScreen(
    part: Part,
    records: List<ServiceRecord>,
    currentOdometerKm: Double,
    today: LocalDate,
    parked: Boolean,
    onBack: () -> Unit,
    onSave: (ServiceRecord) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var editing by remember { mutableStateOf<ServiceRecord?>(null) }
    var type by remember { mutableStateOf(RecordType.REPLACED) }
    var dateText by remember { mutableStateOf(today.toString()) }
    var kmText by remember { mutableStateOf(currentOdometerKm.toLong().toString()) }
    var costText by remember { mutableStateOf("") }

    fun startEdit(r: ServiceRecord?) {
        editing = r
        type = r?.type ?: RecordType.REPLACED
        dateText = (r?.date ?: today).toString()
        kmText = (r?.odometerKm ?: currentOdometerKm).toLong().toString()
        costText = r?.cost?.toString() ?: ""
    }

    val date = runCatching { LocalDate.parse(dateText) }.getOrNull()
    val km = kmText.toDoubleOrNull()

    Column(Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onBack, enabled = parked) { Text("← 돌아가기", fontSize = 18.sp) }
            Spacer(Modifier.width(20.dp))
            Text("${part.koreanName} 기록 고치기", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
        Text(if (editing == null) "새 기록 추가" else "기록 수정", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RecordType.entries.forEach { t ->
                FilterChip(selected = type == t, onClick = { type = t }, enabled = parked, label = { Text(t.label(), fontSize = 18.sp) })
            }
            OutlinedTextField(
                dateText, { dateText = it }, enabled = parked, singleLine = true, isError = date == null,
                label = { Text("날짜 (YYYY-MM-DD)") }, modifier = Modifier.width(220.dp),
            )
            OutlinedTextField(
                kmText, { kmText = it.filter(Char::isDigit) }, enabled = parked, singleLine = true, isError = km == null,
                label = { Text("주행거리 km") }, modifier = Modifier.width(180.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            OutlinedTextField(
                costText, { costText = it.filter(Char::isDigit) }, enabled = parked, singleLine = true,
                label = { Text("비용 원 (선택)") }, modifier = Modifier.width(180.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            Button(
                enabled = parked && date != null && km != null,
                onClick = {
                    val base = editing ?: ServiceRecord(part = part, type = type, date = date!!, odometerKm = km!!, source = RecordSource.MANUAL)
                    onSave(base.copy(type = type, date = date!!, odometerKm = km!!, cost = costText.toLongOrNull()?.takeIf { it > 0 }))
                    startEdit(null)
                },
            ) { Text(if (editing == null) "추가" else "저장", fontSize = 18.sp) }
            if (editing != null) TextButton(onClick = { startEdit(null) }, enabled = parked) { Text("취소") }
        }
        Spacer(Modifier.height(24.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(records, key = { it.id }) { r ->
                RecordRow(r) {
                    Spacer(Modifier.width(12.dp))
                    TextButton(onClick = { startEdit(r) }, enabled = parked) { Text("수정") }
                    TextButton(onClick = { onDelete(r.id) }, enabled = parked) { Text("삭제", color = ReplaceColor) }
                }
            }
        }
    }
}

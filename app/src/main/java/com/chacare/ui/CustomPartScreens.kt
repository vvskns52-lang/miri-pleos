package com.chacare.ui

import com.chacare.domain.intervalText
import com.chacare.domain.wonText
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
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
import com.chacare.data.CustomEstimate
import com.chacare.data.CustomRecord
import java.time.LocalDate

/** 새로 추가할 부품 입력값. 주기와 마지막 교체는 비워 둘 수 있다. */
data class NewCustomPart(
    val name: String,
    val intervalKm: Int?,
    val intervalMonths: Int?,
    val lastDate: LocalDate?,
    val lastKm: Double?,
    val memo: String,
    val lastCost: Long?,
)

private fun parseCost(text: String) = text.toLongOrNull()?.takeIf { it > 0 }

@Composable
private fun NumberField(value: String, onValue: (String) -> Unit, label: String, width: Int = 200) {
    OutlinedTextField(
        value, { onValue(it.filter(Char::isDigit)) }, singleLine = true, label = { Text(label) },
        modifier = Modifier.width(width.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}

/** 목록에 없는 부품을 직접 추가한다. 이름만 있어도 저장된다. */
@Composable
fun CustomAddScreen(currentOdometerKm: Double, today: LocalDate, parked: Boolean, onBack: () -> Unit, onSave: (NewCustomPart) -> Unit) {
    var name by remember { mutableStateOf("") }
    var km by remember { mutableStateOf("") }
    var months by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(today.toString()) }
    var odo by remember { mutableStateOf(currentOdometerKm.toLong().toString()) }
    var memo by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull()

    Column(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onBack, enabled = parked) { Text("← 목록", fontSize = 18.sp) }
            Spacer(Modifier.width(20.dp))
            Text("부품 직접 추가", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        Spacer(Modifier.height(8.dp))
        Text("목록에 없는 부품을 적어 두면 언제 무엇을 교체했는지 기록할 수 있어요.", fontSize = 19.sp, color = TextSecondary)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            PartImageRes(R_CUSTOM, 240)
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(name, { name = it }, singleLine = true, label = { Text("부품 이름 (예: 에어컨 가스, 블랙박스)") }, modifier = Modifier.width(520.dp))
                Text("교체 주기 (모르면 비워 두세요)", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField(km, { km = it }, "몇 km마다")
                    NumberField(months, { months = it }, "몇 개월마다")
                }
                Text("마지막으로 교체한 때 (모르면 비워 두세요)", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(date, { date = it }, singleLine = true, isError = date.isNotBlank() && parsedDate == null, label = { Text("날짜 (YYYY-MM-DD)") }, modifier = Modifier.width(220.dp))
                    NumberField(odo, { odo = it }, "그때 주행거리 km")
                    OutlinedTextField(memo, { memo = it }, singleLine = true, label = { Text("메모") }, modifier = Modifier.width(260.dp))
                    NumberField(cost, { cost = it }, "비용 원 (선택)", 180)
                }
                Button(
                    enabled = parked && name.isNotBlank() && (date.isBlank() || parsedDate != null),
                    onClick = {
                        onSave(
                            NewCustomPart(
                                name, km.toIntOrNull()?.takeIf { it > 0 }, months.toIntOrNull()?.takeIf { it > 0 },
                                parsedDate, odo.toDoubleOrNull(), memo, parseCost(cost),
                            ),
                        )
                    },
                ) { Text("추가하기", fontSize = 20.sp) }
            }
        }
    }
}

/** 직접 추가한 부품의 상세: 교체 기록을 쌓고, 필요 없으면 지운다. */
@Composable
fun CustomDetailScreen(
    estimate: CustomEstimate,
    records: List<CustomRecord>,
    currentOdometerKm: Double,
    today: LocalDate,
    parked: Boolean,
    onBack: () -> Unit,
    onAddRecord: (LocalDate, Double, String, Long?) -> Unit,
    onDeleteRecord: (Long) -> Unit,
    onDeletePart: () -> Unit,
) {
    var date by remember { mutableStateOf(today.toString()) }
    var odo by remember { mutableStateOf(currentOdometerKm.toLong().toString()) }
    var memo by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }
    val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull()
    val p = estimate.part

    Column(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 28.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onBack, enabled = parked) { Text("← 목록", fontSize = 18.sp) }
            Spacer(Modifier.weight(1f))
            if (confirmDelete) {
                Text("이 부품과 기록을 모두 지울까요?", fontSize = 18.sp, color = ReplaceColor)
                TextButton(onClick = onDeletePart, enabled = parked) { Text("지우기", color = ReplaceColor, fontSize = 18.sp) }
                TextButton(onClick = { confirmDelete = false }) { Text("취소", fontSize = 18.sp) }
            } else {
                TextButton(onClick = { confirmDelete = true }, enabled = parked) { Text("부품 삭제", color = ReplaceColor, fontSize = 18.sp) }
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(Modifier.width(420.dp).card().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                PartImageRes(R_CUSTOM, 300)
                Spacer(Modifier.height(16.dp))
                Text(p.name, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(12.dp))
                estimate.status?.let { StatusPill(it) } ?: Text("기록만 하는 항목", fontSize = 17.sp, color = TextSecondary)
            }
            Column(Modifier.weight(1f)) {
                Text("교체 주기: ${intervalText(p.intervalKm, p.intervalMonths)} · 직접 입력", fontSize = 18.sp, color = TextSecondary)
                if (estimate.status != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(customRemainingText(estimate), fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Spacer(Modifier.height(12.dp))
                    LifeGauge(estimate.remainingRatio, estimate.status)
                }
                Spacer(Modifier.height(24.dp))
                Text("교체 기록 추가", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(date, { date = it }, singleLine = true, enabled = parked, isError = parsedDate == null, label = { Text("날짜 (YYYY-MM-DD)") }, modifier = Modifier.width(220.dp))
                    NumberField(odo, { odo = it }, "주행거리 km", 180)
                    OutlinedTextField(memo, { memo = it }, singleLine = true, enabled = parked, label = { Text("메모") }, modifier = Modifier.width(240.dp))
                    NumberField(cost, { cost = it }, "비용 원 (선택)", 170)
                    Button(
                        enabled = parked && parsedDate != null && odo.toDoubleOrNull() != null,
                        onClick = { onAddRecord(parsedDate!!, odo.toDouble(), memo, parseCost(cost)); memo = ""; cost = "" },
                    ) { Text("추가", fontSize = 18.sp) }
                }
                Spacer(Modifier.height(24.dp))
                Text("이력", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(10.dp))
                if (records.isEmpty()) Text("아직 기록이 없어요", fontSize = 18.sp, color = TextSecondary)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(records, key = { it.id }) { r ->
                        Row(
                            Modifier.fillMaxWidth().card(16.dp).padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${r.date} · %,d km".format(r.odometerKm.coerceAtLeast(0.0).toLong()), fontSize = 18.sp, color = TextPrimary)
                            Spacer(Modifier.width(16.dp))
                            r.cost?.let { Text(wonText(it), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary); Spacer(Modifier.width(16.dp)) }
                            Text(r.memo, fontSize = 17.sp, color = TextSecondary, modifier = Modifier.weight(1f))
                            TextButton(onClick = { onDeleteRecord(r.id) }, enabled = parked) { Text("삭제", color = ReplaceColor) }
                        }
                    }
                }
            }
        }
    }
}

fun customRemainingText(e: CustomEstimate): String = when {
    e.status == null -> e.last?.let { "마지막 교체 ${it.date}" } ?: "아직 기록이 없어요"
    e.remainingRatio == null -> "교체 이력 정보 없음"
    (e.remainingKm ?: 1.0) <= 0.0 || (e.remainingMonths ?: 1.0) <= 0.0 -> "교체 시기가 지났어요"
    else -> listOfNotNull(
        e.remainingKm?.let { com.chacare.domain.displayKm(it) },
        e.remainingMonths?.let { com.chacare.domain.displayMonths(it) },
    ).joinToString(" · ") + " 남음"
}

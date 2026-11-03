package com.chacare.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chacare.domain.Brand
import com.chacare.domain.MODELS
import com.chacare.domain.Part
import com.chacare.domain.Powertrain
import com.chacare.domain.VehicleModel
import com.chacare.domain.specsFor

/** 차종 선택 상태. model == null && other == true 이면 "목록에 없어요". */
data class VehicleChoice(val brand: Brand = Brand.HYUNDAI, val model: VehicleModel? = null, val other: Boolean = false, val powertrain: Powertrain? = null) {
    val complete get() = (model != null || other) && powertrain != null
}

/** "차종은 무엇인가요?" — 고른 차종 매뉴얼 기준으로 교환 주기가 정해진다. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VehicleSelect(choice: VehicleChoice, onChange: (VehicleChoice) -> Unit) {
    Column {
        Text("차종은 무엇인가요?", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text("고른 차종의 매뉴얼 기준으로 소모품 교환 주기를 맞춰 드려요.", fontSize = 19.sp, color = TextSecondary)
        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Brand.entries.forEach { b ->
                FilterChip(
                    choice.brand == b && !choice.other, { onChange(VehicleChoice(brand = b)) },
                    label = { Text(b.label, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)) },
                )
            }
            Spacer(Modifier.width(12.dp))
            FilterChip(
                choice.other, { onChange(VehicleChoice(brand = choice.brand, other = true)) },
                label = { Text("목록에 없어요", fontSize = 20.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)) },
            )
        }
        Spacer(Modifier.height(16.dp))
        if (!choice.other) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MODELS.filter { it.brand == choice.brand }.forEach { m ->
                    FilterChip(
                        choice.model == m,
                        { onChange(choice.copy(model = m, powertrain = m.powertrains.singleOrNull())) },
                        label = { Text(m.name, fontSize = 19.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)) },
                    )
                }
            }
        }
        val options = when {
            choice.other -> Powertrain.entries.toSet()
            else -> choice.model?.powertrains.orEmpty()
        }
        if (options.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("동력원", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.width(80.dp))
                Powertrain.entries.filter { it in options }.forEach { p ->
                    FilterChip(
                        choice.powertrain == p, { onChange(choice.copy(powertrain = p)) },
                        label = { Text(p.label, fontSize = 19.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)) },
                    )
                }
            }
        }
        val pt = choice.powertrain
        if (choice.complete && pt != null) {
            Spacer(Modifier.height(20.dp))
            IntervalPreview(pt, choice.model)
        }
    }
}

@Composable
private fun IntervalPreview(powertrain: Powertrain, model: VehicleModel?) {
    val specs = specsFor(powertrain, model).associateBy { it.part }
    val highlight = listOf(Part.ENGINE_OIL, Part.CABIN_FILTER, Part.BRAKE_FLUID, Part.COOLANT, Part.REDUCER_OIL)
        .mapNotNull { specs[it] }
        .joinToString("  ·  ") { s -> "${s.part.koreanName} ${intervalText(s.intervalKm, s.intervalMonths)}" }
    Text(
        "${model?.name ?: "일반"} ${powertrain.label} 권장 주기  —  $highlight",
        Modifier.card(16.dp).padding(horizontal = 20.dp, vertical = 14.dp),
        fontSize = 17.sp, color = TextSecondary,
    )
}

fun intervalText(km: Int?, months: Int?): String = listOfNotNull(
    km?.let { "%,dkm".format(it) },
    months?.let { "${it}개월" },
).joinToString(" 또는 ").ifEmpty { "주기 없음" }

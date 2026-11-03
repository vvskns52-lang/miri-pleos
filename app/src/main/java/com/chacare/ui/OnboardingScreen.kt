package com.chacare.ui

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
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chacare.domain.Part
import com.chacare.domain.Powertrain
import com.chacare.domain.VehicleModel
import com.chacare.domain.specsFor

/** 마지막 교체가 대략 몇 km 전이었는지. null = 모름. */
private val KM_AGO_OPTIONS: List<Pair<String, Int?>> = listOf(
    "모름" to null,
    "1천 km 이내" to 500,
    "약 5천 km 전" to 5_000,
    "약 1만 km 전" to 10_000,
    "2만 km 이상 전" to 20_000,
)

/**
 * 1단계: 차종 → 그 차종 매뉴얼 주기 적용.
 * 2단계: 처음 설치하면 교체 이력을 모르므로 대략만 묻는다(설계서 5.3).
 */
@Composable
fun OnboardingScreen(onDone: (VehicleModel?, Powertrain, Map<Part, Int?>) -> Unit) {
    var choice by remember { mutableStateOf(VehicleChoice()) }
    var step by remember { mutableStateOf(0) }
    val answers = remember { mutableStateMapOf<Part, Int?>() }

    Column(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 28.dp)) {
        if (step == 0) {
            Column(Modifier.weight(1f)) { VehicleSelect(choice) { choice = it } }
            Button(onClick = { step = 1 }, enabled = choice.complete) { Text("다음", fontSize = 20.sp) }
            return@Column
        }
        val pt = choice.powertrain ?: return@Column
        Text("마지막으로 언제 교체했나요?", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text("대략이면 충분해요. 모르면 '모름'으로 두셔도 돼요.", fontSize = 19.sp, color = TextSecondary)
        Spacer(Modifier.height(20.dp))
        val parts = specsFor(pt, choice.model).map { it.part }.filter { it != Part.WASHER_FLUID }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(parts) { part ->
                Row(
                    Modifier.fillMaxWidth().card(18.dp).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    PartImage(part, 72.dp)
                    Text(part.koreanName, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.width(160.dp))
                    KM_AGO_OPTIONS.forEach { (label, km) ->
                        FilterChip(
                            answers[part] == km && (km != null || part in answers), { answers[part] = km },
                            label = { Text(label, fontSize = 18.sp, modifier = Modifier.padding(vertical = 8.dp)) },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { step = 0 }) { Text("← 차종 다시 고르기", fontSize = 18.sp) }
            Button(onClick = { onDone(choice.model, pt, parts.associateWith { answers[it] }) }) { Text("시작하기", fontSize = 20.sp) }
        }
    }
}

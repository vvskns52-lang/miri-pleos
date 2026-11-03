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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chacare.domain.Part
import com.chacare.domain.Powertrain
import com.chacare.domain.specsFor

/** 마지막 교체가 대략 몇 km 전이었는지. null = 모름. */
private val KM_AGO_OPTIONS: List<Pair<String, Int?>> = listOf(
    "모름" to null,
    "1천 km 이내" to 500,
    "약 5천 km 전" to 5_000,
    "약 1만 km 전" to 10_000,
    "2만 km 이상 전" to 20_000,
)

/** 처음 설치하면 교체 이력을 모르므로 대략만 묻는다(설계서 5.3). */
@Composable
fun OnboardingScreen(
    knownPowertrain: Powertrain?,
    onDone: (Powertrain, Map<Part, Int?>) -> Unit,
) {
    var powertrain by remember { mutableStateOf(knownPowertrain) }
    val answers = remember { mutableStateMapOf<Part, Int?>() }

    Column(Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp)) {
        Text("차케어를 시작할게요", fontSize = 34.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "소모품을 마지막으로 언제 교체했는지 대략 알려주세요. 모르면 '모름'으로 두셔도 돼요.",
            fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(20.dp))
        if (knownPowertrain == null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("차량 종류", fontSize = 22.sp, modifier = Modifier.width(140.dp))
                FilterChip(powertrain == Powertrain.ICE, { powertrain = Powertrain.ICE }, label = { Text("내연기관·하이브리드", fontSize = 18.sp) })
                FilterChip(powertrain == Powertrain.EV, { powertrain = Powertrain.EV }, label = { Text("전기차", fontSize = 18.sp) })
            }
            Spacer(Modifier.height(16.dp))
        }
        val pt = powertrain
        if (pt != null) {
            val parts = specsFor(pt).map { it.part }.filter { it != Part.WASHER_FLUID }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(parts) { part ->
                    Row(
                        Modifier.fillMaxWidth().card(18.dp).padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        PartImage(part, 72.dp)
                        Text(part.koreanName, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(160.dp))
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
            Button(onClick = { onDone(pt, parts.associateWith { answers[it] }) }) { Text("시작하기", fontSize = 20.sp) }
        }
    }
}

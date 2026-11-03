package com.chacare.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chacare.domain.LifeEstimate
import com.chacare.domain.LifeStatus
import com.chacare.domain.Part
import com.chacare.domain.displayKm
import com.chacare.domain.displayMonths

@Composable
fun DashboardScreen(
    estimates: List<LifeEstimate>,
    odometerKm: Double?,
    parked: Boolean,
    onOpen: (Part) -> Unit,
    onSecretTap: () -> Unit,
) {
    var taps by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 28.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column {
                Text(
                    "차케어",
                    Modifier.clickable(enabled = parked) { if (++taps >= 5) { taps = 0; onSecretTap() } },
                    fontSize = 34.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(summary(estimates), color = TextSecondary, fontSize = 20.sp)
            }
            Spacer(Modifier.weight(1f))
            if (odometerKm != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("누적 주행거리", color = TextSecondary, fontSize = 16.sp)
                    Text("%,d km".format(odometerKm.toLong()), color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 480.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            items(estimates, key = { it.part }) { e -> PartCard(e, enabled = parked) { onOpen(e.part) } }
        }
    }
}

private fun summary(estimates: List<LifeEstimate>): String {
    val replace = estimates.count { it.status == LifeStatus.REPLACE }
    val check = estimates.count { it.status == LifeStatus.CHECK }
    return when {
        replace > 0 -> "교체가 필요한 소모품이 ${replace}개 있어요"
        check > 0 -> "점검해 볼 소모품이 ${check}개 있어요"
        else -> "모든 소모품이 좋은 상태예요"
    }
}

@Composable
private fun PartCard(e: LifeEstimate, enabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.card().clickable(enabled = enabled, onClick = onClick).padding(start = 12.dp, end = 24.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PartImage(e.part, 136.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    e.part.koreanName, Modifier.weight(1f),
                    fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                StatusPill(e.status)
            }
            Spacer(Modifier.height(16.dp))
            LifeGauge(e.remainingRatio, e.status)
            Spacer(Modifier.height(10.dp))
            Text(remainingText(e), fontSize = 18.sp, color = TextSecondary)
        }
    }
}

@Composable
fun LifeGauge(ratio: Double?, status: LifeStatus) {
    Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Track)) {
        if (ratio != null) {
            Box(
                Modifier.fillMaxHeight().fillMaxWidth(ratio.coerceIn(0.0, 1.0).toFloat())
                    .clip(RoundedCornerShape(5.dp)).background(status.color()),
            )
        }
    }
}

fun remainingText(e: LifeEstimate): String = when {
    e.part == Part.WASHER_FLUID -> if (e.status == LifeStatus.REPLACE) "보충이 필요해요" else "부족 경고 없음"
    e.remainingRatio == null -> "교체 이력 정보 없음"
    (e.remainingKm ?: 1.0) <= 0.0 || (e.remainingMonths ?: 1.0) <= 0.0 -> "교체 시기가 지났어요"
    else -> listOfNotNull(
        e.remainingKm?.let { displayKm(it) },
        e.remainingMonths?.let { displayMonths(it) },
    ).joinToString(" · ") + " 남음"
}

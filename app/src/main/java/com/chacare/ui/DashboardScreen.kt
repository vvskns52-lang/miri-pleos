package com.chacare.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chacare.data.CustomEstimate
import com.chacare.data.SevereStatus
import com.chacare.domain.IntervalKind
import com.chacare.domain.LifeEstimate
import com.chacare.domain.LifeStatus
import com.chacare.domain.Part
import com.chacare.domain.Powertrain
import com.chacare.domain.VehicleModel
import com.chacare.domain.bundleOf
import com.chacare.domain.displayKm
import com.chacare.domain.displayMonths
import com.chacare.domain.summary

@Composable
fun DashboardScreen(
    estimates: List<LifeEstimate>,
    customs: List<CustomEstimate>,
    vehicle: Pair<VehicleModel?, Powertrain>?,
    odometerKm: Double?,
    severe: SevereStatus?,
    parked: Boolean,
    onSevereOff: (Boolean) -> Unit,
    onOpen: (Part) -> Unit,
    onOpenCustom: (Long) -> Unit,
    onAddCustom: () -> Unit,
    onChangeVehicle: () -> Unit,
    onAsk: () -> Unit,
    onCosts: () -> Unit,
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
                Text(summary(estimates, customs), color = TextSecondary, fontSize = 20.sp)
            }
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = onCosts, enabled = parked, shape = RoundedCornerShape(50)) {
                Text("정비 비용", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(12.dp))
            // 말로 주고받으므로 주행 중에도 누를 수 있게 둔다.
            Button(onClick = onAsk, shape = RoundedCornerShape(50)) {
                Text("말로 물어보기", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(24.dp))
            if (vehicle != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("내 차", color = TextSecondary, fontSize = 16.sp)
                    Text(
                        "${vehicle.first?.let { "${it.brand.label} ${it.name}" } ?: "일반 차량"} · ${vehicle.second.label}",
                        color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.width(12.dp))
                OutlinedButton(onClick = onChangeVehicle, enabled = parked) { Text("차종 변경", fontSize = 16.sp) }
                Spacer(Modifier.width(32.dp))
            }
            if (odometerKm != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("누적 주행거리", color = TextSecondary, fontSize = 16.sp)
                    Text("%,d km".format(odometerKm.toLong()), color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        val bundle = bundleOf(estimates)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 400.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // 배너도 목록과 함께 스크롤되도록 한 줄 전체를 차지하는 항목으로 넣는다.
            if (severe != null && severe.detected.isNotEmpty()) {
                item(key = "severe", span = { GridItemSpan(maxLineSpan) }) { SevereBanner(severe, parked, onSevereOff) }
            }
            if (bundle.isNotEmpty()) {
                item(key = "bundle", span = { GridItemSpan(maxLineSpan) }) { BundleCard(bundle, parked, onOpen) }
            }
            items(estimates, key = { it.part }) { e ->
                CardShell(enabled = parked, onClick = { onOpen(e.part) }, image = { PartImage(e.part, 136.dp) }) {
                    CardBody(e.part.koreanName, e.part.summary, e.status, e.remainingRatio, remainingText(e), e.kind, severe = e.severeBy.isNotEmpty())
                }
            }
            items(customs, key = { "custom-${it.part.id}" }) { c ->
                CardShell(enabled = parked, onClick = { onOpenCustom(c.part.id) }, image = { PartImageRes(R_CUSTOM, 136) }) {
                    CardBody(c.part.name, "직접 추가한 부품", c.status, c.remainingRatio, customRemainingText(c))
                }
            }
            item(key = "add-custom") {
                Box(
                    Modifier.height(162.dp).clip(RoundedCornerShape(24.dp))
                        .border(2.dp, Divider, RoundedCornerShape(24.dp))
                        .clickable(enabled = parked, onClick = onAddCustom),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("+", fontSize = 40.sp, color = Accent, fontWeight = FontWeight.Light)
                        Text("목록에 없는 부품 직접 추가", fontSize = 19.sp, color = Accent, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/** 시기가 됐거나 곧 되는 항목을 묶어 "정비소 한 번에" 하라고 권한다. */
@Composable
private fun BundleCard(items: List<LifeEstimate>, parked: Boolean, onOpen: (Part) -> Unit) {
    Column(Modifier.fillMaxWidth().card().padding(horizontal = 24.dp, vertical = 18.dp)) {
        Text("다음 정비 때 함께 하세요", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text("정비소 한 번 방문으로 ${items.size}가지를 끝낼 수 있어요", fontSize = 16.sp, color = TextSecondary)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items.forEach { e ->
                Row(
                    Modifier.clip(RoundedCornerShape(16.dp)).background(Track.copy(alpha = 0.5f))
                        .clickable(enabled = parked) { onOpen(e.part) }.padding(end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PartImage(e.part, 64.dp)
                    Column {
                        Text(e.part.koreanName, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text(bundleLabel(e), fontSize = 15.sp, color = if (e.status == LifeStatus.REPLACE) ReplaceColor else CheckColor)
                    }
                }
            }
        }
    }
}

private fun bundleLabel(e: LifeEstimate): String {
    val verb = if (e.kind == IntervalKind.INSPECT) "점검" else "교체"
    return if (e.status == LifeStatus.REPLACE) "$verb 필요" else "곧 $verb"
}

/** 운행 기록으로 판정한 가혹 조건과 근거. 사용자가 끄면 통상 조건으로 돌아간다. */
@Composable
private fun SevereBanner(s: SevereStatus, parked: Boolean, onOff: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(if (s.off) Track else CheckColor.copy(alpha = 0.12f))
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                if (s.off) "가혹 조건 판정을 껐어요 · 통상 조건 주기로 보여드려요"
                else "운행 기록을 보니 가혹 조건이에요 · 취급설명서의 가혹 조건 주기를 적용했어요",
                fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = if (s.off) TextSecondary else TextPrimary,
            )
            Text(
                s.detected.joinToString("   ") { "${it.label} (${s.evidence[it]})" },
                fontSize = 16.sp, color = TextSecondary,
            )
        }
        OutlinedButton(onClick = { onOff(!s.off) }, enabled = parked) {
            Text(if (s.off) "다시 적용" else "통상 조건으로 보기", fontSize = 16.sp)
        }
    }
}

private fun summary(estimates: List<LifeEstimate>, customs: List<CustomEstimate>): String {
    val statuses = estimates.map { it.status } + customs.mapNotNull { it.status }
    val replace = statuses.count { it == LifeStatus.REPLACE }
    val check = statuses.count { it == LifeStatus.CHECK }
    return when {
        replace > 0 -> "교체가 필요한 소모품이 ${replace}개 있어요"
        check > 0 -> "점검해 볼 소모품이 ${check}개 있어요"
        else -> "모든 소모품이 좋은 상태예요"
    }
}

@Composable
private fun CardShell(enabled: Boolean, onClick: () -> Unit, image: @Composable () -> Unit, body: @Composable () -> Unit) {
    Row(
        Modifier.card().clickable(enabled = enabled, onClick = onClick).padding(start = 12.dp, end = 24.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        image()
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) { body() }
    }
}

@Composable
private fun CardBody(
    name: String, subtitle: String, status: LifeStatus?, ratio: Double?, remaining: String,
    kind: IntervalKind = IntervalKind.REPLACE, severe: Boolean = false,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            name, Modifier.weight(1f),
            fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        if (severe) {
            Text("가혹", color = CheckColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
        }
        if (status != null) StatusPill(status, kind)
    }
    Spacer(Modifier.height(4.dp))
    Text(subtitle, fontSize = 16.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    Spacer(Modifier.height(12.dp))
    if (status != null) LifeGauge(ratio, status) else Box(Modifier.fillMaxWidth().height(10.dp))
    Spacer(Modifier.height(10.dp))
    Text(remaining, fontSize = 18.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
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

/** 취급설명서가 정한 종류에 맞춰 "교체까지" / "점검까지" / 주기 없음 설명을 보여준다. */
fun remainingText(e: LifeEstimate): String {
    val verb = if (e.kind == IntervalKind.INSPECT) "점검" else "교체"
    return when {
        e.part == Part.WASHER_FLUID -> if (e.status == LifeStatus.REPLACE) "보충이 필요해요" else "부족 경고 없음"
        e.kind == IntervalKind.NONE -> e.note ?: "교체 주기 없음"
        e.remainingRatio == null -> "$verb 이력 정보 없음"
        (e.remainingKm ?: 1.0) <= 0.0 || (e.remainingMonths ?: 1.0) <= 0.0 -> "$verb 시기가 지났어요"
        else -> "${verb}까지 " + listOfNotNull(
            e.remainingKm?.let { displayKm(it) },
            e.remainingMonths?.let { displayMonths(it) },
        ).joinToString(" · ")
    }
}

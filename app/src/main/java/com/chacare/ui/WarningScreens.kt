package com.chacare.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chacare.data.WarningEvent
import com.chacare.domain.Part
import com.chacare.domain.Urgency
import com.chacare.domain.WarningLight

fun Urgency.color() = when (this) {
    Urgency.STOP_NOW -> ReplaceColor
    Urgency.SHOP_SOON -> CheckColor
    Urgency.CHECK -> Color(0xFFB08900)
}

/** 경고등 그림 대신 급한 정도를 색으로 보여 주는 원형 표시. */
@Composable
fun WarningDot(light: WarningLight, size: Dp = 56.dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(light.urgency.color()),
        contentAlignment = Alignment.Center,
    ) { Text("!", color = Color.White, fontSize = (size.value * 0.5f).sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun UrgencyPill(u: Urgency) {
    Text(
        u.label,
        Modifier.clip(RoundedCornerShape(50)).background(u.color().copy(alpha = 0.12f)).padding(horizontal = 14.dp, vertical = 6.dp),
        color = u.color(), fontSize = 16.sp, fontWeight = FontWeight.Bold,
    )
}

/** 대시보드 맨 위: 가장 급한 경고등과 할 일. */
@Composable
fun WarningBanner(lights: List<WarningLight>, parked: Boolean, onOpen: () -> Unit) {
    val top = lights.first()
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(top.urgency.color().copy(alpha = 0.12f))
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WarningDot(top, 52.dp)
        Spacer(Modifier.width(20.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    top.koreanName + "이 켜졌어요" + if (lights.size > 1) "  외 ${lights.size - 1}개" else "",
                    fontSize = 21.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
                )
                Spacer(Modifier.width(12.dp))
                UrgencyPill(top.urgency)
            }
            Spacer(Modifier.height(4.dp))
            Text(top.action, fontSize = 17.sp, color = TextPrimary, lineHeight = 24.sp)
        }
        Spacer(Modifier.width(16.dp))
        OutlinedButton(onClick = onOpen, enabled = parked) { Text("자세히", fontSize = 16.sp) }
    }
}

/** 지금 켜진 경고등, 켜졌던 기록, 앱이 아는 경고등 전체를 한 화면에. */
@Composable
fun WarningGuideScreen(
    active: List<WarningLight>,
    log: List<WarningEvent>,
    parked: Boolean,
    onBack: () -> Unit,
    onOpenPart: (Part) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 28.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onBack, enabled = parked) { Text("← 목록", fontSize = 18.sp) }
            Spacer(Modifier.width(20.dp))
            Text("경고등 안내", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "계기판 경고등이 켜지면 주행 중에도 바로 말로 알려 드려요. 정비와 관련된 경고등 ${WarningLight.entries.size}가지를 정리했어요.",
            fontSize = 19.sp, color = TextSecondary,
        )
        Spacer(Modifier.height(24.dp))

        if (active.isEmpty()) {
            Text("지금 켜진 경고등은 없어요.", fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = GoodColor)
        } else {
            SectionTitle("지금 켜진 경고등")
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                active.forEach { l -> ActiveCard(l, parked, onOpenPart) }
            }
        }

        if (log.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            SectionTitle("최근 켜진 기록")
            Column(Modifier.fillMaxWidth().card().padding(horizontal = 24.dp, vertical = 12.dp)) {
                log.take(10).forEach { e ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${e.date} · %,d km".format(e.odometerKm.coerceAtLeast(0.0).toLong()), fontSize = 18.sp, color = TextSecondary, modifier = Modifier.width(300.dp))
                        Text(e.light.koreanName, fontSize = 19.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        SectionTitle("경고등 사전")
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            WarningLight.entries.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    pair.forEach { l -> GuideCard(l, Modifier.weight(1f)) }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun ActiveCard(l: WarningLight, parked: Boolean, onOpenPart: (Part) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(l.urgency.color().copy(alpha = 0.10f)).padding(24.dp),
    ) {
        WarningDot(l, 64.dp)
        Spacer(Modifier.width(20.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(l.koreanName, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.width(12.dp))
                UrgencyPill(l.urgency)
            }
            Spacer(Modifier.height(8.dp))
            Text(l.detail, fontSize = 18.sp, color = TextPrimary, lineHeight = 26.sp)
            Spacer(Modifier.height(8.dp))
            Text("이렇게 하세요: ${l.action}", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, lineHeight = 26.sp)
            l.part?.let { p ->
                TextButton(onClick = { onOpenPart(p) }, enabled = parked) { Text("관련 소모품: ${p.koreanName} 보기 →", fontSize = 17.sp) }
            }
        }
    }
}

@Composable
private fun GuideCard(l: WarningLight, modifier: Modifier) {
    Row(modifier.card().padding(20.dp)) {
        WarningDot(l, 44.dp)
        Spacer(Modifier.width(16.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(l.koreanName, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f, fill = false))
                Spacer(Modifier.width(10.dp))
                UrgencyPill(l.urgency)
            }
            Spacer(Modifier.height(6.dp))
            Text("${l.cause} 켜져요.", fontSize = 16.sp, color = TextSecondary)
            Spacer(Modifier.height(4.dp))
            Text(l.action, fontSize = 16.sp, color = TextPrimary, lineHeight = 23.sp)
        }
    }
}

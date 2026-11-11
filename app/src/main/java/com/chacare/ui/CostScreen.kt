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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chacare.domain.CostSummary
import com.chacare.domain.wonText
import java.time.YearMonth

private val BAR_MAX = 160.dp

/** 교체 기록에 적은 비용을 이번 달·올해·월별·항목별로 모아 보여준다. */
@Composable
fun CostScreen(costs: CostSummary?, parked: Boolean, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 40.dp, vertical = 28.dp).verticalScroll(rememberScrollState())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onBack, enabled = parked) { Text("← 목록", fontSize = 18.sp) }
            Spacer(Modifier.width(20.dp))
            Text("정비 비용", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        Spacer(Modifier.height(8.dp))
        Text("교체 기록에 적은 비용을 모았어요. 부품 화면의 '기록 고치기'에서 비용을 적을 수 있어요.", fontSize = 19.sp, color = TextSecondary)
        Spacer(Modifier.height(24.dp))
        if (costs == null) return@Column
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            StatTile("이번 달", costs.thisMonth, Modifier.weight(1f))
            StatTile("올해", costs.thisYear, Modifier.weight(1f))
            StatTile("최근 12개월", costs.last12Months, Modifier.weight(1f))
        }
        Spacer(Modifier.height(20.dp))
        if (costs.recent.isEmpty()) {
            Text("아직 비용을 적은 기록이 없어요.", fontSize = 20.sp, color = TextSecondary)
            return@Column
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(Modifier.weight(1.4f).card().padding(24.dp)) {
                Text("월별 정비 비용", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(16.dp))
                MonthlyBars(costs.monthly)
            }
            Column(Modifier.weight(1f).card().padding(24.dp)) {
                Text("올해 항목별", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(12.dp))
                if (costs.byItemThisYear.isEmpty()) Text("올해 적은 비용이 없어요", fontSize = 18.sp, color = TextSecondary)
                costs.byItemThisYear.forEach { (name, won) -> AmountRow(name, won) }
            }
        }
        Spacer(Modifier.height(20.dp))
        Column(Modifier.fillMaxWidth().card().padding(24.dp)) {
            Text("최근 기록", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(12.dp))
            costs.recent.take(20).forEach { AmountRow("${it.date}  ${it.name}", it.won) }
        }
    }
}

@Composable
private fun StatTile(label: String, won: Long, modifier: Modifier) {
    Column(modifier.card().padding(horizontal = 24.dp, vertical = 20.dp)) {
        Text(label, fontSize = 18.sp, color = TextSecondary)
        Spacer(Modifier.height(6.dp))
        Text(wonText(won), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

@Composable
private fun AmountRow(label: String, won: Long) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 19.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        Text(wonText(won), fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}

/** 막대 위 금액은 짧게: 8만, 7.5만, 15만(10만 이상은 반올림해 정수로, 좁은 막대 위에서 줄이 넘어가지 않게). */
private fun shortWon(won: Long): String = when {
    won == 0L -> ""
    won < 10_000 -> "%,d".format(won)
    won >= 100_000 -> "${(won + 5_000) / 10_000}만"
    won % 10_000 == 0L -> "${won / 10_000}만"
    else -> "%.1f만".format(won / 10_000.0)
}

@Composable
private fun MonthlyBars(monthly: List<Pair<YearMonth, Long>>) {
    val max = monthly.maxOf { it.second }.coerceAtLeast(1)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
        monthly.forEachIndexed { i, (month, won) ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(shortWon(won), fontSize = 14.sp, color = TextSecondary, textAlign = TextAlign.Center, maxLines = 1, softWrap = false)
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier.fillMaxWidth().height(BAR_MAX * (won.toFloat() / max)).clip(RoundedCornerShape(6.dp))
                        .background(if (i == monthly.lastIndex) Accent else Accent.copy(alpha = 0.35f)),
                )
                Spacer(Modifier.height(6.dp))
                Text("${month.monthValue}월", fontSize = 15.sp, color = if (i == monthly.lastIndex) TextPrimary else TextSecondary)
            }
        }
    }
}

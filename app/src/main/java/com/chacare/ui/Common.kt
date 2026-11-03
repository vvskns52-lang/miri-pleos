package com.chacare.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chacare.data.RecordSource
import com.chacare.data.RecordType
import com.chacare.domain.LifeStatus
import com.chacare.domain.Part

val ChaCareColors = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    background = Color(0xFF101418),
    surface = Color(0xFF1B2127),
    surfaceVariant = Color(0xFF242C34),
    onBackground = Color(0xFFE8EAED),
    onSurface = Color(0xFFE8EAED),
)

val GoodColor = Color(0xFF66BB6A)
val CheckColor = Color(0xFFFFA726)
val ReplaceColor = Color(0xFFEF5350)
val UnknownColor = Color(0xFF5F6B76)

fun LifeStatus.label() = when (this) {
    LifeStatus.GOOD -> "좋음"
    LifeStatus.CHECK -> "점검 권장"
    LifeStatus.REPLACE -> "교체 필요"
}

fun LifeStatus.color() = when (this) {
    LifeStatus.GOOD -> GoodColor
    LifeStatus.CHECK -> CheckColor
    LifeStatus.REPLACE -> ReplaceColor
}

fun RecordType.label() = when (this) {
    RecordType.REPLACED -> "교체"
    RecordType.REFILLED -> "보충"
}

fun RecordSource.label() = when (this) {
    RecordSource.SENSOR_VOICE -> "센서 감지 · 음성 확인"
    RecordSource.SHOP_VOICE -> "정비소 방문 · 음성 답변"
    RecordSource.MANUAL -> "직접 입력"
    RecordSource.ONBOARDING -> "첫 설정"
}

/** 상세 화면의 "왜 이렇게 추정했는지" 한 줄(설계서 5.3). */
fun factorReason(part: Part, factor: Double): String? = when {
    factor >= 1.1 -> when (part) {
        Part.BRAKE_PAD -> "최근 브레이크 사용이 평균보다 많아 교체 시기를 앞당겼어요"
        Part.ENGINE_OIL, Part.TRANSMISSION_OIL -> "짧은 거리 주행이 많아 교체 시기를 앞당겼어요"
        Part.CABIN_FILTER -> "에어컨 사용이 많아 교체 시기를 앞당겼어요"
        Part.WIPER_BLADE -> "와이퍼 사용이 많아 교체 시기를 앞당겼어요"
        else -> null
    }
    factor <= 0.9 -> "운행 습관 덕분에 교체 시기가 늦춰졌어요"
    else -> null
}

@Composable
fun DrivingBanner() {
    Box(
        Modifier.fillMaxWidth().background(ReplaceColor.copy(alpha = 0.85f)).padding(vertical = 12.dp),
    ) {
        Text(
            "주행 중에는 조작할 수 없어요",
            Modifier.padding(horizontal = 24.dp),
            color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold,
        )
    }
}

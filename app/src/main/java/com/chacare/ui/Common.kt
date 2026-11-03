package com.chacare.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chacare.R
import com.chacare.data.RecordSource
import com.chacare.data.RecordType
import com.chacare.domain.LifeStatus
import com.chacare.domain.Part

// Pleos 기본 화면처럼 흰 바탕. 소모품 이미지도 흰 배경이라 카드와 이어져 보인다.
val TextPrimary = Color(0xFF111418)
val TextSecondary = Color(0xFF6B7280)
val Divider = Color(0xFFE9ECEF)
val Track = Color(0xFFEEF0F3)
val Accent = Color(0xFF2F6BFF)

val ChaCareColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    secondaryContainer = Color(0xFFE8EFFF),
    onSecondaryContainer = Accent,
    background = Color.White,
    onBackground = TextPrimary,
    surface = Color.White,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFF5F6F8),
    onSurfaceVariant = TextSecondary,
    outline = Divider,
)

val GoodColor = Color(0xFF1F9D63)
val CheckColor = Color(0xFFE98A00)
val ReplaceColor = Color(0xFFE5484D)
val UnknownColor = Color(0xFFB8BEC6)

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

@DrawableRes
fun Part.imageRes(): Int = when (this) {
    Part.ENGINE_OIL -> R.drawable.part_engine_oil
    Part.BRAKE_FLUID -> R.drawable.part_brake_fluid
    Part.BRAKE_PAD -> R.drawable.part_brake_pad
    Part.TRANSMISSION_OIL -> R.drawable.part_transmission_oil
    Part.CABIN_FILTER -> R.drawable.part_cabin_filter
    Part.WIPER_BLADE -> R.drawable.part_wiper_blade
    Part.WASHER_FLUID -> R.drawable.part_washer_fluid
    Part.TIRE -> R.drawable.part_tire
    Part.SPARK_PLUG -> R.drawable.part_spark_plug
    Part.ENGINE_AIR_FILTER -> R.drawable.part_engine_air_filter
    Part.COOLANT -> R.drawable.part_coolant
    Part.BATTERY -> R.drawable.part_battery
    Part.DRIVE_BELT -> R.drawable.part_drive_belt
    Part.REDUCER_OIL -> R.drawable.part_reducer_oil
}

@Composable
fun PartImage(part: Part, size: Dp, modifier: Modifier = Modifier) {
    Image(
        painterResource(part.imageRes()), contentDescription = part.koreanName,
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit,
    )
}

/** 흰 카드: 얇은 테두리로만 구분한다. */
fun Modifier.card(radius: Dp = 24.dp): Modifier =
    clip(RoundedCornerShape(radius)).background(Color.White).border(1.dp, Divider, RoundedCornerShape(radius))

@Composable
fun StatusPill(status: LifeStatus) {
    Text(
        status.label(),
        Modifier.clip(RoundedCornerShape(50)).background(status.color().copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        color = status.color(), fontSize = 17.sp, fontWeight = FontWeight.Bold,
    )
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
    Box(Modifier.fillMaxWidth().background(ReplaceColor).padding(vertical = 12.dp)) {
        Text(
            "주행 중에는 조작할 수 없어요",
            Modifier.padding(horizontal = 32.dp),
            color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold,
        )
    }
}

package com.chacare.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chacare.engine.QaState

/** 말로 물어본 질문과 답을 화면 아래에 함께 보여준다(소리를 못 들었을 때 대비). */
@Composable
fun QaPanel(state: QaState, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 24.dp)
            .shadow(12.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp))
            .background(Color.White).padding(horizontal = 28.dp, vertical = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("말로 물어보기", fontSize = 16.sp, color = Accent, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = onClose) { Text("닫기", fontSize = 16.sp) }
        }
        when (state) {
            QaState.Listening -> Text("듣고 있어요… 예: \"엔진오일 언제 갈아?\"", fontSize = 22.sp, color = TextPrimary)
            is QaState.Answered -> {
                Text("“${state.question ?: "(잘 들리지 않았어요)"}”", fontSize = 18.sp, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                Text(state.answer, fontSize = 22.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold, lineHeight = 32.sp)
            }
        }
    }
}

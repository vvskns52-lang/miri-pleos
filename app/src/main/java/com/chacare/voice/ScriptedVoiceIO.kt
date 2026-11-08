package com.chacare.voice

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate

/**
 * 마이크가 없는 에뮬레이터 시연용(설계서 5.10): [nextAnswer]에 넣어 둔 말을 다음 한 번의 듣기 결과로 돌려준다.
 * 비어 있으면 실제 음성 인식을 쓴다.
 */
class ScriptedVoiceIO(private val inner: VoiceIO, private val nextAnswer: MutableStateFlow<String?>) : VoiceIO {
    override suspend fun speak(text: String) = inner.speak(text)

    override suspend fun listen(timeoutMillis: Long): String? {
        val scripted = nextAnswer.getAndUpdate { null } ?: return inner.listen(timeoutMillis)
        delay(1_500) // 실제로 듣는 것처럼 잠깐 기다린다
        return scripted
    }

    override fun stop() = inner.stop()
}

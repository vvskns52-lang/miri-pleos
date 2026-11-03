package com.chacare.voice

import kotlinx.coroutines.delay

/** 미리 정한 답을 차례로 돌려준다. 듣는 동안 [onListen]을 실행해 주행 전환 같은 상황을 만든다. */
class FakeVoiceIO(answers: List<String?>, private val onListen: (() -> Unit)? = null) : VoiceIO {
    private val queue = ArrayDeque(answers)
    val spoken = mutableListOf<String>()
    var stopped = false
        private set

    override suspend fun speak(text: String) {
        spoken += text
        delay(100)
    }

    override suspend fun listen(timeoutMillis: Long): String? {
        onListen?.invoke()
        delay(1_000)
        return queue.removeFirstOrNull()
    }

    override fun stop() {
        stopped = true
    }
}

package com.chacare.voice

interface VoiceIO {
    /** 다 말할 때까지 기다린다. */
    suspend fun speak(text: String)

    /** 최종 인식 결과. 오류·무응답·타임아웃이면 null. */
    suspend fun listen(timeoutMillis: Long = 8_000): String?

    /** 말하기·듣기를 즉시 멈춘다(주행 시작 시). */
    fun stop()
}

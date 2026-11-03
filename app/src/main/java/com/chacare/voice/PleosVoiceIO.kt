package com.chacare.voice

import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.listener.ResultListener
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.listener.EventListener
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import ai.pleos.playground.stt.constant.ErrorType as SttError
import ai.pleos.playground.stt.constant.Mode as SttMode
import ai.pleos.playground.stt.listener.OnInitializeListener as SttInit
import ai.pleos.playground.tts.constant.ErrorType as TtsError
import ai.pleos.playground.tts.constant.Mode as TtsMode
import ai.pleos.playground.tts.listener.OnInitializeListener as TtsInit

private const val TAG = "ChaCare"
private const val INIT_TIMEOUT_MS = 5_000L
private const val SPEAK_TIMEOUT_MS = 20_000L

/** 차량 내장(ON_DEVICE) 엔진만 쓰므로 앱 등록·네트워크가 필요 없다. */
class PleosVoiceIO(context: Context) : VoiceIO {
    private val tts = TextToSpeech(context.applicationContext, TtsMode.ON_DEVICE)
    private val stt = SpeechToText(context.applicationContext, SttMode.ON_DEVICE)
    private val ttsReady = CompletableDeferred<Boolean>()
    private val sttReady = CompletableDeferred<Boolean>()

    @Volatile private var speaking: CompletableDeferred<Unit>? = null
    @Volatile private var hearing: CompletableDeferred<String?>? = null

    private val ttsEvents = object : EventListener {
        override fun onStart() = Unit
        override fun onReady() = Unit
        override fun onDone() { speaking?.complete(Unit) }
        override fun onError(errorType: TtsError) {
            Log.w(TAG, "tts error: $errorType")
            speaking?.complete(Unit)
        }
        override fun onStop() { speaking?.complete(Unit) }
        override fun onUpdatedRms(rms: Double) = Unit
    }

    private val sttResults = object : ResultListener {
        override fun onReady() = Unit
        override fun onStartedRecognition() = Unit
        override fun onUpdated(stt: String, completed: Boolean) { if (completed) hearing?.complete(stt) }
        override fun onUpdatedEpdData(on: Long, off: Long) = Unit
        override fun onUpdatedRms(rms: Double) = Unit
        override fun onEndedRecognition() { hearing?.complete(null) }
        override fun onError(errorType: SttError) {
            Log.w(TAG, "stt error: $errorType")
            hearing?.complete(null)
        }
    }

    init {
        tts.initialize(object : TtsInit {
            override fun onSuccess() {
                tts.setLocale(Locale.KOREA)
                tts.addEventListener(ttsEvents)
                ttsReady.complete(true)
            }
            override fun onFailure(errorType: TtsError) {
                Log.w(TAG, "tts init failed: $errorType")
                ttsReady.complete(false)
            }
        })
        stt.initialize(object : SttInit {
            override fun onSuccess() {
                stt.setLocale(Locale.KOREA)
                stt.addListener(sttResults)
                sttReady.complete(true)
            }
            override fun onFailure(errorType: SttError) {
                Log.w(TAG, "stt init failed: $errorType")
                sttReady.complete(false)
            }
        })
    }

    override suspend fun speak(text: String) {
        if (withTimeoutOrNull(INIT_TIMEOUT_MS) { ttsReady.await() } != true) return
        val done = CompletableDeferred<Unit>().also { speaking = it }
        tts.speak(text, true)
        withTimeoutOrNull(SPEAK_TIMEOUT_MS) { done.await() }
    }

    override suspend fun listen(timeoutMillis: Long): String? {
        if (withTimeoutOrNull(INIT_TIMEOUT_MS) { sttReady.await() } != true) return null
        val result = CompletableDeferred<String?>().also { hearing = it }
        stt.request()
        val text = withTimeoutOrNull(timeoutMillis) { result.await() }
        if (text == null) stt.stop()
        return text?.takeIf { it.isNotBlank() }
    }

    override fun stop() {
        runCatching { tts.stop() }
        runCatching { stt.stop() }
        speaking?.complete(Unit)
        hearing?.complete(null)
    }

    fun release() {
        runCatching { tts.removeEventListener(ttsEvents); tts.release() }
        runCatching { stt.removeListener(sttResults); stt.release() }
    }
}

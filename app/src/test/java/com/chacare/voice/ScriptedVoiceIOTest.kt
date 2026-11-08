package com.chacare.voice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScriptedVoiceIOTest {
    @Test fun scriptedAnswerIsUsedOnce() = runTest {
        val next = MutableStateFlow<String?>("응 교체했어")
        val voice = ScriptedVoiceIO(FakeVoiceIO(listOf("마이크로 들은 말")), next)
        assertEquals("응 교체했어", voice.listen())
        assertNull(next.value)
        assertEquals("마이크로 들은 말", voice.listen())
    }
}

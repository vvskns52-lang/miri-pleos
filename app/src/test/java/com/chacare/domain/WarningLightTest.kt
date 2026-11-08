package com.chacare.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WarningLightTest {
    @Test fun brakeLightIgnoredWhileParked() {
        val lit = setOf(WarningLight.BRAKE, WarningLight.TPMS)
        assertEquals(listOf(WarningLight.TPMS), activeWarnings(lit, parked = true))
        assertEquals(listOf(WarningLight.BRAKE, WarningLight.TPMS), activeWarnings(lit, parked = false))
    }

    @Test fun mostUrgentFirst() = assertEquals(
        listOf(WarningLight.OIL_PRESSURE, WarningLight.CHECK_ENGINE, WarningLight.TPMS),
        activeWarnings(setOf(WarningLight.TPMS, WarningLight.CHECK_ENGINE, WarningLight.OIL_PRESSURE), parked = false),
    )

    @Test fun newlyLitAreOnlyTheOnesNotAnnounced() = assertEquals(
        listOf(WarningLight.CHECK_ENGINE),
        newlyLit(listOf(WarningLight.TPMS, WarningLight.CHECK_ENGINE), announced = setOf(WarningLight.TPMS)),
    )

    @Test fun announcement() = assertEquals(
        "엔진오일 압력 경고등이 켜졌어요. 안전한 곳에 차를 세우고 시동을 끄세요. 엔진오일 양을 확인하고, 양이 정상인데도 켜지면 운행하지 말고 정비소에 연락하세요.",
        announceWarning(WarningLight.OIL_PRESSURE),
    )

    @Test fun findsLightByNameIgnoringSpaces() {
        assertEquals(listOf(WarningLight.CHECK_ENGINE), findWarningLights("엔진경고등 뭐야"))
        assertEquals(listOf(WarningLight.OIL_PRESSURE), findWarningLights("엔진오일 경고등 켜졌어"))
        assertEquals(listOf(WarningLight.ABS), findWarningLights("ABS 불 들어왔어"))
    }

    @Test fun everyLightHasGuidance() = WarningLight.entries.forEach {
        assertTrue(it.name, it.cause.isNotBlank() && it.action.isNotBlank() && it.detail.isNotBlank() && it.aliases.isNotEmpty())
    }
}

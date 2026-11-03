package com.chacare.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SensorChangeDetectorTest {
    @Test fun lowToNormalRecovers() {
        val d = SensorChangeDetector()
        assertNull(d.onSignal(SensorKind.BRAKE_FLUID_LEVEL, SignalState.LOW, 1))
        val r = d.onSignal(SensorKind.BRAKE_FLUID_LEVEL, SignalState.NORMAL, 2)!!
        assertEquals(Part.BRAKE_FLUID, r.part)
        assertEquals(Part.BRAKE_FLUID, r.toPending()!!.part)
    }

    @Test fun firstNormalIgnored() = assertNull(SensorChangeDetector().onSignal(SensorKind.OIL_LEVEL, SignalState.NORMAL, 1))

    @Test fun unknownDoesNotReset() {
        val d = SensorChangeDetector()
        d.onSignal(SensorKind.OIL_LEVEL, SignalState.LOW, 1)
        d.onSignal(SensorKind.OIL_LEVEL, SignalState.UNKNOWN, 2)
        assertNotNull(d.onSignal(SensorKind.OIL_LEVEL, SignalState.NORMAL, 3))
    }

    @Test fun restoredStateSurvivesRestart() {
        val d = SensorChangeDetector(mapOf(SensorKind.OIL_LEVEL to SignalState.LOW))
        assertNotNull(d.onSignal(SensorKind.OIL_LEVEL, SignalState.NORMAL, 1))
    }

    @Test fun tireAndWasherNotAsked() {
        val d = SensorChangeDetector()
        d.onSignal(SensorKind.TIRE_PRESSURE_WARNING, SignalState.LOW, 1)
        assertNull(d.onSignal(SensorKind.TIRE_PRESSURE_WARNING, SignalState.NORMAL, 2)!!.toPending())
        d.onSignal(SensorKind.WASHER_WARNING, SignalState.LOW, 3)
        assertTrue(d.isLow(SensorKind.WASHER_WARNING))
        assertNull(d.onSignal(SensorKind.WASHER_WARNING, SignalState.NORMAL, 4)!!.toPending())
    }
}

package com.chacare.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PartSpecsTest {
    @Test fun engineOilSpec() {
        val s = DEFAULT_SPECS.single { it.part == Part.ENGINE_OIL }
        assertEquals(10_000, s.intervalKm)
        assertEquals(12, s.intervalMonths)
        assertEquals(SensorKind.OIL_LEVEL, s.sensor)
    }

    @Test fun tableMatchesSpec() {
        val m = DEFAULT_SPECS.associateBy { it.part }
        assertEquals(40_000 to 24, m[Part.BRAKE_FLUID]!!.let { it.intervalKm to it.intervalMonths })
        assertEquals(40_000, m[Part.BRAKE_PAD]!!.intervalKm)
        assertNull(m[Part.BRAKE_PAD]!!.sensor)
        assertEquals(100_000, m[Part.TRANSMISSION_OIL]!!.intervalKm)
        assertEquals(15_000 to 12, m[Part.CABIN_FILTER]!!.let { it.intervalKm to it.intervalMonths })
        assertEquals(null to 12, m[Part.WIPER_BLADE]!!.let { it.intervalKm to it.intervalMonths })
        assertEquals(null to null, m[Part.WASHER_FLUID]!!.let { it.intervalKm to it.intervalMonths })
        assertEquals(50_000, m[Part.TIRE]!!.intervalKm)
        assertEquals(SensorKind.TIRE_PRESSURE_WARNING, m[Part.TIRE]!!.sensor)
    }

    @Test fun evHidesOils() {
        val ev = specsFor(Powertrain.EV).map { it.part }
        assertFalse(Part.ENGINE_OIL in ev)
        assertFalse(Part.TRANSMISSION_OIL in ev)
        assertTrue(Part.BRAKE_PAD in ev)
        assertEquals(8, specsFor(Powertrain.ICE).size)
    }
}

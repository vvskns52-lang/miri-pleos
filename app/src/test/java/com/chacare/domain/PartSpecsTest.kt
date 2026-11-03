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

    @Test fun addedParts() {
        val m = DEFAULT_SPECS.associateBy { it.part }
        assertEquals(100_000, m[Part.SPARK_PLUG]!!.intervalKm)
        assertEquals(40_000, m[Part.ENGINE_AIR_FILTER]!!.intervalKm)
        assertEquals(40_000 to 24, m[Part.COOLANT]!!.let { it.intervalKm to it.intervalMonths })
        assertEquals(null to 48, m[Part.BATTERY]!!.let { it.intervalKm to it.intervalMonths })
        assertEquals(80_000, m[Part.DRIVE_BELT]!!.intervalKm)
    }

    @Test fun evHidesEngineParts() {
        val ev = specsFor(Powertrain.EV).map { it.part }
        for (p in listOf(Part.ENGINE_OIL, Part.TRANSMISSION_OIL, Part.SPARK_PLUG, Part.ENGINE_AIR_FILTER, Part.DRIVE_BELT)) {
            assertFalse(p in ev)
        }
        assertTrue(Part.BRAKE_PAD in ev)
        assertTrue(Part.COOLANT in ev)
        assertTrue(Part.BATTERY in ev)
        assertEquals(13, specsFor(Powertrain.ICE).size)
        assertEquals(8, ev.size)
    }
}

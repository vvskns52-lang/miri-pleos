package com.chacare.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PartSpecsTest {
    private fun ice() = specsFor(Powertrain.ICE).associateBy { it.part }
    private fun ev() = specsFor(Powertrain.EV).associateBy { it.part }
    private fun PartSpec.pair() = intervalKm to intervalMonths

    /** 현대자동차그룹 공식 안내(엔진오일·점화플러그·브레이크액·냉각수·변속기·구동벨트). */
    @Test fun hyundaiGroupOfficial() {
        val m = ice()
        assertEquals(15_000 to 12, m[Part.ENGINE_OIL]!!.pair())
        assertEquals(15_000 to 12, m[Part.ENGINE_AIR_FILTER]!!.pair()) // 엔진오일 교체 때 함께
        assertEquals(160_000 to null, m[Part.SPARK_PLUG]!!.pair())
        assertEquals(50_000 to null, m[Part.BRAKE_FLUID]!!.pair())
        assertEquals(40_000 to 24, m[Part.COOLANT]!!.pair())
        assertEquals(100_000 to null, m[Part.TRANSMISSION_OIL]!!.pair())
        assertEquals(90_000 to 72, m[Part.DRIVE_BELT]!!.pair())
    }

    /** 기아 공식 정비 안내(에어컨 필터·와이퍼·브레이크 패드·배터리, EV 감속기·냉각수). */
    @Test fun kiaOfficial() {
        val m = ice()
        assertEquals(20_000 to 12, m[Part.CABIN_FILTER]!!.pair())
        assertEquals(20_000 to 12, m[Part.WIPER_BLADE]!!.pair())
        assertEquals(50_000 to null, m[Part.BRAKE_PAD]!!.pair())
        assertEquals(80_000 to null, m[Part.BATTERY]!!.pair())
        assertEquals(100_000 to null, ev()[Part.REDUCER_OIL]!!.pair())
        assertEquals(60_000 to null, ev()[Part.COOLANT]!!.pair())
    }

    @Test fun powertrainParts() {
        assertEquals(13, specsFor(Powertrain.ICE).size)
        assertEquals(13, specsFor(Powertrain.HYBRID).size)
        val ev = ev().keys
        assertEquals(9, ev.size)
        for (p in listOf(Part.ENGINE_OIL, Part.TRANSMISSION_OIL, Part.SPARK_PLUG, Part.ENGINE_AIR_FILTER, Part.DRIVE_BELT)) {
            assertFalse(p in ev)
        }
        assertFalse(Part.REDUCER_OIL in ice().keys)
    }

    @Test fun sensorsAndWasher() {
        val m = ice()
        assertEquals(SensorKind.OIL_LEVEL, m[Part.ENGINE_OIL]!!.sensor)
        assertEquals(SensorKind.TIRE_PRESSURE_WARNING, m[Part.TIRE]!!.sensor)
        assertNull(m[Part.BRAKE_PAD]!!.sensor)
        assertEquals(null to null, m[Part.WASHER_FLUID]!!.pair())
    }

    @Test fun modelOverrideWins() {
        val model = VehicleModel(
            "test", Brand.HYUNDAI, "테스트카", setOf(Powertrain.ICE),
            overrides = mapOf(Powertrain.ICE to mapOf(Part.ENGINE_OIL to Interval(10_000, 6))),
        )
        val m = specsFor(Powertrain.ICE, model).associateBy { it.part }
        assertEquals(10_000 to 6, m[Part.ENGINE_OIL]!!.pair())
        assertEquals(160_000 to null, m[Part.SPARK_PLUG]!!.pair())
    }

    @Test fun modelCatalog() {
        assertNotNull(findModel("ioniq5"))
        assertEquals(setOf(Powertrain.EV), findModel("ioniq5")!!.powertrains)
        assertTrue(Powertrain.HYBRID in findModel("avante")!!.powertrains)
        assertTrue(MODELS.map { it.id }.toSet().size == MODELS.size) // id 중복 없음
        assertTrue(MODELS.all { it.powertrains.isNotEmpty() })
        assertNull(findModel("nope"))
        assertNotNull(intervalSource(Part.SPARK_PLUG))
    }
}

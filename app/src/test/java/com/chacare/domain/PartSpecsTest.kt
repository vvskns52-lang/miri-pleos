package com.chacare.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 값은 2026-11 현대·기아·제네시스 공식 한국어 취급설명서 「통상 조건」 점검표 기준. */
class PartSpecsTest {
    private fun specs(model: String, p: Powertrain) = specsFor(p, findModel(model)).associateBy { it.part }
    private fun PartSpec.v() = Triple(intervalKm, intervalMonths, kind)

    @Test fun avanteGasoline_CN8_2027() {
        val m = specs("avante", Powertrain.ICE)
        assertEquals(Triple(15_000, 12, IntervalKind.REPLACE), m[Part.ENGINE_OIL]!!.v())
        assertEquals(Triple(150_000, null, IntervalKind.REPLACE), m[Part.SPARK_PLUG]!!.v())
        assertEquals(Triple(40_000, null, IntervalKind.REPLACE), m[Part.ENGINE_AIR_FILTER]!!.v())
        assertEquals(Triple(null, 12, IntervalKind.REPLACE), m[Part.CABIN_FILTER]!!.v())
        assertEquals(Triple(90_000, 72, IntervalKind.INSPECT), m[Part.DRIVE_BELT]!!.v())
        assertEquals(IntervalKind.NONE, m[Part.TRANSMISSION_OIL]!!.kind)
    }

    @Test fun avanteHybrid_CN8KH_2027() {
        val m = specs("avante", Powertrain.HYBRID)
        assertEquals(Triple(75_000, null, IntervalKind.REPLACE), m[Part.SPARK_PLUG]!!.v())
        assertEquals(Triple(105_000, 48, IntervalKind.REPLACE), m[Part.DRIVE_BELT]!!.v())
    }

    @Test fun modelDifferences() {
        assertEquals(10_000, specs("tucson", Powertrain.ICE)[Part.ENGINE_OIL]!!.intervalKm)
        assertEquals(165_000, specs("sonata", Powertrain.ICE)[Part.SPARK_PLUG]!!.intervalKm)
        assertEquals(70_000, specs("santafe", Powertrain.ICE)[Part.SPARK_PLUG]!!.intervalKm)
        assertEquals(Triple(null, 12, IntervalKind.INSPECT), specs("tucson", Powertrain.ICE)[Part.CABIN_FILTER]!!.v())
        assertEquals(Triple(12_000, 12, IntervalKind.INSPECT), specs("tucson", Powertrain.ICE)[Part.BATTERY]!!.v())
        assertEquals(Triple(15_000, 12, IntervalKind.REPLACE), specs("k5", Powertrain.ICE)[Part.CABIN_FILTER]!!.v())
        assertEquals(Triple(100_000, 48, IntervalKind.REPLACE), specs("sorento", Powertrain.HYBRID)[Part.DRIVE_BELT]!!.v())
        assertEquals(80_000, specs("g70", Powertrain.ICE)[Part.SPARK_PLUG]!!.intervalKm)
        assertEquals(Triple(15_000, 12, IntervalKind.REPLACE), specs("ioniq9", Powertrain.EV)[Part.CABIN_FILTER]!!.v())
    }

    @Test fun commonToAllManuals() {
        for (model in MODELS) for (p in model.powertrains) {
            val m = specsFor(p, model).associateBy { it.part }
            assertEquals("${model.id} $p 브레이크액", 50_000, m[Part.BRAKE_FLUID]!!.intervalKm)
            assertEquals("${model.id} $p 패드", Triple(10_000, null, IntervalKind.INSPECT), m[Part.BRAKE_PAD]!!.v())
            assertEquals("${model.id} $p 냉각수", 40_000 to 24, m[Part.COOLANT]!!.let { it.intervalKm to it.intervalMonths })
            if (p != Powertrain.EV) {
                assertEquals("${model.id} $p 에어클리너", 40_000, m[Part.ENGINE_AIR_FILTER]!!.intervalKm)
                assertNotNull("${model.id} $p 엔진오일", m[Part.ENGINE_OIL]!!.intervalKm)
                assertNotNull("${model.id} $p 점화플러그", m[Part.SPARK_PLUG]!!.intervalKm)
            }
        }
    }

    @Test fun electric() {
        val m = specs("ioniq5", Powertrain.EV)
        assertEquals(Triple(60_000, null, IntervalKind.INSPECT), m[Part.REDUCER_OIL]!!.v())
        assertEquals(Triple(12_000, 12, IntervalKind.INSPECT), m[Part.BATTERY]!!.v())
        assertEquals(9, m.size)
        for (p in listOf(Part.ENGINE_OIL, Part.TRANSMISSION_OIL, Part.SPARK_PLUG, Part.ENGINE_AIR_FILTER, Part.DRIVE_BELT)) {
            assertFalse(p in m)
        }
    }

    @Test fun genericWhenModelUnknown() {
        assertEquals(13, specsFor(Powertrain.ICE).size)
        assertEquals(13, specsFor(Powertrain.HYBRID).size)
        assertEquals(9, specsFor(Powertrain.EV).size)
        assertFalse(Part.REDUCER_OIL in specsFor(Powertrain.ICE).map { it.part })
    }

    @Test fun sensorsAndWasher() {
        val m = specsFor(Powertrain.ICE).associateBy { it.part }
        assertEquals(SensorKind.OIL_LEVEL, m[Part.ENGINE_OIL]!!.sensor)
        assertEquals(SensorKind.TIRE_PRESSURE_WARNING, m[Part.TIRE]!!.sensor)
        assertNull(m[Part.BRAKE_PAD]!!.sensor)
        assertEquals(IntervalKind.NONE, m[Part.WASHER_FLUID]!!.kind)
    }

    @Test fun catalog() {
        assertEquals(setOf(Powertrain.EV), findModel("ioniq5")!!.powertrains)
        assertTrue(Powertrain.HYBRID in findModel("avante")!!.powertrains)
        assertEquals(MODELS.size, MODELS.map { it.id }.toSet().size)
        assertTrue(MODELS.all { it.powertrains.isNotEmpty() })
        assertNull(findModel("nope"))
        assertEquals("CN8 2027", findModel("avante")!!.manuals.getValue(Powertrain.ICE).label)
    }

    @Test fun sourceNamesTheManual() {
        val avante = findModel("avante")
        assertTrue(intervalSource(Part.SPARK_PLUG, avante, Powertrain.ICE).contains("CN8 2027"))
        assertTrue(intervalSource(Part.WIPER_BLADE, avante, Powertrain.ICE).contains("취급설명서에 주기 없음"))
        assertTrue(intervalSource(Part.ENGINE_OIL, null, Powertrain.ICE).contains("목록에 없는 차"))
    }
}

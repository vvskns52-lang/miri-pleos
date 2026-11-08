package com.chacare.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SevereTest {
    private fun u(km: Double = 1_000.0, min: Double = 1_200.0, brakes: Int = 500, short: Int = 2, trips: Int = 20) =
        UsageStats(drivenKm = km, drivingMinutes = min, brakePresses = brakes, shortTrips = short, totalTrips = trips)

    @Test fun normalDrivingIsNotSevere() = assertTrue(detectSevere(u()).isEmpty()) // 50km/h, 0.5회/km, 단거리 10%

    @Test fun shortTrips() = assertEquals(setOf(SevereReason.SHORT_TRIPS), detectSevere(u(short = 12)))

    @Test fun congestion() = assertEquals(setOf(SevereReason.CONGESTION), detectSevere(u(min = 3_600.0))) // 평균 16.7km/h

    @Test fun stopAndGo() = assertEquals(setOf(SevereReason.STOP_AND_GO), detectSevere(u(brakes = 2_500)))

    @Test fun tooLittleDataIsNotJudged() {
        assertTrue(detectSevere(u(km = 150.0, short = 20, trips = 20)).isEmpty())
        assertTrue(detectSevere(u(trips = 5, short = 5)).isEmpty())
    }

    @Test fun avanteGasolineSevere() {
        val m = specsFor(Powertrain.ICE, findModel("avante"), setOf(SevereReason.SHORT_TRIPS)).associateBy { it.part }
        assertEquals(7_500 to 6, m[Part.ENGINE_OIL]!!.let { it.intervalKm to it.intervalMonths })
        assertEquals(setOf(SevereReason.SHORT_TRIPS), m[Part.ENGINE_OIL]!!.severeBy)
        assertEquals(100_000, m[Part.TRANSMISSION_OIL]!!.intervalKm)
        assertEquals(IntervalKind.REPLACE, m[Part.TRANSMISSION_OIL]!!.kind)
    }

    /** 아반떼 IVT 오일의 가혹 조건에는 교통 체증이 없다(1,4,5,6,7,8 → 짧은 거리·정지 출발). */
    @Test fun reasonMustMatchThePart() {
        val m = specsFor(Powertrain.ICE, findModel("avante"), setOf(SevereReason.CONGESTION)).associateBy { it.part }
        assertEquals(7_500, m[Part.ENGINE_OIL]!!.intervalKm)
        assertEquals(IntervalKind.NONE, m[Part.TRANSMISSION_OIL]!!.kind)
        assertTrue(m[Part.TRANSMISSION_OIL]!!.severeBy.isEmpty())
    }

    @Test fun hybridBeltOnlyForTrafficOrStopAndGo() {
        val short = specsFor(Powertrain.HYBRID, findModel("k8"), setOf(SevereReason.SHORT_TRIPS)).associateBy { it.part }
        assertEquals(100_000, short[Part.DRIVE_BELT]!!.intervalKm)
        val stop = specsFor(Powertrain.HYBRID, findModel("k8"), setOf(SevereReason.STOP_AND_GO)).associateBy { it.part }
        assertEquals(50_000 to 24, stop[Part.DRIVE_BELT]!!.let { it.intervalKm to it.intervalMonths })
        assertEquals(5_000 to 6, stop[Part.ENGINE_OIL]!!.let { it.intervalKm to it.intervalMonths })
    }

    /** 제네시스 엔진오일 가혹 조건(택시·한랭지·견인·비추천 오일)은 차가 알 수 없어 그대로. */
    @Test fun genesisOilUnchanged() {
        val all = SevereReason.entries.toSet()
        val m = specsFor(Powertrain.ICE, findModel("g80"), all).associateBy { it.part }
        assertEquals(10_000, m[Part.ENGINE_OIL]!!.intervalKm)
        assertFalse(m[Part.ENGINE_OIL]!!.severeBy.isNotEmpty())
        assertEquals(100_000, m[Part.TRANSMISSION_OIL]!!.intervalKm)
    }

    @Test fun genericAndEv() {
        val g = specsFor(Powertrain.ICE, null, setOf(SevereReason.SHORT_TRIPS)).associateBy { it.part }
        assertEquals(5_000 to 6, g[Part.ENGINE_OIL]!!.let { it.intervalKm to it.intervalMonths })
        val kona = specsFor(Powertrain.EV, findModel("kona"), setOf(SevereReason.SHORT_TRIPS)).associateBy { it.part }
        assertEquals(Triple(120_000, null, IntervalKind.REPLACE), kona[Part.REDUCER_OIL]!!.let { Triple(it.intervalKm, it.intervalMonths, it.kind) })
        val ev6 = specsFor(Powertrain.EV, findModel("ev6"), SevereReason.entries.toSet()).associateBy { it.part }
        assertEquals(IntervalKind.INSPECT, ev6[Part.REDUCER_OIL]!!.kind)
    }

    @Test fun noReasonsMeansNormal() =
        assertEquals(specsFor(Powertrain.ICE, findModel("tucson")), specsFor(Powertrain.ICE, findModel("tucson"), emptySet()))
}

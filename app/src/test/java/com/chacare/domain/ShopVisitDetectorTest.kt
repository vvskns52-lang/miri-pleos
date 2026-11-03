package com.chacare.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShopVisitDetectorTest {
    private val shop = Shop("s1", "테스트정비", 37.5, 127.0)
    private val nearLat = 37.5003 // 약 33m
    private val nearLon = 127.0
    private val min = 60_000L

    @Test fun distance() = assertEquals(33.4, distanceMeters(37.5, 127.0, nearLat, nearLon), 1.0)

    @Test fun dwell30minEmitsOnce() {
        val d = ShopVisitDetector(listOf(shop))
        assertNull(d.onLocation(nearLat, nearLon, 0))
        assertNull(d.onLocation(nearLat, nearLon, 29 * min))
        assertEquals(ShopVisit("s1", 0), d.onLocation(nearLat, nearLon, 30 * min))
        assertNull(d.onLocation(nearLat, nearLon, 40 * min))
    }

    @Test fun leavingResets() {
        val d = ShopVisitDetector(listOf(shop))
        d.onLocation(nearLat, nearLon, 0)
        d.onLocation(37.51, 127.0, 10 * min)
        assertNull(d.onLocation(nearLat, nearLon, 31 * min))
    }

    @Test fun survivesIgnitionOff() {
        val before = ShopVisitDetector(listOf(shop)).apply { onLocation(nearLat, nearLon, 0) }.state()
        val after = ShopVisitDetector(listOf(shop), initial = before)
        assertEquals(ShopVisit("s1", 0), after.onLocation(nearLat, nearLon, 60 * min))
    }

    @Test fun outside100m() {
        val d = ShopVisitDetector(listOf(shop))
        d.onLocation(37.5012, 127.0, 0)
        assertNull(d.onLocation(37.5012, 127.0, 31 * min))
    }
}

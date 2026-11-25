package com.zahid.aroundme.core.geo

import com.zahid.aroundme.domain.model.DistanceUnit
import com.zahid.aroundme.domain.model.GeoPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeoUtilsTest {
    @Test
    fun haversineMeters_returnsZeroForIdenticalPoints() {
        val point = GeoPoint(24.6949, 46.6852)
        val distance = GeoUtils.haversineMeters(point, point)
        assertEquals(0.0, distance, absoluteTolerance = 0.01)
    }

    @Test
    fun haversineMeters_calculatesRealisticShortDistance() {
        val from = GeoPoint(24.6949, 46.6852)
        val to = GeoPoint(24.6968, 46.6879)
        val distance = GeoUtils.haversineMeters(from, to)
        assertTrue(distance in 250.0..450.0, "Expected ~340m, got $distance")
    }

    @Test
    fun formatDistance_formatsMetricAndArabicUnits() {
        assertEquals("350 m", GeoUtils.formatDistance(350.0, DistanceUnit.METRIC, isArabic = false))
        assertEquals("1.2 km", GeoUtils.formatDistance(1240.0, DistanceUnit.METRIC, isArabic = false))
        assertEquals("1.2 كم", GeoUtils.formatDistance(1240.0, DistanceUnit.METRIC, isArabic = true))
    }
}

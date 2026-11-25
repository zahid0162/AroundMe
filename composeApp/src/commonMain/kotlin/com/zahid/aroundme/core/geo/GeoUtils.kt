package com.zahid.aroundme.core.geo

import com.zahid.aroundme.domain.model.DistanceUnit
import com.zahid.aroundme.domain.model.GeoPoint
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object GeoUtils {
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    /**
     * Straight-line Haversine distance in meters between two WGS84 coordinates.
     * Note: Never present this as driving distance.
     */
    fun haversineMeters(from: GeoPoint, to: GeoPoint): Double {
        val dLat = (to.latitude - from.latitude) * PI / 180.0
        val dLon = (to.longitude - from.longitude) * PI / 180.0
        val lat1 = from.latitude * PI / 180.0
        val lat2 = to.latitude * PI / 180.0

        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    fun formatDistance(
        meters: Double,
        unit: DistanceUnit = DistanceUnit.METRIC,
        isArabic: Boolean = false
    ): String {
        return when (unit) {
            DistanceUnit.METRIC -> {
                if (meters < 1000.0) {
                    val rounded = (meters / 10.0).roundToInt() * 10
                    if (isArabic) "$rounded م" else "$rounded m"
                } else {
                    val kmTenths = ((meters / 1000.0) * 10.0).roundToInt()
                    val whole = kmTenths / 10
                    val frac = kmTenths % 10
                    val value = "$whole.$frac"
                    if (isArabic) "$value كم" else "$value km"
                }
            }
            DistanceUnit.IMPERIAL -> {
                val feet = meters * 3.28084
                if (feet < 1000.0) {
                    val rounded = (feet / 50.0).roundToInt() * 50
                    if (isArabic) "$rounded قدم" else "$rounded ft"
                } else {
                    val milesTenths = ((meters / 1609.344) * 10.0).roundToInt()
                    val whole = milesTenths / 10
                    val frac = milesTenths % 10
                    val value = "$whole.$frac"
                    if (isArabic) "$value ميل" else "$value mi"
                }
            }
        }
    }

    fun formatRating(rating: Double): String {
        val tenths = (rating * 10.0).roundToInt()
        return "${tenths / 10}.${tenths % 10}"
    }
}

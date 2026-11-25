package com.zahid.aroundme.domain.model

data class GeoPoint(
    val latitude: Double,
    val longitude: Double
)

enum class PriceLevel(val symbol: String) {
    INEXPENSIVE("$"),
    MODERATE("$$"),
    EXPENSIVE("$$$"),
    VERY_EXPENSIVE("$$$$")
}

/**
 * Ephemeral photo reference returned by Places API (New) during an active session.
 * Never persisted to Room / local disk per Google Maps Platform ToS Section 3.2.3.
 */
data class PlacePhoto(
    val resourceName: String,
    val resolvedUri: String? = null,
    val widthPx: Int? = null,
    val heightPx: Int? = null,
    val attributionName: String? = null,
    val attributionUri: String? = null
)

data class PlaceReview(
    val authorName: String,
    val rating: Double? = null,
    val relativePublishTime: String? = null,
    val text: String? = null
)

data class Place(
    val id: String,
    val name: String,
    val primaryCategory: PlaceCategory,
    val primaryCategoryLabel: String,
    val address: String,
    val location: GeoPoint,
    val rating: Double? = null,
    val reviewCount: Int? = null,
    val isOpenNow: Boolean? = null,
    val priceLevel: PriceLevel? = null,
    val phoneNumber: String? = null,
    val websiteUrl: String? = null,
    val googleMapsUri: String? = null,
    val directionsUri: String? = null,
    val openingHoursSummary: String? = null,
    val weekdayHours: List<String> = emptyList(),
    val attributes: List<String> = emptyList(),
    val editorialSummary: String? = null,
    val photos: List<PlacePhoto> = emptyList(),
    val reviews: List<PlaceReview> = emptyList(),
    val businessStatus: String? = null
)

data class LocationContext(
    val label: String,
    val coordinates: GeoPoint,
    val isCurrentLocation: Boolean
)

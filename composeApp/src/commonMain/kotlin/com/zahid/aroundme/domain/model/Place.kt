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
    val openingHoursSummary: String? = null,
    val attributes: List<String> = emptyList(),
    val editorialSummary: String? = null
)

data class LocationContext(
    val label: String,
    val coordinates: GeoPoint,
    val isCurrentLocation: Boolean
)

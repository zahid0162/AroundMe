package com.zahid.aroundme.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class LatLngDto(
    val latitude: Double,
    val longitude: Double
)

@Serializable
data class CircleDto(
    val center: LatLngDto,
    val radius: Double
)

@Serializable
data class LocationConstraintDto(
    val circle: CircleDto
)

@Serializable
data class SearchNearbyRequestDto(
    val includedTypes: List<String>,
    val maxResultCount: Int = 20,
    val rankPreference: String = "POPULARITY",
    val languageCode: String = "en",
    val locationRestriction: LocationConstraintDto
)

@Serializable
data class SearchTextRequestDto(
    val textQuery: String,
    val includedType: String? = null,
    val openNow: Boolean? = null,
    val minRating: Double? = null,
    val pageSize: Int = 20,
    val languageCode: String = "en",
    val locationBias: LocationConstraintDto? = null
)

@Serializable
data class AutocompleteRequestDto(
    val input: String,
    val sessionToken: String? = null,
    val languageCode: String = "en",
    val origin: LatLngDto? = null,
    val locationBias: LocationConstraintDto? = null,
    val includeQueryPredictions: Boolean = true
)

@Serializable
data class LocalizedTextDto(
    val text: String = "",
    val languageCode: String? = null
)

@Serializable
data class OpeningHoursDto(
    val openNow: Boolean? = null,
    val weekdayDescriptions: List<String> = emptyList()
)

@Serializable
data class AuthorAttributionDto(
    val displayName: String? = null,
    val uri: String? = null
)

@Serializable
data class PhotoDto(
    val name: String = "",
    val widthPx: Int? = null,
    val heightPx: Int? = null,
    val authorAttributions: List<AuthorAttributionDto> = emptyList()
)

@Serializable
data class ReviewDto(
    val relativePublishTimeDescription: String? = null,
    val rating: Double? = null,
    val text: LocalizedTextDto? = null,
    val originalText: LocalizedTextDto? = null,
    val authorAttribution: AuthorAttributionDto? = null
)

@Serializable
data class GoogleMapsLinksDto(
    val directionsUri: String? = null,
    val placeUri: String? = null,
    val reviewsUri: String? = null,
    val photosUri: String? = null
)

@Serializable
data class AccessibilityOptionsDto(
    val wheelchairAccessibleEntrance: Boolean? = null,
    val wheelchairAccessibleParking: Boolean? = null,
    val wheelchairAccessibleRestroom: Boolean? = null,
    val wheelchairAccessibleSeating: Boolean? = null
)

@Serializable
data class ParkingOptionsDto(
    val freeParkingLot: Boolean? = null,
    val paidParkingLot: Boolean? = null,
    val freeStreetParking: Boolean? = null,
    val valetParking: Boolean? = null,
    val freeGarageParking: Boolean? = null
)

@Serializable
data class PlaceDto(
    val id: String = "",
    val name: String? = null,
    val displayName: LocalizedTextDto? = null,
    val primaryType: String? = null,
    val primaryTypeDisplayName: LocalizedTextDto? = null,
    val types: List<String> = emptyList(),
    val formattedAddress: String? = null,
    val shortFormattedAddress: String? = null,
    val location: LatLngDto? = null,
    val rating: Double? = null,
    val userRatingCount: Int? = null,
    val priceLevel: String? = null,
    val businessStatus: String? = null,
    val currentOpeningHours: OpeningHoursDto? = null,
    val regularOpeningHours: OpeningHoursDto? = null,
    val nationalPhoneNumber: String? = null,
    val internationalPhoneNumber: String? = null,
    val websiteUri: String? = null,
    val googleMapsUri: String? = null,
    val googleMapsLinks: GoogleMapsLinksDto? = null,
    val editorialSummary: LocalizedTextDto? = null,
    val photos: List<PhotoDto> = emptyList(),
    val reviews: List<ReviewDto> = emptyList(),
    val outdoorSeating: Boolean? = null,
    val delivery: Boolean? = null,
    val takeout: Boolean? = null,
    val dineIn: Boolean? = null,
    val reservable: Boolean? = null,
    val goodForChildren: Boolean? = null,
    val accessibilityOptions: AccessibilityOptionsDto? = null,
    val parkingOptions: ParkingOptionsDto? = null
)

@Serializable
data class PlacesSearchResponseDto(
    val places: List<PlaceDto> = emptyList(),
    val nextPageToken: String? = null
)

@Serializable
data class AutocompleteTextDto(
    val text: String = ""
)

@Serializable
data class StructuredFormatDto(
    val mainText: AutocompleteTextDto? = null,
    val secondaryText: AutocompleteTextDto? = null
)

@Serializable
data class PlacePredictionDto(
    val placeId: String = "",
    val text: AutocompleteTextDto? = null,
    val structuredFormat: StructuredFormatDto? = null,
    val types: List<String> = emptyList(),
    val distanceMeters: Int? = null
)

@Serializable
data class QueryPredictionDto(
    val text: AutocompleteTextDto? = null
)

@Serializable
data class AutocompleteSuggestionItemDto(
    val placePrediction: PlacePredictionDto? = null,
    val queryPrediction: QueryPredictionDto? = null
)

@Serializable
data class AutocompleteResponseDto(
    val suggestions: List<AutocompleteSuggestionItemDto> = emptyList()
)

@Serializable
data class PhotoMediaResponseDto(
    val name: String = "",
    val photoUri: String? = null
)

package com.zahid.aroundme.core.platform

import com.zahid.aroundme.domain.model.GeoPoint

data class PlatformPlacesConfig(
    val apiKey: String,
    val proxyBaseUrl: String = "",
    val platformHeaders: Map<String, String> = emptyMap()
) {
    val hasConfiguredBackend: Boolean
        get() = apiKey.isNotBlank() || proxyBaseUrl.isNotBlank()

    val resolvedBaseUrl: String
        get() = proxyBaseUrl.trim().trimEnd('/').ifEmpty { "https://places.googleapis.com/v1" }
}

interface PlatformActions {
    fun openDirections(
        placeName: String,
        location: GeoPoint,
        placeId: String,
        directionsUri: String?
    )

    fun dialPhone(phoneNumber: String)
    fun openWebsite(url: String)
    fun sharePlace(placeName: String, address: String, googleMapsUri: String?)
}

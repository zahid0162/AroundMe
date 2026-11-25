package com.zahid.aroundme.data.remote

import com.zahid.aroundme.core.platform.PlatformPlacesConfig
import com.zahid.aroundme.domain.model.GeoPoint
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess

interface PlacesRemoteDataSource {
    val isConfigured: Boolean
    suspend fun searchNearby(
        center: GeoPoint,
        radiusMeters: Double,
        includedTypes: List<String>,
        rankByDistance: Boolean,
        languageCode: String
    ): PlacesSearchResponseDto

    suspend fun searchText(
        query: String,
        center: GeoPoint,
        radiusMeters: Double,
        includedType: String?,
        openNow: Boolean?,
        minRating: Double?,
        languageCode: String
    ): PlacesSearchResponseDto

    suspend fun autocomplete(
        input: String,
        center: GeoPoint,
        sessionToken: String,
        languageCode: String
    ): AutocompleteResponseDto

    suspend fun getPlaceDetails(
        placeId: String,
        sessionToken: String?,
        languageCode: String
    ): PlaceDto

    suspend fun fetchPhotoUri(
        photoResourceName: String,
        maxWidthPx: Int,
        maxHeightPx: Int
    ): String?
}

class PlacesApiException(
    val statusCode: Int,
    override val message: String
) : Exception(message)

class PlacesApiClient(
    private val httpClient: HttpClient,
    private val config: PlatformPlacesConfig
) : PlacesRemoteDataSource {

    override val isConfigured: Boolean
        get() = config.hasConfiguredBackend

    override suspend fun searchNearby(
        center: GeoPoint,
        radiusMeters: Double,
        includedTypes: List<String>,
        rankByDistance: Boolean,
        languageCode: String
    ): PlacesSearchResponseDto {
        val body = SearchNearbyRequestDto(
            includedTypes = includedTypes.take(50),
            maxResultCount = 20,
            rankPreference = if (rankByDistance) "DISTANCE" else "POPULARITY",
            languageCode = languageCode,
            locationRestriction = LocationConstraintDto(
                circle = CircleDto(
                    center = LatLngDto(center.latitude, center.longitude),
                    radius = radiusMeters.coerceIn(100.0, 50_000.0)
                )
            )
        )
        val response = httpClient.post("${config.resolvedBaseUrl}/places:searchNearby") {
            applyPlacesHeaders(SEARCH_FIELD_MASK)
            setBody(body)
        }
        return decodeOrThrow(response)
    }

    override suspend fun searchText(
        query: String,
        center: GeoPoint,
        radiusMeters: Double,
        includedType: String?,
        openNow: Boolean?,
        minRating: Double?,
        languageCode: String
    ): PlacesSearchResponseDto {
        val body = SearchTextRequestDto(
            textQuery = query.trim(),
            includedType = includedType,
            openNow = openNow,
            minRating = minRating,
            pageSize = 20,
            languageCode = languageCode,
            locationBias = LocationConstraintDto(
                circle = CircleDto(
                    center = LatLngDto(center.latitude, center.longitude),
                    radius = radiusMeters.coerceIn(500.0, 50_000.0)
                )
            )
        )
        val response = httpClient.post("${config.resolvedBaseUrl}/places:searchText") {
            applyPlacesHeaders(SEARCH_FIELD_MASK)
            setBody(body)
        }
        return decodeOrThrow(response)
    }

    override suspend fun autocomplete(
        input: String,
        center: GeoPoint,
        sessionToken: String,
        languageCode: String
    ): AutocompleteResponseDto {
        val body = AutocompleteRequestDto(
            input = input.trim(),
            sessionToken = sessionToken,
            languageCode = languageCode,
            origin = LatLngDto(center.latitude, center.longitude),
            locationBias = LocationConstraintDto(
                circle = CircleDto(
                    center = LatLngDto(center.latitude, center.longitude),
                    radius = 15_000.0
                )
            ),
            includeQueryPredictions = true
        )
        val response = httpClient.post("${config.resolvedBaseUrl}/places:autocomplete") {
            applyPlacesHeaders(AUTOCOMPLETE_FIELD_MASK)
            setBody(body)
        }
        return decodeOrThrow(response)
    }

    override suspend fun getPlaceDetails(
        placeId: String,
        sessionToken: String?,
        languageCode: String
    ): PlaceDto {
        val encodedId = placeId.trim().removePrefix("places/").encodeURLPathPart()
        val response = httpClient.get("${config.resolvedBaseUrl}/places/$encodedId") {
            applyPlacesHeaders(DETAILS_FIELD_MASK)
            parameter("languageCode", languageCode)
            if (!sessionToken.isNullOrBlank()) {
                parameter("sessionToken", sessionToken)
            }
        }
        return decodeOrThrow(response)
    }

    override suspend fun fetchPhotoUri(
        photoResourceName: String,
        maxWidthPx: Int,
        maxHeightPx: Int
    ): String? {
        val cleanName = photoResourceName.trim().trimStart('/')
        if (!cleanName.startsWith("places/")) return null

        val response = httpClient.get("${config.resolvedBaseUrl}/$cleanName/media") {
            applyPlacesHeaders(fieldMask = null)
            parameter("maxWidthPx", maxWidthPx.coerceIn(1, 4800))
            parameter("maxHeightPx", maxHeightPx.coerceIn(1, 4800))
            parameter("skipHttpRedirect", true)
        }
        val media: PhotoMediaResponseDto = decodeOrThrow(response)
        return media.photoUri?.takeIf { it.startsWith("https://") }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.applyPlacesHeaders(fieldMask: String?) {
        contentType(ContentType.Application.Json)
        if (config.apiKey.isNotBlank()) {
            header("X-Goog-Api-Key", config.apiKey)
        }
        header("X-Goog-Maps-Solution-ID", SOLUTION_ATTRIBUTION_ID)
        if (!fieldMask.isNullOrBlank()) {
            header("X-Goog-FieldMask", fieldMask)
        }
        config.platformHeaders.forEach { (key, value) ->
            header(key, value)
        }
    }

    private suspend inline fun <reified T> decodeOrThrow(response: HttpResponse): T {
        if (response.status.isSuccess()) {
            return response.body()
        }
        val code = response.status.value
        val message = when (code) {
            429 -> "Places service is busy right now. Please try again in a moment."
            403 -> "Places API access is unavailable for this configuration."
            404 -> "This place could not be found."
            else -> "Unable to reach Google Places right now (HTTP $code)."
        }
        throw PlacesApiException(code, message)
    }

    companion object {
        // Mandatory AI Studio usage attribution ID override
        const val SOLUTION_ATTRIBUTION_ID = "gmp_mcp_codeassist_v1_aistudio"

        // Explicit field masks with no spaces and no wildcards (*) to minimize SKU billing
        const val SEARCH_FIELD_MASK =
            "places.id,places.displayName,places.formattedAddress,places.shortFormattedAddress," +
                "places.location,places.primaryType,places.primaryTypeDisplayName,places.types," +
                "places.rating,places.userRatingCount,places.priceLevel,places.currentOpeningHours.openNow," +
                "places.photos,places.businessStatus"

        const val AUTOCOMPLETE_FIELD_MASK =
            "suggestions.placePrediction.placeId,suggestions.placePrediction.text," +
                "suggestions.placePrediction.structuredFormat,suggestions.placePrediction.types," +
                "suggestions.placePrediction.distanceMeters,suggestions.queryPrediction.text"

        const val DETAILS_FIELD_MASK =
            "id,displayName,formattedAddress,shortFormattedAddress,location,primaryType," +
                "primaryTypeDisplayName,types,rating,userRatingCount,priceLevel,businessStatus," +
                "currentOpeningHours,regularOpeningHours,nationalPhoneNumber,internationalPhoneNumber," +
                "websiteUri,googleMapsUri,googleMapsLinks,editorialSummary,reviews,photos," +
                "outdoorSeating,delivery,takeout,dineIn,reservable,goodForChildren," +
                "accessibilityOptions,parkingOptions"
    }
}

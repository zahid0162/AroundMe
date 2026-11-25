package com.zahid.aroundme.domain.repository

import com.zahid.aroundme.domain.model.AutocompleteSuggestion
import com.zahid.aroundme.domain.model.FilterState
import com.zahid.aroundme.domain.model.GeoPoint
import com.zahid.aroundme.domain.model.LocationContext
import com.zahid.aroundme.domain.model.Place
import com.zahid.aroundme.domain.model.PlacesFeedResult
import com.zahid.aroundme.domain.model.SavedCollection
import com.zahid.aroundme.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface PlacesRepository {
    val isLiveApiConfigured: Boolean

    fun observeNearbyPlaces(
        center: GeoPoint,
        filter: FilterState,
        languageCode: String = "en"
    ): Flow<PlacesFeedResult>

    suspend fun autocomplete(
        query: String,
        center: GeoPoint,
        sessionToken: String,
        languageCode: String = "en"
    ): Result<List<AutocompleteSuggestion>>

    suspend fun searchPlaces(
        query: String,
        center: GeoPoint,
        languageCode: String = "en"
    ): Result<List<Place>>

    suspend fun getPlaceById(
        placeId: String,
        sessionToken: String? = null,
        languageCode: String = "en"
    ): Result<Place>

    suspend fun resolvePhotoUri(
        photoResourceName: String,
        maxWidthPx: Int = 600,
        maxHeightPx: Int = 400
    ): String?

    fun getRecentSearches(): Flow<List<String>>
    suspend fun recordSearch(query: String)
    suspend fun clearRecentSearches()
    fun getAvailableLocations(): List<LocationContext>
}

interface SavedPlacesRepository {
    val collections: StateFlow<List<SavedCollection>>
    val savedPlaceIds: StateFlow<Set<String>>
    val placeNotes: StateFlow<Map<String, String>>

    fun getCachedCoordinates(placeId: String): GeoPoint?

    suspend fun toggleSavePlace(
        placeId: String,
        collectionId: String = "want_to_visit",
        coordinates: GeoPoint? = null
    )

    suspend fun assignPlaceToCollection(
        placeId: String,
        collectionId: String,
        coordinates: GeoPoint? = null
    )

    suspend fun createCollection(name: String)
    suspend fun renameCollection(collectionId: String, newName: String)
    suspend fun deleteCollection(collectionId: String)
    suspend fun updatePlaceNote(placeId: String, note: String)
}

interface UserPreferencesRepository {
    val preferences: StateFlow<UserPreferences>
    suspend fun updatePreferences(transform: (UserPreferences) -> UserPreferences)
}

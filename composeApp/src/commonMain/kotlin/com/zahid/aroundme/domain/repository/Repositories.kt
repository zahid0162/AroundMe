package com.zahid.aroundme.domain.repository

import com.zahid.aroundme.domain.model.FilterState
import com.zahid.aroundme.domain.model.GeoPoint
import com.zahid.aroundme.domain.model.LocationContext
import com.zahid.aroundme.domain.model.Place
import com.zahid.aroundme.domain.model.SavedCollection
import com.zahid.aroundme.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface PlacesRepository {
    fun getNearbyPlaces(center: GeoPoint, filter: FilterState): Flow<List<Place>>
    fun searchPlaces(query: String, center: GeoPoint): Flow<List<Place>>
    suspend fun getPlaceById(placeId: String): Place?
    fun getRecentSearches(): Flow<List<String>>
    suspend fun recordSearch(query: String)
    fun getAvailableLocations(): List<LocationContext>
}

interface SavedPlacesRepository {
    val collections: StateFlow<List<SavedCollection>>
    val savedPlaceIds: StateFlow<Set<String>>
    suspend fun toggleSavePlace(placeId: String, collectionId: String = "want_to_visit")
    suspend fun createCollection(name: String)
}

interface UserPreferencesRepository {
    val preferences: StateFlow<UserPreferences>
    suspend fun updatePreferences(transform: (UserPreferences) -> UserPreferences)
}

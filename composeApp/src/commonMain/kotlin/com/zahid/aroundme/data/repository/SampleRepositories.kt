package com.zahid.aroundme.data.repository

import com.zahid.aroundme.core.geo.GeoUtils
import com.zahid.aroundme.data.sample.SamplePlacesDataSource
import com.zahid.aroundme.domain.model.FilterState
import com.zahid.aroundme.domain.model.GeoPoint
import com.zahid.aroundme.domain.model.LocationContext
import com.zahid.aroundme.domain.model.Place
import com.zahid.aroundme.domain.model.SavedCollection
import com.zahid.aroundme.domain.model.SortOption
import com.zahid.aroundme.domain.model.UserPreferences
import com.zahid.aroundme.domain.repository.PlacesRepository
import com.zahid.aroundme.domain.repository.SavedPlacesRepository
import com.zahid.aroundme.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class SamplePlacesRepository : PlacesRepository {
    private val recentSearches = MutableStateFlow(
        listOf("Specialty coffee", "24h Pharmacy", "Levantine dinner", "Parks")
    )

    override fun getNearbyPlaces(center: GeoPoint, filter: FilterState): Flow<List<Place>> {
        val filtered = SamplePlacesDataSource.places.filter { place ->
            val categoryMatch = filter.selectedCategory == null || place.primaryCategory == filter.selectedCategory
            val openMatch = !filter.openNowOnly || place.isOpenNow == true
            val ratingMatch = filter.minRating == null || (place.rating ?: 0.0) >= filter.minRating
            val distance = GeoUtils.haversineMeters(center, place.location)
            categoryMatch && openMatch && ratingMatch && distance <= filter.maxDistanceMeters
        }

        val sorted = when (filter.sortOption) {
            SortOption.NEAREST -> filtered.sortedBy { GeoUtils.haversineMeters(center, it.location) }
            SortOption.HIGHEST_RATED -> filtered.sortedByDescending { it.rating ?: 0.0 }
            SortOption.MOST_REVIEWED -> filtered.sortedByDescending { it.reviewCount ?: 0 }
            SortOption.RECOMMENDED -> filtered.sortedByDescending { place ->
                val ratingScore = (place.rating ?: 3.5) * 2.0
                val distancePenalty = GeoUtils.haversineMeters(center, place.location) / 1000.0
                ratingScore - distancePenalty
            }
        }
        return flowOf(sorted)
    }

    override fun searchPlaces(query: String, center: GeoPoint): Flow<List<Place>> {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return flowOf(emptyList())
        return flowOf(
            SamplePlacesDataSource.places.filter { place ->
                place.name.lowercase().contains(trimmed) ||
                    place.primaryCategoryLabel.lowercase().contains(trimmed) ||
                    place.address.lowercase().contains(trimmed)
            }.sortedBy { GeoUtils.haversineMeters(center, it.location) }
        )
    }

    override suspend fun getPlaceById(placeId: String): Place? =
        SamplePlacesDataSource.places.find { it.id == placeId }

    override fun getRecentSearches(): Flow<List<String>> = recentSearches.asStateFlow()

    override suspend fun recordSearch(query: String) {
        val clean = query.trim()
        if (clean.isNotEmpty()) {
            recentSearches.update { current ->
                (listOf(clean) + current.filterNot { it.equals(clean, ignoreCase = true) }).take(6)
            }
        }
    }

    override fun getAvailableLocations(): List<LocationContext> = SamplePlacesDataSource.locations
}

class InMemorySavedPlacesRepository : SavedPlacesRepository {
    private val _collections = MutableStateFlow(SamplePlacesDataSource.defaultCollections)
    override val collections: StateFlow<List<SavedCollection>> = _collections.asStateFlow()

    private val _savedPlaceIds = MutableStateFlow(
        SamplePlacesDataSource.defaultCollections.flatMap { it.placeIds }.toSet()
    )
    override val savedPlaceIds: StateFlow<Set<String>> = _savedPlaceIds.asStateFlow()

    override suspend fun toggleSavePlace(placeId: String, collectionId: String) {
        val currentlySaved = _savedPlaceIds.value.contains(placeId)
        _collections.update { list ->
            list.map { collection ->
                if (currentlySaved) {
                    collection.copy(placeIds = collection.placeIds - placeId)
                } else if (collection.id == collectionId) {
                    collection.copy(placeIds = (collection.placeIds + placeId).distinct())
                } else {
                    collection
                }
            }
        }
        _savedPlaceIds.value = _collections.value.flatMap { it.placeIds }.toSet()
    }

    override suspend fun createCollection(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        val id = "custom_${clean.lowercase().replace(" ", "_")}_${_collections.value.size}"
        _collections.update { it + SavedCollection(id = id, name = clean, nameAr = clean, placeIds = emptyList()) }
    }
}

class InMemoryUserPreferencesRepository : UserPreferencesRepository {
    private val _preferences = MutableStateFlow(UserPreferences())
    override val preferences: StateFlow<UserPreferences> = _preferences.asStateFlow()

    override suspend fun updatePreferences(transform: (UserPreferences) -> UserPreferences) {
        _preferences.update(transform)
    }
}

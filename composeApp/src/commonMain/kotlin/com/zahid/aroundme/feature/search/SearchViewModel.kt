package com.zahid.aroundme.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zahid.aroundme.domain.model.AppLanguage
import com.zahid.aroundme.domain.model.DistanceUnit
import com.zahid.aroundme.domain.model.GeoPoint
import com.zahid.aroundme.domain.model.Place
import com.zahid.aroundme.domain.repository.PlacesRepository
import com.zahid.aroundme.domain.repository.SavedPlacesRepository
import com.zahid.aroundme.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val results: List<Place> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val referenceCenter: GeoPoint = GeoPoint(24.6949, 46.6852),
    val savedPlaceIds: Set<String> = emptySet(),
    val distanceUnit: DistanceUnit = DistanceUnit.METRIC,
    val isArabic: Boolean = false
)

class SearchViewModel(
    private val placesRepository: PlacesRepository,
    private val savedPlacesRepository: SavedPlacesRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val center = placesRepository.getAvailableLocations().first().coordinates

    @OptIn(ExperimentalCoroutinesApi::class)
    private val searchResults = query.flatMapLatest { q ->
        placesRepository.searchPlaces(q, center)
    }

    val uiState: StateFlow<SearchUiState> = combine(
        query,
        searchResults,
        placesRepository.getRecentSearches(),
        savedPlacesRepository.savedPlaceIds,
        userPreferencesRepository.preferences
    ) { q, results, recent, savedIds, prefs ->
        SearchUiState(
            query = q,
            results = results,
            recentSearches = recent,
            referenceCenter = center,
            savedPlaceIds = savedIds,
            distanceUnit = prefs.distanceUnit,
            isArabic = prefs.language == AppLanguage.ARABIC
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SearchUiState(referenceCenter = center)
    )

    fun onQueryChanged(newQuery: String) {
        query.value = newQuery
    }

    fun submitQuery(selectedQuery: String) {
        query.value = selectedQuery
        viewModelScope.launch {
            placesRepository.recordSearch(selectedQuery)
        }
    }

    fun toggleSavePlace(placeId: String) {
        viewModelScope.launch {
            savedPlacesRepository.toggleSavePlace(placeId)
        }
    }
}

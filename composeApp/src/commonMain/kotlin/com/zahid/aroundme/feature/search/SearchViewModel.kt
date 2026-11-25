package com.zahid.aroundme.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zahid.aroundme.domain.model.AppLanguage
import com.zahid.aroundme.domain.model.AutocompleteSuggestion
import com.zahid.aroundme.domain.model.DistanceUnit
import com.zahid.aroundme.domain.model.GeoPoint
import com.zahid.aroundme.domain.model.Place
import com.zahid.aroundme.domain.repository.LocationRepository
import com.zahid.aroundme.domain.repository.PlacesRepository
import com.zahid.aroundme.domain.repository.SavedPlacesRepository
import com.zahid.aroundme.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

data class SearchUiState(
    val query: String = "",
    val suggestions: List<AutocompleteSuggestion> = emptyList(),
    val results: List<Place> = emptyList(),
    val hasSubmittedSearch: Boolean = false,
    val isLoadingSuggestions: Boolean = false,
    val isSearching: Boolean = false,
    val errorMessage: String? = null,
    val recentSearches: List<String> = emptyList(),
    val referenceCenter: GeoPoint = GeoPoint(24.6949, 46.6852),
    val savedPlaceIds: Set<String> = emptySet(),
    val distanceUnit: DistanceUnit = DistanceUnit.METRIC,
    val isArabic: Boolean = false
)

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val placesRepository: PlacesRepository,
    private val savedPlacesRepository: SavedPlacesRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val defaultCenter = placesRepository.getAvailableLocations().first().coordinates
    private val query = MutableStateFlow("")
    private val suggestions = MutableStateFlow<List<AutocompleteSuggestion>>(emptyList())
    private val results = MutableStateFlow<List<Place>>(emptyList())
    private val hasSubmittedSearch = MutableStateFlow(false)
    private val isLoadingSuggestions = MutableStateFlow(false)
    private val isSearching = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    private var activeSessionToken: String = generateSessionToken()
    private var searchJob: Job? = null

    init {
        query
            .debounce(300)
            .distinctUntilChanged()
            .onEach { q ->
                val trimmed = q.trim()
                if (trimmed.isEmpty()) {
                    suggestions.value = emptyList()
                    results.value = emptyList()
                    hasSubmittedSearch.value = false
                    errorMessage.value = null
                    return@onEach
                }
                if (!hasSubmittedSearch.value) {
                    fetchAutocomplete(trimmed)
                }
            }
            .launchIn(viewModelScope)
    }

    val uiState: StateFlow<SearchUiState> = combine(
        query,
        suggestions,
        results,
        hasSubmittedSearch,
        isLoadingSuggestions,
        isSearching,
        errorMessage,
        placesRepository.getRecentSearches(),
        savedPlacesRepository.savedPlaceIds,
        userPreferencesRepository.preferences,
        locationRepository.currentLocation
    ) { values ->
        val q = values[0] as String
        @Suppress("UNCHECKED_CAST")
        val autoSuggestions = values[1] as List<AutocompleteSuggestion>
        @Suppress("UNCHECKED_CAST")
        val searchResults = values[2] as List<Place>
        val submitted = values[3] as Boolean
        val loadingSugg = values[4] as Boolean
        val loadingSearch = values[5] as Boolean
        val err = values[6] as String?
        @Suppress("UNCHECKED_CAST")
        val recent = values[7] as List<String>
        @Suppress("UNCHECKED_CAST")
        val savedIds = values[8] as Set<String>
        val prefs = values[9] as com.zahid.aroundme.domain.model.UserPreferences
        val gpsPoint = values[10] as GeoPoint?

        SearchUiState(
            query = q,
            suggestions = autoSuggestions,
            results = searchResults,
            hasSubmittedSearch = submitted,
            isLoadingSuggestions = loadingSugg,
            isSearching = loadingSearch,
            errorMessage = err,
            recentSearches = recent,
            referenceCenter = gpsPoint ?: defaultCenter,
            savedPlaceIds = savedIds,
            distanceUnit = prefs.distanceUnit,
            isArabic = prefs.language == AppLanguage.ARABIC
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SearchUiState(referenceCenter = defaultCenter)
    )

    fun onQueryChanged(newQuery: String) {
        hasSubmittedSearch.value = false
        errorMessage.value = null
        query.value = newQuery
        if (newQuery.isBlank()) {
            suggestions.value = emptyList()
            results.value = emptyList()
            rotateSessionToken()
        }
    }

    fun submitQuery(selectedQuery: String = query.value) {
        val clean = selectedQuery.trim()
        if (clean.isEmpty()) return
        query.value = clean
        hasSubmittedSearch.value = true
        suggestions.value = emptyList()
        errorMessage.value = null
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            isSearching.value = true
            placesRepository.recordSearch(clean)
            val langCode = userPreferencesRepository.preferences.value.language.code
            val center = uiState.value.referenceCenter
            placesRepository.searchPlaces(clean, center, langCode)
                .onSuccess { places ->
                    results.value = places
                    errorMessage.value = null
                }
                .onFailure { throwable ->
                    results.value = emptyList()
                    errorMessage.value = throwable.message ?: "Unable to search places."
                }
            isSearching.value = false
            rotateSessionToken()
        }
    }

    fun consumeSessionTokenForPlaceSelection(): String {
        val current = activeSessionToken
        rotateSessionToken()
        return current
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            placesRepository.clearRecentSearches()
        }
    }

    fun toggleSavePlace(placeId: String) {
        val coords = results.value.find { it.id == placeId }?.location
        viewModelScope.launch {
            savedPlacesRepository.toggleSavePlace(placeId = placeId, coordinates = coords)
        }
    }

    suspend fun resolvePhotoUri(photoResourceName: String): String? =
        placesRepository.resolvePhotoUri(photoResourceName, maxWidthPx = 320, maxHeightPx = 320)

    private suspend fun fetchAutocomplete(trimmedQuery: String) {
        isLoadingSuggestions.value = true
        val langCode = userPreferencesRepository.preferences.value.language.code
        val center = uiState.value.referenceCenter
        placesRepository.autocomplete(
            query = trimmedQuery,
            center = center,
            sessionToken = activeSessionToken,
            languageCode = langCode
        ).onSuccess { list ->
            suggestions.value = list
            errorMessage.value = null
        }.onFailure { throwable ->
            suggestions.value = emptyList()
            errorMessage.value = throwable.message
        }
        isLoadingSuggestions.value = false
    }

    private fun rotateSessionToken() {
        activeSessionToken = generateSessionToken()
    }

    private fun generateSessionToken(): String {
        val bytes = Random.nextBytes(16)
        return bytes.joinToString("") { byte ->
            val v = byte.toInt() and 0xFF
            v.toString(16).padStart(2, '0')
        }
    }
}

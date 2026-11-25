package com.zahid.aroundme.feature.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zahid.aroundme.domain.model.AppLanguage
import com.zahid.aroundme.domain.model.DistanceUnit
import com.zahid.aroundme.domain.model.FilterState
import com.zahid.aroundme.domain.model.LocationContext
import com.zahid.aroundme.domain.model.Place
import com.zahid.aroundme.domain.model.PlaceCategory
import com.zahid.aroundme.domain.model.SortOption
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ExploreViewMode {
    MAP,
    LIST
}

enum class SheetDetent {
    COLLAPSED,
    HALF_EXPANDED,
    EXPANDED
}

data class ExploreUiState(
    val activeLocation: LocationContext,
    val availableLocations: List<LocationContext> = emptyList(),
    val places: List<Place> = emptyList(),
    val filter: FilterState = FilterState(),
    val selectedPlaceId: String? = null,
    val viewMode: ExploreViewMode = ExploreViewMode.MAP,
    val sheetDetent: SheetDetent = SheetDetent.HALF_EXPANDED,
    val savedPlaceIds: Set<String> = emptySet(),
    val distanceUnit: DistanceUnit = DistanceUnit.METRIC,
    val isArabic: Boolean = false,
    val showSearchThisArea: Boolean = false
)

class ExploreViewModel(
    private val placesRepository: PlacesRepository,
    private val savedPlacesRepository: SavedPlacesRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val availableLocations = placesRepository.getAvailableLocations()
    private val activeLocation = MutableStateFlow(availableLocations.first())
    private val filterState = MutableStateFlow(FilterState())
    private val selectedPlaceId = MutableStateFlow<String?>(null)
    private val viewMode = MutableStateFlow(ExploreViewMode.MAP)
    private val sheetDetent = MutableStateFlow(SheetDetent.HALF_EXPANDED)
    private val showSearchThisArea = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val placesFlow = combine(activeLocation, filterState) { location, filter ->
        location to filter
    }.flatMapLatest { (location, filter) ->
        placesRepository.getNearbyPlaces(location.coordinates, filter)
    }

    val uiState: StateFlow<ExploreUiState> = combine(
        activeLocation,
        placesFlow,
        filterState,
        selectedPlaceId,
        viewMode,
        sheetDetent,
        savedPlacesRepository.savedPlaceIds,
        userPreferencesRepository.preferences,
        showSearchThisArea
    ) { values ->
        val location = values[0] as LocationContext
        @Suppress("UNCHECKED_CAST")
        val places = values[1] as List<Place>
        val filter = values[2] as FilterState
        val selectedId = values[3] as String?
        val mode = values[4] as ExploreViewMode
        val detent = values[5] as SheetDetent
        @Suppress("UNCHECKED_CAST")
        val savedIds = values[6] as Set<String>
        val prefs = values[7] as com.zahid.aroundme.domain.model.UserPreferences
        val searchArea = values[8] as Boolean

        ExploreUiState(
            activeLocation = location,
            availableLocations = availableLocations,
            places = places,
            filter = filter,
            selectedPlaceId = selectedId,
            viewMode = mode,
            sheetDetent = detent,
            savedPlaceIds = savedIds,
            distanceUnit = prefs.distanceUnit,
            isArabic = prefs.language == AppLanguage.ARABIC,
            showSearchThisArea = searchArea
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExploreUiState(
            activeLocation = availableLocations.first(),
            availableLocations = availableLocations
        )
    )

    fun selectCategory(category: PlaceCategory?) {
        filterState.update {
            it.copy(selectedCategory = if (it.selectedCategory == category) null else category)
        }
    }

    fun toggleOpenNow() {
        filterState.update { it.copy(openNowOnly = !it.openNowOnly) }
    }

    fun selectSortOption(sortOption: SortOption) {
        filterState.update { it.copy(sortOption = sortOption) }
    }

    fun selectPlace(placeId: String?) {
        selectedPlaceId.value = placeId
    }

    fun setViewMode(mode: ExploreViewMode) {
        viewMode.value = mode
    }

    fun cycleSheetDetent() {
        sheetDetent.update {
            when (it) {
                SheetDetent.COLLAPSED -> SheetDetent.HALF_EXPANDED
                SheetDetent.HALF_EXPANDED -> SheetDetent.EXPANDED
                SheetDetent.EXPANDED -> SheetDetent.COLLAPSED
            }
        }
    }

    fun setSheetDetent(detent: SheetDetent) {
        sheetDetent.value = detent
    }

    fun selectLocation(location: LocationContext) {
        activeLocation.value = location
        selectedPlaceId.value = null
        showSearchThisArea.value = false
    }

    fun onMapMoved() {
        showSearchThisArea.value = true
    }

    fun searchThisArea() {
        showSearchThisArea.value = false
    }

    fun toggleSavePlace(placeId: String) {
        viewModelScope.launch {
            savedPlacesRepository.toggleSavePlace(placeId)
        }
    }
}

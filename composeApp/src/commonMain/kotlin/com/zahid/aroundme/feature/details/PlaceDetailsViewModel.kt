package com.zahid.aroundme.feature.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zahid.aroundme.core.geo.GeoUtils
import com.zahid.aroundme.domain.model.AppLanguage
import com.zahid.aroundme.domain.model.DistanceUnit
import com.zahid.aroundme.domain.model.Place
import com.zahid.aroundme.domain.repository.PlacesRepository
import com.zahid.aroundme.domain.repository.SavedPlacesRepository
import com.zahid.aroundme.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlaceDetailsUiState(
    val place: Place? = null,
    val distanceLabel: String? = null,
    val isSaved: Boolean = false,
    val isArabic: Boolean = false
)

class PlaceDetailsViewModel(
    private val placeId: String,
    private val placesRepository: PlacesRepository,
    private val savedPlacesRepository: SavedPlacesRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val loadedPlace = MutableStateFlow<Place?>(null)
    private val center = placesRepository.getAvailableLocations().first().coordinates

    init {
        viewModelScope.launch {
            loadedPlace.value = placesRepository.getPlaceById(placeId)
        }
    }

    val uiState: StateFlow<PlaceDetailsUiState> = combine(
        loadedPlace,
        savedPlacesRepository.savedPlaceIds,
        userPreferencesRepository.preferences
    ) { place, savedIds, prefs ->
        val isArabic = prefs.language == AppLanguage.ARABIC
        val distance = place?.let {
            GeoUtils.formatDistance(
                GeoUtils.haversineMeters(center, it.location),
                prefs.distanceUnit,
                isArabic
            )
        }
        PlaceDetailsUiState(
            place = place,
            distanceLabel = distance,
            isSaved = savedIds.contains(placeId),
            isArabic = isArabic
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlaceDetailsUiState()
    )

    fun toggleSave() {
        viewModelScope.launch {
            savedPlacesRepository.toggleSavePlace(placeId)
        }
    }
}

package com.zahid.aroundme.feature.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zahid.aroundme.domain.model.AppLanguage
import com.zahid.aroundme.domain.model.DistanceUnit
import com.zahid.aroundme.domain.model.GeoPoint
import com.zahid.aroundme.domain.model.Place
import com.zahid.aroundme.domain.model.SavedCollection
import com.zahid.aroundme.domain.repository.PlacesRepository
import com.zahid.aroundme.domain.repository.SavedPlacesRepository
import com.zahid.aroundme.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SavedUiState(
    val collections: List<SavedCollection> = emptyList(),
    val selectedCollectionId: String? = null,
    val hydratedPlaces: List<Place> = emptyList(),
    val referenceCenter: GeoPoint = GeoPoint(24.6949, 46.6852),
    val distanceUnit: DistanceUnit = DistanceUnit.METRIC,
    val isArabic: Boolean = false
)

class SavedViewModel(
    private val placesRepository: PlacesRepository,
    private val savedPlacesRepository: SavedPlacesRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val selectedCollectionId = MutableStateFlow<String?>(null)
    private val center = placesRepository.getAvailableLocations().first().coordinates

    val uiState: StateFlow<SavedUiState> = combine(
        savedPlacesRepository.collections,
        savedPlacesRepository.savedPlaceIds,
        selectedCollectionId,
        userPreferencesRepository.preferences
    ) { collections, savedIds, activeCollectionId, prefs ->
        val activeIds = if (activeCollectionId == null) {
            savedIds
        } else {
            collections.find { it.id == activeCollectionId }?.placeIds?.toSet().orEmpty()
        }
        val places = activeIds.mapNotNull { id -> placesRepository.getPlaceById(id) }
        SavedUiState(
            collections = collections,
            selectedCollectionId = activeCollectionId,
            hydratedPlaces = places,
            referenceCenter = center,
            distanceUnit = prefs.distanceUnit,
            isArabic = prefs.language == AppLanguage.ARABIC
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SavedUiState()
    )

    fun selectCollection(collectionId: String?) {
        selectedCollectionId.value = if (selectedCollectionId.value == collectionId) null else collectionId
    }

    fun toggleSavePlace(placeId: String) {
        viewModelScope.launch {
            savedPlacesRepository.toggleSavePlace(placeId)
        }
    }
}

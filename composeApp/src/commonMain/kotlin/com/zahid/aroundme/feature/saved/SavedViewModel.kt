package com.zahid.aroundme.feature.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zahid.aroundme.domain.model.AppLanguage
import com.zahid.aroundme.domain.model.DistanceUnit
import com.zahid.aroundme.domain.model.GeoPoint
import com.zahid.aroundme.domain.model.Place
import com.zahid.aroundme.domain.model.SavedCollection
import com.zahid.aroundme.domain.model.UserPreferences
import com.zahid.aroundme.domain.repository.LocationRepository
import com.zahid.aroundme.domain.repository.PlacesRepository
import com.zahid.aroundme.domain.repository.SavedPlacesRepository
import com.zahid.aroundme.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SavedPlaceItem(
    val placeId: String,
    val place: Place?,
    val fallbackCoordinates: GeoPoint?,
    val userNote: String
)

data class SavedUiState(
    val collections: List<SavedCollection> = emptyList(),
    val selectedCollectionId: String? = null,
    val items: List<SavedPlaceItem> = emptyList(),
    val isLoading: Boolean = false,
    val referenceCenter: GeoPoint = GeoPoint(24.6949, 46.6852),
    val distanceUnit: DistanceUnit = DistanceUnit.METRIC,
    val isArabic: Boolean = false
) {
    val hydratedPlaces: List<Place>
        get() = items.mapNotNull { it.place }
}

class SavedViewModel(
    private val placesRepository: PlacesRepository,
    private val savedPlacesRepository: SavedPlacesRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val selectedCollectionId = MutableStateFlow<String?>(null)
    private val defaultCenter = placesRepository.getAvailableLocations().first().coordinates
    private val isHydrating = MutableStateFlow(false)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val hydratedPlacesFlow = combine(
        savedPlacesRepository.collections,
        savedPlacesRepository.savedPlaceIds,
        selectedCollectionId,
        savedPlacesRepository.placeNotes,
        userPreferencesRepository.preferences
    ) { collections, savedIds, activeCollectionId, notesMap, prefs ->
        val activeIds = if (activeCollectionId == null) {
            savedIds.toList()
        } else {
            collections.find { it.id == activeCollectionId }?.placeIds.orEmpty()
        }
        Triple(activeIds, notesMap, prefs.language.code)
    }.mapLatest { (activeIds, notesMap, langCode) ->
        if (activeIds.isEmpty()) {
            isHydrating.value = false
            return@mapLatest emptyList()
        }
        isHydrating.value = true
        val resolved = coroutineScope {
            activeIds.map { id ->
                async {
                    val place = placesRepository.getPlaceById(id, languageCode = langCode).getOrNull()
                    SavedPlaceItem(
                        placeId = id,
                        place = place,
                        fallbackCoordinates = savedPlacesRepository.getCachedCoordinates(id),
                        userNote = notesMap[id].orEmpty()
                    )
                }
            }.awaitAll()
        }
        isHydrating.value = false
        resolved
    }

    val uiState: StateFlow<SavedUiState> = combine(
        savedPlacesRepository.collections,
        selectedCollectionId,
        hydratedPlacesFlow,
        isHydrating,
        userPreferencesRepository.preferences,
        locationRepository.currentLocation
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val collections = values[0] as List<SavedCollection>
        val activeCollectionId = values[1] as String?
        @Suppress("UNCHECKED_CAST")
        val items = values[2] as List<SavedPlaceItem>
        val loading = values[3] as Boolean
        val prefs = values[4] as UserPreferences
        val gpsPoint = values[5] as GeoPoint?

        SavedUiState(
            collections = collections,
            selectedCollectionId = activeCollectionId,
            items = items,
            isLoading = loading,
            referenceCenter = gpsPoint ?: defaultCenter,
            distanceUnit = prefs.distanceUnit,
            isArabic = prefs.language == AppLanguage.ARABIC
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SavedUiState(referenceCenter = defaultCenter)
    )

    fun selectCollection(collectionId: String?) {
        selectedCollectionId.value = if (selectedCollectionId.value == collectionId) null else collectionId
    }

    fun toggleSavePlace(placeId: String) {
        viewModelScope.launch {
            savedPlacesRepository.toggleSavePlace(placeId)
        }
    }

    fun createCollection(name: String) {
        viewModelScope.launch {
            savedPlacesRepository.createCollection(name)
        }
    }

    fun deleteCollection(collectionId: String) {
        if (selectedCollectionId.value == collectionId) {
            selectedCollectionId.value = null
        }
        viewModelScope.launch {
            savedPlacesRepository.deleteCollection(collectionId)
        }
    }

    fun updatePlaceNote(placeId: String, note: String) {
        viewModelScope.launch {
            savedPlacesRepository.updatePlaceNote(placeId, note)
        }
    }

    suspend fun resolvePhotoUri(photoResourceName: String): String? =
        placesRepository.resolvePhotoUri(photoResourceName, maxWidthPx = 320, maxHeightPx = 320)
}

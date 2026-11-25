package com.zahid.aroundme.feature.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zahid.aroundme.core.geo.GeoUtils
import com.zahid.aroundme.core.platform.PlatformActions
import com.zahid.aroundme.domain.model.AppLanguage
import com.zahid.aroundme.domain.model.Place
import com.zahid.aroundme.domain.model.PlacePhoto
import com.zahid.aroundme.domain.model.SavedCollection
import com.zahid.aroundme.domain.model.UserPreferences
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ResolvedPlacePhoto(
    val photo: PlacePhoto,
    val resolvedUrl: String
)

data class PlaceDetailsUiState(
    val place: Place? = null,
    val resolvedPhotos: List<ResolvedPlacePhoto> = emptyList(),
    val distanceLabel: String? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isSaved: Boolean = false,
    val collections: List<SavedCollection> = emptyList(),
    val assignedCollectionIds: Set<String> = emptySet(),
    val userNote: String = "",
    val isArabic: Boolean = false
)

class PlaceDetailsViewModel(
    private val placeId: String,
    private val sessionToken: String? = null,
    private val placesRepository: PlacesRepository,
    private val savedPlacesRepository: SavedPlacesRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val platformActions: PlatformActions
) : ViewModel() {

    private val loadedPlace = MutableStateFlow<Place?>(null)
    private val resolvedPhotos = MutableStateFlow<List<ResolvedPlacePhoto>>(emptyList())
    private val isLoading = MutableStateFlow(true)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val center = placesRepository.getAvailableLocations().first().coordinates
    private var sessionConsumed = false

    init {
        loadDetails()
    }

    val uiState: StateFlow<PlaceDetailsUiState> = combine(
        loadedPlace,
        resolvedPhotos,
        isLoading,
        errorMessage,
        savedPlacesRepository.savedPlaceIds,
        savedPlacesRepository.collections,
        savedPlacesRepository.placeNotes,
        userPreferencesRepository.preferences
    ) { values ->
        val place = values[0] as Place?
        @Suppress("UNCHECKED_CAST")
        val photos = values[1] as List<ResolvedPlacePhoto>
        val loading = values[2] as Boolean
        val error = values[3] as String?
        @Suppress("UNCHECKED_CAST")
        val savedIds = values[4] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val allCollections = values[5] as List<SavedCollection>
        @Suppress("UNCHECKED_CAST")
        val notesMap = values[6] as Map<String, String>
        val prefs = values[7] as UserPreferences

        val isArabic = prefs.language == AppLanguage.ARABIC
        val distance = place?.let {
            GeoUtils.formatDistance(
                GeoUtils.haversineMeters(center, it.location),
                prefs.distanceUnit,
                isArabic
            )
        }
        val assigned = allCollections
            .filter { it.placeIds.contains(placeId) }
            .map { it.id }
            .toSet()

        PlaceDetailsUiState(
            place = place,
            resolvedPhotos = photos,
            distanceLabel = distance,
            isLoading = loading,
            errorMessage = error,
            isSaved = savedIds.contains(placeId),
            collections = allCollections,
            assignedCollectionIds = assigned,
            userNote = notesMap[placeId].orEmpty(),
            isArabic = isArabic
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlaceDetailsUiState()
    )

    fun retry() {
        loadDetails()
    }

    fun toggleSave() {
        val coords = loadedPlace.value?.location
        viewModelScope.launch {
            savedPlacesRepository.toggleSavePlace(placeId = placeId, coordinates = coords)
        }
    }

    fun toggleCollectionAssignment(collectionId: String) {
        val coords = loadedPlace.value?.location
        viewModelScope.launch {
            savedPlacesRepository.assignPlaceToCollection(
                placeId = placeId,
                collectionId = collectionId,
                coordinates = coords
            )
        }
    }

    fun createCollectionAndAssign(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        val coords = loadedPlace.value?.location
        viewModelScope.launch {
            savedPlacesRepository.createCollection(clean)
            val created = savedPlacesRepository.collections.value.lastOrNull {
                it.name.equals(clean, ignoreCase = true)
            }
            if (created != null) {
                savedPlacesRepository.assignPlaceToCollection(
                    placeId = placeId,
                    collectionId = created.id,
                    coordinates = coords
                )
            }
        }
    }

    fun updateNote(note: String) {
        viewModelScope.launch {
            savedPlacesRepository.updatePlaceNote(placeId = placeId, note = note)
        }
    }

    fun openDirections() {
        val place = loadedPlace.value ?: return
        platformActions.openDirections(
            placeName = place.name,
            location = place.location,
            placeId = place.id,
            directionsUri = place.directionsUri
        )
    }

    fun dialPhone() {
        val phone = loadedPlace.value?.phoneNumber ?: return
        platformActions.dialPhone(phone)
    }

    fun openWebsite() {
        val website = loadedPlace.value?.websiteUrl ?: return
        platformActions.openWebsite(website)
    }

    fun sharePlace() {
        val place = loadedPlace.value ?: return
        platformActions.sharePlace(
            placeName = place.name,
            address = place.address,
            googleMapsUri = place.googleMapsUri
        )
    }

    private fun loadDetails() {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            val langCode = userPreferencesRepository.preferences.value.language.code
            val tokenToUse = if (!sessionConsumed) sessionToken else null
            sessionConsumed = true

            placesRepository.getPlaceById(
                placeId = placeId,
                sessionToken = tokenToUse,
                languageCode = langCode
            ).onSuccess { place ->
                loadedPlace.value = place
                errorMessage.value = null
                isLoading.value = false
                resolvePhotos(place.photos.take(4))
            }.onFailure { throwable ->
                loadedPlace.value = null
                errorMessage.value = throwable.message ?: "Unable to load place details."
                isLoading.value = false
            }
        }
    }

    private suspend fun resolvePhotos(photos: List<PlacePhoto>) {
        if (photos.isEmpty()) {
            resolvedPhotos.value = emptyList()
            return
        }
        val loaded = coroutineScope {
            photos.map { photo ->
                async {
                    val uri = placesRepository.resolvePhotoUri(
                        photoResourceName = photo.resourceName,
                        maxWidthPx = 900,
                        maxHeightPx = 600
                    )
                    if (uri != null) ResolvedPlacePhoto(photo, uri) else null
                }
            }.awaitAll().filterNotNull()
        }
        resolvedPhotos.value = loaded
    }
}

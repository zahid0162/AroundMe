package com.zahid.aroundme.feature.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zahid.aroundme.core.geo.GeoUtils
import com.zahid.aroundme.domain.model.AppLanguage
import com.zahid.aroundme.domain.model.CameraCommand
import com.zahid.aroundme.domain.model.DistanceUnit
import com.zahid.aroundme.domain.model.FilterState
import com.zahid.aroundme.domain.model.GeoPoint
import com.zahid.aroundme.domain.model.LocationContext
import com.zahid.aroundme.domain.model.LocationPermissionStatus
import com.zahid.aroundme.domain.model.MapCameraRegion
import com.zahid.aroundme.domain.model.Place
import com.zahid.aroundme.domain.model.PlaceCategory
import com.zahid.aroundme.domain.model.SortOption
import com.zahid.aroundme.domain.model.ThemePreference
import com.zahid.aroundme.domain.model.UserPreferences
import com.zahid.aroundme.domain.repository.LocationRepository
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
import kotlin.math.max

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
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val isArabic: Boolean = false,
    val permissionStatus: LocationPermissionStatus = LocationPermissionStatus.UNKNOWN,
    val showLocationBanner: Boolean = false,
    val isLocating: Boolean = false,
    val showSearchThisArea: Boolean = false,
    val cameraCommand: CameraCommand? = null
)

class ExploreViewModel(
    private val placesRepository: PlacesRepository,
    private val savedPlacesRepository: SavedPlacesRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val availableLocations = placesRepository.getAvailableLocations()
    private val activeLocation = MutableStateFlow(availableLocations.first())
    private val queryCenter = MutableStateFlow(availableLocations.first().coordinates)
    private val pendingCameraRegion = MutableStateFlow<MapCameraRegion?>(null)
    private val filterState = MutableStateFlow(FilterState())
    private val selectedPlaceId = MutableStateFlow<String?>(null)
    private val viewMode = MutableStateFlow(ExploreViewMode.MAP)
    private val sheetDetent = MutableStateFlow(SheetDetent.HALF_EXPANDED)
    private val showSearchThisArea = MutableStateFlow(false)
    private val locationBannerDismissed = MutableStateFlow(false)
    private val isLocating = MutableStateFlow(false)
    private var nextCommandId = 1L
    private val cameraCommand = MutableStateFlow(
        CameraCommand(
            id = nextCommandId++,
            target = availableLocations.first().coordinates,
            zoom = 14.2f,
            animate = false
        )
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val placesFlow = combine(queryCenter, filterState) { center, filter ->
        center to filter
    }.flatMapLatest { (center, filter) ->
        placesRepository.getNearbyPlaces(center, filter)
    }

    init {
        locationRepository.refreshPermissionState()
        if (locationRepository.permissionStatus.value.isGranted) {
            centerOnDeviceLocation()
        }
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
        locationRepository.permissionStatus,
        locationBannerDismissed,
        isLocating,
        showSearchThisArea,
        cameraCommand
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
        val prefs = values[7] as UserPreferences
        val permStatus = values[8] as LocationPermissionStatus
        val bannerDismissed = values[9] as Boolean
        val locating = values[10] as Boolean
        val searchArea = values[11] as Boolean
        val camCmd = values[12] as CameraCommand?

        val shouldShowBanner = !bannerDismissed && !permStatus.isGranted

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
            themePreference = prefs.themePreference,
            isArabic = prefs.language == AppLanguage.ARABIC,
            permissionStatus = permStatus,
            showLocationBanner = shouldShowBanner,
            isLocating = locating,
            showSearchThisArea = searchArea,
            cameraCommand = camCmd
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExploreUiState(
            activeLocation = availableLocations.first(),
            availableLocations = availableLocations,
            cameraCommand = cameraCommand.value
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

    fun onMarkerSelected(placeId: String) {
        selectedPlaceId.value = placeId
        if (sheetDetent.value == SheetDetent.COLLAPSED) {
            sheetDetent.value = SheetDetent.HALF_EXPANDED
        }
    }

    fun onListPlaceHighlighted(place: Place) {
        selectedPlaceId.value = place.id
        issueCameraCommand(place.location, zoom = 15.2f, animate = true)
    }

    fun onMapTap() {
        selectedPlaceId.value = null
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
        queryCenter.value = location.coordinates
        selectedPlaceId.value = null
        showSearchThisArea.value = false
        pendingCameraRegion.value = null
        issueCameraCommand(location.coordinates, zoom = 14.2f, animate = true)
    }

    fun onCameraIdle(region: MapCameraRegion) {
        pendingCameraRegion.value = region
        val movedMeters = GeoUtils.haversineMeters(queryCenter.value, region.center)
        val thresholdMeters = max(350.0, region.visibleRadiusMeters * 0.20)
        if (movedMeters >= thresholdMeters) {
            showSearchThisArea.value = true
        }
    }

    fun searchThisArea() {
        val region = pendingCameraRegion.value ?: return
        queryCenter.value = region.center
        activeLocation.value = LocationContext(
            label = if (uiState.value.isArabic) "المنطقة المحددة" else "Selected Map Area",
            coordinates = region.center,
            isCurrentLocation = false
        )
        filterState.update {
            it.copy(maxDistanceMeters = region.visibleRadiusMeters.coerceIn(1000.0, 50_000.0))
        }
        showSearchThisArea.value = false
    }

    fun onPermissionResult(status: LocationPermissionStatus) {
        locationRepository.refreshPermissionState()
        if (status.isGranted) {
            locationBannerDismissed.value = true
            centerOnDeviceLocation()
        }
    }

    fun centerOnDeviceLocation() {
        viewModelScope.launch {
            isLocating.value = true
            val result = locationRepository.refreshCurrentLocation()
            isLocating.value = false
            result.onSuccess { coords ->
                val label = if (uiState.value.isArabic) "موقعك الحالي" else "Current Location"
                activeLocation.value = LocationContext(
                    label = label,
                    coordinates = coords,
                    isCurrentLocation = true
                )
                queryCenter.value = coords
                showSearchThisArea.value = false
                issueCameraCommand(coords, zoom = 14.8f, animate = true)
            }
        }
    }

    fun dismissLocationBanner() {
        locationBannerDismissed.value = true
    }

    fun openAppSettings() {
        locationRepository.openAppSettings()
    }

    fun openLocationServicesSettings() {
        locationRepository.openLocationServicesSettings()
    }

    fun toggleSavePlace(placeId: String) {
        viewModelScope.launch {
            savedPlacesRepository.toggleSavePlace(placeId)
        }
    }

    private fun issueCameraCommand(target: GeoPoint, zoom: Float, animate: Boolean) {
        cameraCommand.value = CameraCommand(
            id = nextCommandId++,
            target = target,
            zoom = zoom,
            animate = animate
        )
    }
}

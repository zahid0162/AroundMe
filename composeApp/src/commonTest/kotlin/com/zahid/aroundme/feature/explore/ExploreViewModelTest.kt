package com.zahid.aroundme.feature.explore

import app.cash.turbine.test
import com.zahid.aroundme.data.repository.InMemorySavedPlacesRepository
import com.zahid.aroundme.data.repository.InMemoryUserPreferencesRepository
import com.zahid.aroundme.data.repository.SamplePlacesRepository
import com.zahid.aroundme.domain.model.GeoPoint
import com.zahid.aroundme.domain.model.LocationPermissionStatus
import com.zahid.aroundme.domain.model.MapCameraRegion
import com.zahid.aroundme.domain.model.PlaceCategory
import com.zahid.aroundme.domain.model.SortOption
import com.zahid.aroundme.domain.repository.LocationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class FakeExploreLocationRepository(
    initialStatus: LocationPermissionStatus = LocationPermissionStatus.DENIED
) : LocationRepository {
    private val _permissionStatus = MutableStateFlow(initialStatus)
    override val permissionStatus: StateFlow<LocationPermissionStatus> = _permissionStatus.asStateFlow()

    private val _currentLocation = MutableStateFlow<GeoPoint?>(null)
    override val currentLocation: StateFlow<GeoPoint?> = _currentLocation.asStateFlow()

    var devicePoint: GeoPoint = GeoPoint(24.7136, 46.6753)

    override fun refreshPermissionState() {}

    override suspend fun refreshCurrentLocation(): Result<GeoPoint> {
        return if (_permissionStatus.value.isGranted) {
            _currentLocation.value = devicePoint
            Result.success(devicePoint)
        } else {
            Result.failure(SecurityException("Permission denied"))
        }
    }

    override fun openAppSettings() {}
    override fun openLocationServicesSettings() {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class ExploreViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun selectingCategory_filtersNearbyPlaces() = runTest {
        val viewModel = ExploreViewModel(
            SamplePlacesRepository(),
            InMemorySavedPlacesRepository(),
            InMemoryUserPreferencesRepository(),
            FakeExploreLocationRepository()
        )

        viewModel.uiState.test {
            // Skip initial & intermediate loading emissions until loaded
            var state = awaitItem()
            while (state.places.isEmpty() || state.isLoadingPlaces) {
                state = awaitItem()
            }
            assertTrue(state.places.size > 1)

            viewModel.selectCategory(PlaceCategory.CAFES)
            var filtered = awaitItem()
            while (filtered.isLoadingPlaces) {
                filtered = awaitItem()
            }
            assertTrue(filtered.places.isNotEmpty())
            assertTrue(filtered.places.all { it.primaryCategory == PlaceCategory.CAFES })
        }
    }

    @Test
    fun togglingViewMode_preservesSelectedCategoryAndSort() = runTest {
        val viewModel = ExploreViewModel(
            SamplePlacesRepository(),
            InMemorySavedPlacesRepository(),
            InMemoryUserPreferencesRepository(),
            FakeExploreLocationRepository()
        )

        viewModel.uiState.test {
            var state = awaitItem()
            while (state.places.isEmpty() || state.isLoadingPlaces) {
                state = awaitItem()
            }

            viewModel.selectCategory(PlaceCategory.PHARMACIES)
            viewModel.selectSortOption(SortOption.HIGHEST_RATED)
            var afterFilters = awaitItem()
            while (afterFilters.isLoadingPlaces || afterFilters.filter.selectedCategory != PlaceCategory.PHARMACIES) {
                afterFilters = awaitItem()
            }

            viewModel.setViewMode(ExploreViewMode.LIST)
            val afterToggle = awaitItem()
            assertEquals(ExploreViewMode.LIST, afterToggle.viewMode)
            assertEquals(PlaceCategory.PHARMACIES, afterToggle.filter.selectedCategory)
            assertEquals(SortOption.HIGHEST_RATED, afterToggle.filter.sortOption)
        }
    }

    @Test
    fun significantCameraMove_showsSearchThisAreaAndHidesAfterSearch() = runTest {
        val viewModel = ExploreViewModel(
            SamplePlacesRepository(),
            InMemorySavedPlacesRepository(),
            InMemoryUserPreferencesRepository(),
            FakeExploreLocationRepository()
        )

        viewModel.uiState.test {
            var initial = awaitItem()
            while (initial.isLoadingPlaces || initial.places.isEmpty()) {
                initial = awaitItem()
            }
            assertFalse(initial.showSearchThisArea)

            viewModel.onCameraIdle(
                MapCameraRegion(
                    center = GeoPoint(24.6950, 46.6853),
                    zoom = 14.2f,
                    visibleRadiusMeters = 2000.0
                )
            )
            expectNoEvents()

            viewModel.onCameraIdle(
                MapCameraRegion(
                    center = GeoPoint(24.7150, 46.7050),
                    zoom = 14.0f,
                    visibleRadiusMeters = 2500.0
                )
            )
            val moved = awaitItem()
            assertTrue(moved.showSearchThisArea)

            viewModel.searchThisArea()
            var searched = awaitItem()
            while (searched.showSearchThisArea) {
                searched = awaitItem()
            }
            assertFalse(searched.showSearchThisArea)
        }
    }

    @Test
    fun markerSelection_expandsCollapsedSheetAndSyncsId() = runTest {
        val viewModel = ExploreViewModel(
            SamplePlacesRepository(),
            InMemorySavedPlacesRepository(),
            InMemoryUserPreferencesRepository(),
            FakeExploreLocationRepository()
        )

        viewModel.uiState.test {
            var ready = awaitItem()
            while (ready.isLoadingPlaces || ready.places.isEmpty()) {
                ready = awaitItem()
            }

            viewModel.setSheetDetent(SheetDetent.COLLAPSED)
            val collapsed = awaitItem()
            assertEquals(SheetDetent.COLLAPSED, collapsed.sheetDetent)

            viewModel.onMarkerSelected("sample_cafe_1")
            val selected = awaitItem()
            assertEquals("sample_cafe_1", selected.selectedPlaceId)
            assertEquals(SheetDetent.HALF_EXPANDED, selected.sheetDetent)
        }
    }
}

package com.zahid.aroundme.feature.saved

import app.cash.turbine.test
import com.zahid.aroundme.data.repository.InMemorySavedPlacesRepository
import com.zahid.aroundme.data.repository.InMemoryUserPreferencesRepository
import com.zahid.aroundme.data.repository.SamplePlacesRepository
import com.zahid.aroundme.domain.model.GeoPoint
import com.zahid.aroundme.domain.model.LocationPermissionStatus
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

private class FakeSavedLocationRepository : LocationRepository {
    private val _permissionStatus = MutableStateFlow(LocationPermissionStatus.DENIED)
    override val permissionStatus: StateFlow<LocationPermissionStatus> = _permissionStatus.asStateFlow()

    private val _currentLocation = MutableStateFlow<GeoPoint?>(null)
    override val currentLocation: StateFlow<GeoPoint?> = _currentLocation.asStateFlow()

    override fun refreshPermissionState() {}
    override suspend fun refreshCurrentLocation(): Result<GeoPoint> = Result.failure(Exception("denied"))
    override fun openAppSettings() {}
    override fun openLocationServicesSettings() {}
}

@OptIn(ExperimentalCoroutinesApi::class)
class SavedViewModelTest {
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
    fun toggleSavePlace_addsAndRemovesPlaceFromHydratedList() = runTest {
        val viewModel = SavedViewModel(
            SamplePlacesRepository(),
            InMemorySavedPlacesRepository(),
            InMemoryUserPreferencesRepository(),
            FakeSavedLocationRepository()
        )

        viewModel.uiState.test {
            var initial = awaitItem()
            while (initial.hydratedPlaces.isEmpty() || initial.isLoading) {
                initial = awaitItem()
            }
            assertFalse(initial.hydratedPlaces.any { it.id == "sample_bakery_1" })

            viewModel.toggleSavePlace("sample_bakery_1")
            var added = awaitItem()
            while (added.isLoading || !added.hydratedPlaces.any { it.id == "sample_bakery_1" }) {
                added = awaitItem()
            }
            assertTrue(added.hydratedPlaces.any { it.id == "sample_bakery_1" })

            viewModel.toggleSavePlace("sample_bakery_1")
            var removed = awaitItem()
            while (removed.isLoading || removed.hydratedPlaces.any { it.id == "sample_bakery_1" }) {
                removed = awaitItem()
            }
            assertFalse(removed.hydratedPlaces.any { it.id == "sample_bakery_1" })
        }
    }

    @Test
    fun createAndDeleteCustomCollection_updatesCollectionsList() = runTest {
        val viewModel = SavedViewModel(
            SamplePlacesRepository(),
            InMemorySavedPlacesRepository(),
            InMemoryUserPreferencesRepository(),
            FakeSavedLocationRepository()
        )

        viewModel.uiState.test {
            var initial = awaitItem()
            while (initial.collections.isEmpty()) {
                initial = awaitItem()
            }
            val startCount = initial.collections.size

            viewModel.createCollection("Night Spots")
            val createdState = awaitItem()
            assertEquals(startCount + 1, createdState.collections.size)
            val customCol = createdState.collections.first { it.name == "Night Spots" }

            viewModel.deleteCollection(customCol.id)
            val deletedState = awaitItem()
            assertEquals(startCount, deletedState.collections.size)
        }
    }
}

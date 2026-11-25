package com.zahid.aroundme.feature.search

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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

private class FakeSearchLocationRepository : LocationRepository {
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
class SearchViewModelTest {
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
    fun submitQuery_returnsMatchingPlacesAndRecordsRecentSearch() = runTest {
        val viewModel = SearchViewModel(
            SamplePlacesRepository(),
            InMemorySavedPlacesRepository(),
            InMemoryUserPreferencesRepository(),
            FakeSearchLocationRepository()
        )

        viewModel.uiState.test {
            skipItems(1)
            viewModel.submitQuery("Qahwa")
            var state = awaitItem()
            while (state.isSearching || !state.hasSubmittedSearch) {
                state = awaitItem()
            }
            assertTrue(state.results.any { it.id == "sample_cafe_1" })
            assertTrue(state.recentSearches.first() == "Qahwa")
        }
    }

    @Test
    fun typingQuery_populatesAutocompleteSuggestionsAndRotatesSessionTokenOnSelect() = runTest {
        val viewModel = SearchViewModel(
            SamplePlacesRepository(),
            InMemorySavedPlacesRepository(),
            InMemoryUserPreferencesRepository(),
            FakeSearchLocationRepository()
        )

        viewModel.uiState.test {
            skipItems(1)
            viewModel.onQueryChanged("Pharmacy")
            advanceTimeBy(350)
            var state = awaitItem()
            while (state.suggestions.isEmpty() || state.isLoadingSuggestions) {
                state = awaitItem()
            }
            assertTrue(state.suggestions.isNotEmpty())

            val token1 = viewModel.consumeSessionTokenForPlaceSelection()
            val token2 = viewModel.consumeSessionTokenForPlaceSelection()
            assertNotEquals(token1, token2)
        }
    }
}

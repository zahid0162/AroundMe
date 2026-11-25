package com.zahid.aroundme.feature.details

import app.cash.turbine.test
import com.zahid.aroundme.core.platform.PlatformActions
import com.zahid.aroundme.data.repository.InMemorySavedPlacesRepository
import com.zahid.aroundme.data.repository.InMemoryUserPreferencesRepository
import com.zahid.aroundme.data.repository.SamplePlacesRepository
import com.zahid.aroundme.domain.model.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private class FakePlatformActions : PlatformActions {
    var directionsCalls = 0
    var dialCalls = 0
    var websiteCalls = 0
    var shareCalls = 0

    override fun openDirections(placeName: String, location: GeoPoint, placeId: String, directionsUri: String?) {
        directionsCalls++
    }

    override fun dialPhone(phoneNumber: String) {
        dialCalls++
    }

    override fun openWebsite(url: String) {
        websiteCalls++
    }

    override fun sharePlace(placeName: String, address: String, googleMapsUri: String?) {
        shareCalls++
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PlaceDetailsViewModelTest {
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
    fun loadsPlaceDetailsSavesPersonalNoteAndInvokesPlatformActions() = runTest {
        val actions = FakePlatformActions()
        val viewModel = PlaceDetailsViewModel(
            placeId = "sample_cafe_1",
            sessionToken = null,
            placesRepository = SamplePlacesRepository(),
            savedPlacesRepository = InMemorySavedPlacesRepository(),
            userPreferencesRepository = InMemoryUserPreferencesRepository(),
            platformActions = actions
        )

        viewModel.uiState.test {
            var loaded = awaitItem()
            while (loaded.isLoading || loaded.place == null) {
                loaded = awaitItem()
            }
            assertNotNull(loaded.place)
            assertEquals("Qahwa Roastery & Bakehouse", loaded.place?.name)

            viewModel.updateNote("Try the pour-over bar")
            val noted = awaitItem()
            assertEquals("Try the pour-over bar", noted.userNote)
            assertTrue(noted.isSaved)

            viewModel.openDirections()
            viewModel.dialPhone()
            viewModel.openWebsite()
            viewModel.sharePlace()
            assertEquals(1, actions.directionsCalls)
            assertEquals(1, actions.dialCalls)
            assertEquals(1, actions.websiteCalls)
            assertEquals(1, actions.shareCalls)
        }
    }
}

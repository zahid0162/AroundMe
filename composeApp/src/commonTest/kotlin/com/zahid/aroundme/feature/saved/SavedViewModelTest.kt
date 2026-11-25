package com.zahid.aroundme.feature.saved

import app.cash.turbine.test
import com.zahid.aroundme.data.repository.InMemorySavedPlacesRepository
import com.zahid.aroundme.data.repository.InMemoryUserPreferencesRepository
import com.zahid.aroundme.data.repository.SamplePlacesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
            InMemoryUserPreferencesRepository()
        )

        viewModel.uiState.test {
            skipItems(1)
            val initial = awaitItem()
            assertFalse(initial.hydratedPlaces.any { it.id == "sample_bakery_1" })

            viewModel.toggleSavePlace("sample_bakery_1")
            val added = awaitItem()
            assertTrue(added.hydratedPlaces.any { it.id == "sample_bakery_1" })

            viewModel.toggleSavePlace("sample_bakery_1")
            val removed = awaitItem()
            assertFalse(removed.hydratedPlaces.any { it.id == "sample_bakery_1" })
        }
    }
}

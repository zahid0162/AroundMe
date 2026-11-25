package com.zahid.aroundme.feature.explore

import app.cash.turbine.test
import com.zahid.aroundme.data.repository.InMemorySavedPlacesRepository
import com.zahid.aroundme.data.repository.InMemoryUserPreferencesRepository
import com.zahid.aroundme.data.repository.SamplePlacesRepository
import com.zahid.aroundme.domain.model.PlaceCategory
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
import kotlin.test.assertTrue

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
            InMemoryUserPreferencesRepository()
        )

        viewModel.uiState.test {
            skipItems(1)
            val initial = awaitItem()
            assertTrue(initial.places.size > 1)

            viewModel.selectCategory(PlaceCategory.CAFES)
            val filtered = awaitItem()
            assertTrue(filtered.places.isNotEmpty())
            assertTrue(filtered.places.all { it.primaryCategory == PlaceCategory.CAFES })
        }
    }

    @Test
    fun togglingViewMode_preservesSelectedCategory() = runTest {
        val viewModel = ExploreViewModel(
            SamplePlacesRepository(),
            InMemorySavedPlacesRepository(),
            InMemoryUserPreferencesRepository()
        )

        viewModel.uiState.test {
            skipItems(1)
            viewModel.selectCategory(PlaceCategory.PHARMACIES)
            val afterCategory = awaitItem()
            assertEquals(PlaceCategory.PHARMACIES, afterCategory.filter.selectedCategory)

            viewModel.setViewMode(ExploreViewMode.LIST)
            val afterToggle = awaitItem()
            assertEquals(ExploreViewMode.LIST, afterToggle.viewMode)
            assertEquals(PlaceCategory.PHARMACIES, afterToggle.filter.selectedCategory)
        }
    }
}

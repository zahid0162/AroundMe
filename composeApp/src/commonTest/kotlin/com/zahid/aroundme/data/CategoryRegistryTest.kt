package com.zahid.aroundme.data

import com.zahid.aroundme.data.remote.CategoryRegistry
import com.zahid.aroundme.domain.model.PlaceCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CategoryRegistryTest {

    @Test
    fun all28PlaceCategoriesHaveIncludedTypesAndKeyword() {
        assertEquals(28, PlaceCategory.entries.size)
        PlaceCategory.entries.forEach { category ->
            val mapping = CategoryRegistry.mappingFor(category)
            assertTrue(mapping.includedTypes.isNotEmpty(), "Missing includedTypes for $category")
            assertTrue(mapping.searchKeyword.isNotBlank(), "Missing searchKeyword for $category")
        }
    }

    @Test
    fun categoryForGoogleTypesMapsPrimaryAndFallbackTypes() {
        assertEquals(
            PlaceCategory.CAFES,
            CategoryRegistry.categoryForGoogleTypes("coffee_shop", listOf("point_of_interest"))
        )
        assertEquals(
            PlaceCategory.PHARMACIES,
            CategoryRegistry.categoryForGoogleTypes(null, listOf("store", "pharmacy"))
        )
        assertEquals(
            PlaceCategory.MOSQUES,
            CategoryRegistry.categoryForGoogleTypes("mosque", emptyList())
        )
    }
}

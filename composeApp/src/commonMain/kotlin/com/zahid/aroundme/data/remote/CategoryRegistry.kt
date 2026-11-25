package com.zahid.aroundme.data.remote

import com.zahid.aroundme.domain.model.PlaceCategory

data class CategoryMapping(
    val category: PlaceCategory,
    val includedTypes: List<String>,
    val searchKeyword: String
)

object CategoryRegistry {
    private val mappings: Map<PlaceCategory, CategoryMapping> = mapOf(
        PlaceCategory.RESTAURANTS to CategoryMapping(PlaceCategory.RESTAURANTS, listOf("restaurant"), "restaurants"),
        PlaceCategory.CAFES to CategoryMapping(PlaceCategory.CAFES, listOf("cafe", "coffee_shop"), "cafes"),
        PlaceCategory.BAKERIES to CategoryMapping(PlaceCategory.BAKERIES, listOf("bakery"), "bakeries"),
        PlaceCategory.FAST_FOOD to CategoryMapping(PlaceCategory.FAST_FOOD, listOf("fast_food_restaurant"), "fast food"),
        PlaceCategory.ICE_CREAM to CategoryMapping(PlaceCategory.ICE_CREAM, listOf("ice_cream_shop"), "ice cream"),

        PlaceCategory.SHOPPING_MALLS to CategoryMapping(PlaceCategory.SHOPPING_MALLS, listOf("shopping_mall"), "shopping mall"),
        PlaceCategory.SUPERMARKETS to CategoryMapping(PlaceCategory.SUPERMARKETS, listOf("supermarket"), "supermarket"),
        PlaceCategory.GROCERY_STORES to CategoryMapping(PlaceCategory.GROCERY_STORES, listOf("grocery_store"), "grocery store"),
        PlaceCategory.CLOTHING_STORES to CategoryMapping(PlaceCategory.CLOTHING_STORES, listOf("clothing_store"), "clothing store"),

        PlaceCategory.HOSPITALS to CategoryMapping(PlaceCategory.HOSPITALS, listOf("hospital"), "hospital"),
        PlaceCategory.PHARMACIES to CategoryMapping(PlaceCategory.PHARMACIES, listOf("pharmacy"), "pharmacy"),
        PlaceCategory.CLINICS to CategoryMapping(PlaceCategory.CLINICS, listOf("medical_clinic", "doctor"), "clinic"),
        PlaceCategory.DENTISTS to CategoryMapping(PlaceCategory.DENTISTS, listOf("dentist"), "dentist"),

        PlaceCategory.HOTELS to CategoryMapping(PlaceCategory.HOTELS, listOf("hotel", "lodging"), "hotel"),
        PlaceCategory.TOURIST_ATTRACTIONS to CategoryMapping(PlaceCategory.TOURIST_ATTRACTIONS, listOf("tourist_attraction"), "tourist attractions"),
        PlaceCategory.MUSEUMS to CategoryMapping(PlaceCategory.MUSEUMS, listOf("museum"), "museum"),
        PlaceCategory.PARKS to CategoryMapping(PlaceCategory.PARKS, listOf("park"), "park"),

        PlaceCategory.ATMS to CategoryMapping(PlaceCategory.ATMS, listOf("atm"), "atm"),
        PlaceCategory.BANKS to CategoryMapping(PlaceCategory.BANKS, listOf("bank"), "bank"),
        PlaceCategory.PETROL_STATIONS to CategoryMapping(PlaceCategory.PETROL_STATIONS, listOf("gas_station"), "gas station"),
        PlaceCategory.CAR_WASHES to CategoryMapping(PlaceCategory.CAR_WASHES, listOf("car_wash"), "car wash"),
        PlaceCategory.CAR_REPAIR to CategoryMapping(PlaceCategory.CAR_REPAIR, listOf("car_repair"), "car repair"),
        PlaceCategory.LAUNDRY to CategoryMapping(PlaceCategory.LAUNDRY, listOf("laundry"), "laundry"),
        PlaceCategory.BEAUTY_SALONS to CategoryMapping(PlaceCategory.BEAUTY_SALONS, listOf("beauty_salon", "hair_salon"), "beauty salon"),

        PlaceCategory.GYMS to CategoryMapping(PlaceCategory.GYMS, listOf("gym", "fitness_center"), "gym"),
        PlaceCategory.SPORTS_FACILITIES to CategoryMapping(PlaceCategory.SPORTS_FACILITIES, listOf("sports_complex", "stadium"), "sports facility"),
        PlaceCategory.MOSQUES to CategoryMapping(PlaceCategory.MOSQUES, listOf("mosque"), "mosque"),
        PlaceCategory.ENTERTAINMENT to CategoryMapping(PlaceCategory.ENTERTAINMENT, listOf("movie_theater", "amusement_center"), "entertainment")
    )

    val defaultExploreIncludedTypes: List<String> = listOf(
        "restaurant",
        "cafe",
        "pharmacy",
        "park",
        "tourist_attraction",
        "supermarket",
        "museum",
        "mosque"
    )

    fun mappingFor(category: PlaceCategory): CategoryMapping =
        mappings[category] ?: error("Missing CategoryMapping for $category")

    fun includedTypesFor(category: PlaceCategory?): List<String> =
        if (category == null) defaultExploreIncludedTypes else mappingFor(category).includedTypes

    fun categoryForGoogleTypes(primaryType: String?, types: List<String>): PlaceCategory {
        val candidate = primaryType?.lowercase()
        if (candidate != null) {
            mappings.values.firstOrNull { mapping ->
                mapping.includedTypes.any { candidate == it || candidate.endsWith("_$it") }
            }?.let { return it.category }
        }
        for (type in types) {
            val normalized = type.lowercase()
            mappings.values.firstOrNull { mapping ->
                mapping.includedTypes.any { normalized == it || normalized.endsWith("_$it") }
            }?.let { return it.category }
        }
        return PlaceCategory.TOURIST_ATTRACTIONS
    }
}

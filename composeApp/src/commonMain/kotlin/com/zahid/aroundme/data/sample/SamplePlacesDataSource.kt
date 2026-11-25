package com.zahid.aroundme.data.sample

import com.zahid.aroundme.domain.model.GeoPoint
import com.zahid.aroundme.domain.model.LocationContext
import com.zahid.aroundme.domain.model.Place
import com.zahid.aroundme.domain.model.PlaceCategory
import com.zahid.aroundme.domain.model.PriceLevel
import com.zahid.aroundme.domain.model.SavedCollection

/**
 * PHASE 1 PROTOTYPE FIXTURE ONLY.
 * Strictly isolated inside data/sample and replaced by Ktor Places API (New) in Phase 3
 * and Room 3 persistence in Phase 5.
 */
object SamplePlacesDataSource {
    val locations: List<LocationContext> = listOf(
        LocationContext(
            label = "Olaya District, Riyadh",
            coordinates = GeoPoint(24.6949, 46.6852),
            isCurrentLocation = true
        ),
        LocationContext(
            label = "Downtown Dubai",
            coordinates = GeoPoint(25.1972, 55.2744),
            isCurrentLocation = false
        ),
        LocationContext(
            label = "Covent Garden, London",
            coordinates = GeoPoint(51.5117, -0.1240),
            isCurrentLocation = false
        )
    )

    val places: List<Place> = listOf(
        Place(
            id = "sample_cafe_1",
            name = "Qahwa Roastery & Bakehouse",
            primaryCategory = PlaceCategory.CAFES,
            primaryCategoryLabel = "Specialty Cafe",
            address = "Tahlia St, Al Olaya, Riyadh 12222",
            location = GeoPoint(24.6968, 46.6879),
            rating = 4.8,
            reviewCount = 642,
            isOpenNow = true,
            priceLevel = PriceLevel.MODERATE,
            phoneNumber = "+966 11 462 8190",
            websiteUrl = "https://example.com/qahwa",
            openingHoursSummary = "Open · Closes 11:30 PM",
            attributes = listOf("Outdoor seating", "Wi-Fi", "Pour-over bar"),
            editorialSummary = "Sunlit neighborhood espresso bar roasting single-origin beans in-house."
        ),
        Place(
            id = "sample_rest_1",
            name = "Mizan Levantine Kitchen",
            primaryCategory = PlaceCategory.RESTAURANTS,
            primaryCategoryLabel = "Mediterranean Restaurant",
            address = "Prince Sultan Bin Abdulaziz Rd, Riyadh",
            location = GeoPoint(24.6915, 46.6814),
            rating = 4.7,
            reviewCount = 1120,
            isOpenNow = true,
            priceLevel = PriceLevel.EXPENSIVE,
            phoneNumber = "+966 11 488 2210",
            websiteUrl = "https://example.com/mizan",
            openingHoursSummary = "Open · Closes 12:30 AM",
            attributes = listOf("Family friendly", "Reservations", "Valet parking"),
            editorialSummary = "Wood-fired Levantine sharing plates in a limestone courtyard dining room."
        ),
        Place(
            id = "sample_park_1",
            name = "Al Olaya Central Promenade",
            primaryCategory = PlaceCategory.PARKS,
            primaryCategoryLabel = "Urban Park",
            address = "King Fahd Rd Service Lane, Riyadh",
            location = GeoPoint(24.6989, 46.6831),
            rating = 4.6,
            reviewCount = 890,
            isOpenNow = true,
            priceLevel = null,
            openingHoursSummary = "Open 24 hours",
            attributes = listOf("Walking trail", "Shaded seating", "Accessible"),
            editorialSummary = "Landscaped pedestrian promenade lined with palm trees and evening lighting."
        ),
        Place(
            id = "sample_pharm_1",
            name = "Al-Shifa 24h Pharmacy",
            primaryCategory = PlaceCategory.PHARMACIES,
            primaryCategoryLabel = "Pharmacy",
            address = "Al Olaya Main St, Bldg 44, Riyadh",
            location = GeoPoint(24.6933, 46.6896),
            rating = 4.5,
            reviewCount = 215,
            isOpenNow = true,
            priceLevel = PriceLevel.INEXPENSIVE,
            phoneNumber = "+966 11 419 5502",
            openingHoursSummary = "Open 24 hours",
            attributes = listOf("24/7 pharmacist", "Wheelchair accessible")
        ),
        Place(
            id = "sample_museum_1",
            name = "Dar Al-Funoon Heritage Gallery",
            primaryCategory = PlaceCategory.MUSEUMS,
            primaryCategoryLabel = "Art Museum",
            address = "Al Murabba Cultural Quarter, Riyadh",
            location = GeoPoint(24.6820, 46.6970),
            rating = 4.9,
            reviewCount = 430,
            isOpenNow = false,
            priceLevel = PriceLevel.MODERATE,
            websiteUrl = "https://example.com/daralfunoon",
            openingHoursSummary = "Closed · Opens 9:00 AM",
            attributes = listOf("Guided tours", "Gift shop", "Courtyard cafe")
        ),
        Place(
            id = "sample_bakery_1",
            name = "Saffron & Crumb Artisan Bakery",
            primaryCategory = PlaceCategory.BAKERIES,
            primaryCategoryLabel = "Bakery",
            address = "Musa Ibn Nusayr St, Al Olaya, Riyadh",
            location = GeoPoint(24.7002, 46.6911),
            rating = 4.7,
            reviewCount = 389,
            isOpenNow = true,
            priceLevel = PriceLevel.MODERATE,
            openingHoursSummary = "Open · Closes 10:00 PM",
            attributes = listOf("Takeaway", "Fresh sourdough")
        ),
        Place(
            id = "sample_super_1",
            name = "Tamimi Fresh Market",
            primaryCategory = PlaceCategory.SUPERMARKETS,
            primaryCategoryLabel = "Supermarket",
            address = "King Abdulaziz Rd, Riyadh",
            location = GeoPoint(24.7040, 46.6790),
            rating = 4.4,
            reviewCount = 1540,
            isOpenNow = true,
            priceLevel = PriceLevel.MODERATE,
            openingHoursSummary = "Open · Closes 1:00 AM",
            attributes = listOf("Parking lot", "Organic produce", "Bakery counter")
        ),
        Place(
            id = "sample_mosque_1",
            name = "Al-Rajhi Grand Mosque",
            primaryCategory = PlaceCategory.MOSQUES,
            primaryCategoryLabel = "Mosque",
            address = "Al Olaya East Sector, Riyadh",
            location = GeoPoint(24.6904, 46.6934),
            rating = 4.9,
            reviewCount = 2100,
            isOpenNow = true,
            openingHoursSummary = "Open for all prayers",
            attributes = listOf("Accessible entrance", "Ablution area", "Parking")
        )
    )

    val defaultCollections: List<SavedCollection> = listOf(
        SavedCollection("want_to_visit", "Want to Visit", "أماكن أود زيارتها", listOf("sample_museum_1")),
        SavedCollection("favorite_restaurants", "Favorite Restaurants", "مطاعمي المفضلة", listOf("sample_rest_1", "sample_cafe_1")),
        SavedCollection("weekend_plans", "Weekend Plans", "خطط نهاية الأسبوع", listOf("sample_park_1")),
        SavedCollection("travel", "Travel", "السفر", emptyList())
    )
}

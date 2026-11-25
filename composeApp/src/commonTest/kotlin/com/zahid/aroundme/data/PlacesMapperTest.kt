package com.zahid.aroundme.data

import com.zahid.aroundme.data.remote.AuthorAttributionDto
import com.zahid.aroundme.data.remote.AutocompleteResponseDto
import com.zahid.aroundme.data.remote.AutocompleteSuggestionItemDto
import com.zahid.aroundme.data.remote.AutocompleteTextDto
import com.zahid.aroundme.data.remote.GoogleMapsLinksDto
import com.zahid.aroundme.data.remote.LatLngDto
import com.zahid.aroundme.data.remote.LocalizedTextDto
import com.zahid.aroundme.data.remote.OpeningHoursDto
import com.zahid.aroundme.data.remote.PhotoDto
import com.zahid.aroundme.data.remote.PlaceDto
import com.zahid.aroundme.data.remote.PlacePredictionDto
import com.zahid.aroundme.data.remote.PlacesMapper
import com.zahid.aroundme.data.remote.QueryPredictionDto
import com.zahid.aroundme.data.remote.ReviewDto
import com.zahid.aroundme.data.remote.StructuredFormatDto
import com.zahid.aroundme.domain.model.AutocompleteSuggestion
import com.zahid.aroundme.domain.model.PlaceCategory
import com.zahid.aroundme.domain.model.PriceLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlacesMapperTest {

    @Test
    fun toDomainPlace_preservesMissingFieldsAsNullWithoutFabrication() {
        val dto = PlaceDto(
            id = "ChIJ123",
            displayName = LocalizedTextDto("Minimal Spot"),
            primaryType = "cafe",
            location = LatLngDto(24.70, 46.68)
        )

        val place = PlacesMapper.toDomainPlace(dto, isArabic = false)
        assertNotNull(place)
        assertEquals("ChIJ123", place.id)
        assertEquals("Minimal Spot", place.name)
        assertEquals(PlaceCategory.CAFES, place.primaryCategory)
        assertNull(place.rating)
        assertNull(place.reviewCount)
        assertNull(place.isOpenNow)
        assertNull(place.priceLevel)
        assertNull(place.phoneNumber)
        assertNull(place.websiteUrl)
        assertTrue(place.reviews.isEmpty())
    }

    @Test
    fun toDomainPlace_mapsRichDetailsPhotosReviewsAndAttributions() {
        val dto = PlaceDto(
            id = "ChIJ456",
            displayName = LocalizedTextDto("Qahwa House"),
            primaryType = "coffee_shop",
            primaryTypeDisplayName = LocalizedTextDto("Coffee shop"),
            shortFormattedAddress = "Tahlia St, Riyadh",
            location = LatLngDto(24.6968, 46.6879),
            rating = 4.8,
            userRatingCount = 640,
            priceLevel = "PRICE_LEVEL_MODERATE",
            currentOpeningHours = OpeningHoursDto(
                openNow = true,
                weekdayDescriptions = listOf("Monday: 7:00 AM – 11:00 PM")
            ),
            internationalPhoneNumber = "+966 11 462 8190",
            websiteUri = "https://example.com",
            googleMapsLinks = GoogleMapsLinksDto(
                directionsUri = "https://maps.google.com/?daddr=24.6968,46.6879"
            ),
            photos = listOf(
                PhotoDto(
                    name = "places/ChIJ456/photos/photo1",
                    widthPx = 1200,
                    heightPx = 800,
                    authorAttributions = listOf(
                        AuthorAttributionDto(
                            displayName = "Alex M.",
                            uri = "//maps.google.com/maps/contrib/123"
                        )
                    )
                )
            ),
            reviews = listOf(
                ReviewDto(
                    relativePublishTimeDescription = "2 weeks ago",
                    rating = 5.0,
                    text = LocalizedTextDto("Excellent espresso."),
                    authorAttribution = AuthorAttributionDto(displayName = "Nadia K.")
                )
            ),
            outdoorSeating = true
        )

        val place = PlacesMapper.toDomainPlace(dto, isArabic = false)
        assertNotNull(place)
        assertEquals(PriceLevel.MODERATE, place.priceLevel)
        assertEquals(true, place.isOpenNow)
        assertEquals("https://maps.google.com/?daddr=24.6968,46.6879", place.directionsUri)
        assertEquals(1, place.photos.size)
        assertEquals("Alex M.", place.photos.first().attributionName)
        assertEquals("https://maps.google.com/maps/contrib/123", place.photos.first().attributionUri)
        assertEquals(1, place.reviews.size)
        assertEquals("Nadia K.", place.reviews.first().authorName)
        assertTrue(place.attributes.contains("Outdoor seating"))
    }

    @Test
    fun toDomainSuggestions_ignoresZeroOrNegativeDistanceMeters() {
        val response = AutocompleteResponseDto(
            suggestions = listOf(
                AutocompleteSuggestionItemDto(
                    placePrediction = PlacePredictionDto(
                        placeId = "p1",
                        structuredFormat = StructuredFormatDto(
                            mainText = AutocompleteTextDto("Cafe One"),
                            secondaryText = AutocompleteTextDto("Riyadh")
                        ),
                        distanceMeters = 0
                    )
                ),
                AutocompleteSuggestionItemDto(
                    placePrediction = PlacePredictionDto(
                        placeId = "p2",
                        structuredFormat = StructuredFormatDto(
                            mainText = AutocompleteTextDto("Cafe Two"),
                            secondaryText = AutocompleteTextDto("Riyadh")
                        ),
                        distanceMeters = 450
                    )
                ),
                AutocompleteSuggestionItemDto(
                    queryPrediction = QueryPredictionDto(
                        text = AutocompleteTextDto("Coffee near Olaya")
                    )
                )
            )
        )

        val mapped = PlacesMapper.toDomainSuggestions(response)
        assertEquals(3, mapped.size)
        val first = mapped[0] as AutocompleteSuggestion.PlacePrediction
        val second = mapped[1] as AutocompleteSuggestion.PlacePrediction
        val third = mapped[2] as AutocompleteSuggestion.QueryPrediction
        assertNull(first.distanceMeters)
        assertEquals(450, second.distanceMeters)
        assertEquals("Coffee near Olaya", third.queryText)
    }
}

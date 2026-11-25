package com.zahid.aroundme.data.remote

import com.zahid.aroundme.domain.model.AutocompleteSuggestion
import com.zahid.aroundme.domain.model.GeoPoint
import com.zahid.aroundme.domain.model.Place
import com.zahid.aroundme.domain.model.PlacePhoto
import com.zahid.aroundme.domain.model.PlaceReview
import com.zahid.aroundme.domain.model.PriceLevel

object PlacesMapper {

    fun toDomainPlace(dto: PlaceDto, isArabic: Boolean = false): Place? {
        val resolvedId = dto.id.ifBlank {
            dto.name?.removePrefix("places/").orEmpty()
        }
        val coords = dto.location ?: return null
        val displayName = dto.displayName?.text?.trim().orEmpty()
        if (resolvedId.isBlank() || displayName.isBlank()) return null

        val category = CategoryRegistry.categoryForGoogleTypes(dto.primaryType, dto.types)
        val categoryLabel = dto.primaryTypeDisplayName?.text?.takeIf { it.isNotBlank() }
            ?: category.label(isArabic)

        val address = dto.shortFormattedAddress?.takeIf { it.isNotBlank() }
            ?: dto.formattedAddress?.takeIf { it.isNotBlank() }
            ?: ""

        val hoursSource = dto.currentOpeningHours ?: dto.regularOpeningHours
        val isOpenNow = dto.currentOpeningHours?.openNow ?: dto.regularOpeningHours?.openNow
        val weekdayDescriptions = hoursSource?.weekdayDescriptions.orEmpty()
        val hoursSummary = when {
            isOpenNow == true -> if (isArabic) "مفتوح الآن" else "Open now"
            isOpenNow == false -> if (isArabic) "مغلق حالياً" else "Closed"
            else -> weekdayDescriptions.firstOrNull()
        }

        val attributes = buildList {
            if (dto.dineIn == true) add(if (isArabic) "جلوس داخلي" else "Dine-in")
            if (dto.takeout == true) add(if (isArabic) "طلب خارجي" else "Takeout")
            if (dto.delivery == true) add(if (isArabic) "توصيل" else "Delivery")
            if (dto.outdoorSeating == true) add(if (isArabic) "جلسات خارجية" else "Outdoor seating")
            if (dto.reservable == true) add(if (isArabic) "يقبل الحجز" else "Reservations")
            if (dto.goodForChildren == true) add(if (isArabic) "مناسب للعائلات" else "Family friendly")
            if (dto.accessibilityOptions?.wheelchairAccessibleEntrance == true) {
                add(if (isArabic) "مدخل مهيأ للكراسي" else "Wheelchair accessible")
            }
            if (dto.parkingOptions?.valetParking == true) {
                add(if (isArabic) "خدمة صف السيارات" else "Valet parking")
            } else if (
                dto.parkingOptions?.freeParkingLot == true ||
                dto.parkingOptions?.paidParkingLot == true ||
                dto.parkingOptions?.freeGarageParking == true
            ) {
                add(if (isArabic) "مواقف سيارات" else "Parking available")
            }
        }

        val photos = dto.photos.mapNotNull { photo ->
            if (photo.name.isBlank()) return@mapNotNull null
            val attr = photo.authorAttributions.firstOrNull()
            val normalizedUri = attr?.uri?.let { raw ->
                if (raw.startsWith("//")) "https:$raw" else raw
            }
            PlacePhoto(
                resourceName = photo.name,
                widthPx = photo.widthPx,
                heightPx = photo.heightPx,
                attributionName = attr?.displayName?.takeIf { it.isNotBlank() },
                attributionUri = normalizedUri
            )
        }

        val reviews = dto.reviews.mapNotNull { rev ->
            val author = rev.authorAttribution?.displayName?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val body = rev.text?.text?.takeIf { it.isNotBlank() }
                ?: rev.originalText?.text?.takeIf { it.isNotBlank() }
            if (body == null && rev.rating == null) return@mapNotNull null
            PlaceReview(
                authorName = author,
                rating = rev.rating,
                relativePublishTime = rev.relativePublishTimeDescription,
                text = body
            )
        }

        return Place(
            id = resolvedId,
            name = displayName,
            primaryCategory = category,
            primaryCategoryLabel = categoryLabel,
            address = address,
            location = GeoPoint(coords.latitude, coords.longitude),
            rating = dto.rating,
            reviewCount = dto.userRatingCount,
            isOpenNow = isOpenNow,
            priceLevel = mapPriceLevel(dto.priceLevel),
            phoneNumber = dto.internationalPhoneNumber?.takeIf { it.isNotBlank() }
                ?: dto.nationalPhoneNumber?.takeIf { it.isNotBlank() },
            websiteUrl = dto.websiteUri?.takeIf { it.isNotBlank() },
            googleMapsUri = dto.googleMapsUri?.takeIf { it.isNotBlank() }
                ?: dto.googleMapsLinks?.placeUri?.takeIf { it.isNotBlank() },
            directionsUri = dto.googleMapsLinks?.directionsUri?.takeIf { it.isNotBlank() },
            openingHoursSummary = hoursSummary,
            weekdayHours = weekdayDescriptions,
            attributes = attributes,
            editorialSummary = dto.editorialSummary?.text?.takeIf { it.isNotBlank() },
            photos = photos,
            reviews = reviews,
            businessStatus = dto.businessStatus
        )
    }

    fun toDomainSuggestions(response: AutocompleteResponseDto): List<AutocompleteSuggestion> {
        return response.suggestions.mapNotNull { item ->
            val placePred = item.placePrediction
            if (placePred != null && placePred.placeId.isNotBlank()) {
                val main = placePred.structuredFormat?.mainText?.text?.takeIf { it.isNotBlank() }
                    ?: placePred.text?.text?.takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null
                val secondary = placePred.structuredFormat?.secondaryText?.text.orEmpty()
                // Per Places Autocomplete (New) docs: never display distance when omitted or <= 0
                val dist = placePred.distanceMeters?.takeIf { it > 0 }
                return@mapNotNull AutocompleteSuggestion.PlacePrediction(
                    placeId = placePred.placeId,
                    primaryText = main,
                    secondaryText = secondary,
                    distanceMeters = dist,
                    types = placePred.types
                )
            }
            val queryText = item.queryPrediction?.text?.text?.trim().orEmpty()
            if (queryText.isNotEmpty()) {
                return@mapNotNull AutocompleteSuggestion.QueryPrediction(queryText = queryText)
            }
            null
        }
    }

    private fun mapPriceLevel(raw: String?): PriceLevel? = when (raw) {
        "PRICE_LEVEL_INEXPENSIVE" -> PriceLevel.INEXPENSIVE
        "PRICE_LEVEL_MODERATE" -> PriceLevel.MODERATE
        "PRICE_LEVEL_EXPENSIVE" -> PriceLevel.EXPENSIVE
        "PRICE_LEVEL_VERY_EXPENSIVE" -> PriceLevel.VERY_EXPENSIVE
        else -> null
    }
}

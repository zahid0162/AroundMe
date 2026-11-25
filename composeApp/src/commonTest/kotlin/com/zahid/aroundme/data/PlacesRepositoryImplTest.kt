package com.zahid.aroundme.data

import com.zahid.aroundme.data.remote.AutocompleteResponseDto
import com.zahid.aroundme.data.remote.InMemoryRecentSearchStore
import com.zahid.aroundme.data.remote.LatLngDto
import com.zahid.aroundme.data.remote.LocalizedTextDto
import com.zahid.aroundme.data.remote.PlaceDto
import com.zahid.aroundme.data.remote.PlacesRemoteDataSource
import com.zahid.aroundme.data.remote.PlacesRepositoryImpl
import com.zahid.aroundme.data.remote.PlacesSearchResponseDto
import com.zahid.aroundme.domain.model.FilterState
import com.zahid.aroundme.domain.model.GeoPoint
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakePlacesRemoteDataSource(
    override val isConfigured: Boolean = true
) : PlacesRemoteDataSource {
    var nearbyCallCount = 0
    var detailsCallCount = 0
    var photoCallCount = 0
    var lastLanguageCode: String? = null
    var lastSessionToken: String? = null

    override suspend fun searchNearby(
        center: GeoPoint,
        radiusMeters: Double,
        includedTypes: List<String>,
        rankByDistance: Boolean,
        languageCode: String
    ): PlacesSearchResponseDto {
        nearbyCallCount++
        lastLanguageCode = languageCode
        return PlacesSearchResponseDto(
            places = listOf(
                PlaceDto(
                    id = "live_1",
                    displayName = LocalizedTextDto("Live Roastery"),
                    primaryType = "cafe",
                    location = LatLngDto(center.latitude, center.longitude),
                    rating = 4.7
                )
            )
        )
    }

    override suspend fun searchText(
        query: String,
        center: GeoPoint,
        radiusMeters: Double,
        includedType: String?,
        openNow: Boolean?,
        minRating: Double?,
        languageCode: String
    ): PlacesSearchResponseDto = PlacesSearchResponseDto()

    override suspend fun autocomplete(
        input: String,
        center: GeoPoint,
        sessionToken: String,
        languageCode: String
    ): AutocompleteResponseDto = AutocompleteResponseDto()

    override suspend fun getPlaceDetails(
        placeId: String,
        sessionToken: String?,
        languageCode: String
    ): PlaceDto {
        detailsCallCount++
        lastSessionToken = sessionToken
        lastLanguageCode = languageCode
        return PlaceDto(
            id = placeId,
            displayName = LocalizedTextDto("Detailed Place"),
            primaryType = "restaurant",
            location = LatLngDto(24.70, 46.68)
        )
    }

    override suspend fun fetchPhotoUri(
        photoResourceName: String,
        maxWidthPx: Int,
        maxHeightPx: Int
    ): String {
        photoCallCount++
        return "https://lh3.googleusercontent.com/places/sample"
    }
}

class PlacesRepositoryImplTest {

    @Test
    fun observeNearbyPlaces_cachesIdenticalQueriesWithinTtl() = runTest {
        val fakeRemote = FakePlacesRemoteDataSource()
        val repo = PlacesRepositoryImpl(fakeRemote, InMemoryRecentSearchStore())
        val center = GeoPoint(24.6949, 46.6852)

        val first = repo.observeNearbyPlaces(center, FilterState(), "ar").first()
        val second = repo.observeNearbyPlaces(center, FilterState(), "ar").first()

        assertTrue(first.isLiveApi)
        assertNull(first.errorMessage)
        assertEquals(1, first.places.size)
        assertEquals(1, second.places.size)
        assertEquals(1, fakeRemote.nearbyCallCount)
        assertEquals("ar", fakeRemote.lastLanguageCode)
    }

    @Test
    fun observeNearbyPlaces_deduplicatesConcurrentRequests() = runTest {
        val fakeRemote = FakePlacesRemoteDataSource()
        val repo = PlacesRepositoryImpl(fakeRemote, InMemoryRecentSearchStore())
        val center = GeoPoint(24.6949, 46.6852)

        val results = listOf(
            async { repo.observeNearbyPlaces(center, FilterState(), "en").first() },
            async { repo.observeNearbyPlaces(center, FilterState(), "en").first() }
        ).awaitAll()

        assertEquals(2, results.size)
        assertEquals(1, fakeRemote.nearbyCallCount)
    }

    @Test
    fun getPlaceById_forwardsSessionTokenAndCachesDetailsWhenNoSessionToken() = runTest {
        val fakeRemote = FakePlacesRemoteDataSource()
        val repo = PlacesRepositoryImpl(fakeRemote, InMemoryRecentSearchStore())

        val first = repo.getPlaceById("ChIJ_A", sessionToken = "tok_1", languageCode = "en")
        assertTrue(first.isSuccess)
        assertEquals("tok_1", fakeRemote.lastSessionToken)
        assertEquals(1, fakeRemote.detailsCallCount)

        // Subsequent lookup without session token reads cached full details
        val cached = repo.getPlaceById("ChIJ_A", sessionToken = null, languageCode = "en")
        assertTrue(cached.isSuccess)
        assertEquals(1, fakeRemote.detailsCallCount)
    }

    @Test
    fun resolvePhotoUri_cachesResolvedUriPerDimensions() = runTest {
        val fakeRemote = FakePlacesRemoteDataSource()
        val repo = PlacesRepositoryImpl(fakeRemote, InMemoryRecentSearchStore())

        val first = repo.resolvePhotoUri("places/1/photos/abc", 320, 320)
        val second = repo.resolvePhotoUri("places/1/photos/abc", 320, 320)

        assertEquals("https://lh3.googleusercontent.com/places/sample", first)
        assertEquals(first, second)
        assertEquals(1, fakeRemote.photoCallCount)
    }
}

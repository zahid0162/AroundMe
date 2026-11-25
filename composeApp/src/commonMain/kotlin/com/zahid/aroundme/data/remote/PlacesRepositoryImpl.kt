package com.zahid.aroundme.data.remote

import com.zahid.aroundme.core.geo.GeoUtils
import com.zahid.aroundme.data.sample.SamplePlacesDataSource
import com.zahid.aroundme.domain.model.AutocompleteSuggestion
import com.zahid.aroundme.domain.model.FilterState
import com.zahid.aroundme.domain.model.GeoPoint
import com.zahid.aroundme.domain.model.LocationContext
import com.zahid.aroundme.domain.model.Place
import com.zahid.aroundme.domain.model.PlacesFeedResult
import com.zahid.aroundme.domain.model.SortOption
import com.zahid.aroundme.domain.repository.PlacesRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.roundToInt
import kotlin.time.TimeSource

interface RecentSearchStore {
    fun getRecentSearches(): Flow<List<String>>
    suspend fun recordSearch(query: String)
    suspend fun clearRecentSearches()
}

class InMemoryRecentSearchStore : RecentSearchStore {
    private val recent = MutableStateFlow(
        listOf("Specialty coffee", "24h Pharmacy", "Parks", "Museums")
    )

    override fun getRecentSearches(): Flow<List<String>> = recent.asStateFlow()

    override suspend fun recordSearch(query: String) {
        val clean = query.trim()
        if (clean.isEmpty()) return
        recent.update { current ->
            (listOf(clean) + current.filterNot { it.equals(clean, ignoreCase = true) }).take(8)
        }
    }

    override suspend fun clearRecentSearches() {
        recent.value = emptyList()
    }
}

class PlacesRepositoryImpl(
    private val remote: PlacesRemoteDataSource,
    private val recentSearchStore: RecentSearchStore
) : PlacesRepository {

    override val isLiveApiConfigured: Boolean
        get() = remote.isConfigured

    private val timeSource = TimeSource.Monotonic
    private val cacheMutex = Mutex()

    private data class CachedEntry<T>(
        val value: T,
        val cachedAt: TimeSource.Monotonic.ValueTimeMark
    )

    private val nearbyCache = LinkedHashMap<String, CachedEntry<List<Place>>>()
    private val placeDetailsCache = LinkedHashMap<String, CachedEntry<Place>>()
    private val photoUriCache = LinkedHashMap<String, CachedEntry<String>>()
    private val inFlightNearby = HashMap<String, CompletableDeferred<List<Place>>>()

    override fun observeNearbyPlaces(
        center: GeoPoint,
        filter: FilterState,
        languageCode: String
    ): Flow<PlacesFeedResult> = flow {
        if (!remote.isConfigured) {
            val samplePlaces = applyClientFiltersAndSort(
                places = SamplePlacesDataSource.places,
                center = center,
                filter = filter
            )
            emit(PlacesFeedResult(places = samplePlaces, isLiveApi = false))
            return@flow
        }

        val cacheKey = buildNearbyCacheKey(center, filter, languageCode)
        val cached = getCachedNearby(cacheKey)
        if (cached != null) {
            emit(
                PlacesFeedResult(
                    places = applyClientFiltersAndSort(cached, center, filter),
                    isLiveApi = true
                )
            )
            return@flow
        }

        try {
            val fetched = fetchNearbyDeduplicated(
                cacheKey = cacheKey,
                center = center,
                filter = filter,
                languageCode = languageCode
            )
            emit(
                PlacesFeedResult(
                    places = applyClientFiltersAndSort(fetched, center, filter),
                    isLiveApi = true
                )
            )
        } catch (t: Throwable) {
            emit(
                PlacesFeedResult(
                    places = emptyList(),
                    isLiveApi = true,
                    errorMessage = t.message ?: "Unable to load nearby places."
                )
            )
        }
    }

    override suspend fun autocomplete(
        query: String,
        center: GeoPoint,
        sessionToken: String,
        languageCode: String
    ): Result<List<AutocompleteSuggestion>> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return Result.success(emptyList())

        if (!remote.isConfigured) {
            val localMatches = SamplePlacesDataSource.places
                .filter {
                    it.name.contains(trimmed, ignoreCase = true) ||
                        it.primaryCategoryLabel.contains(trimmed, ignoreCase = true) ||
                        it.address.contains(trimmed, ignoreCase = true)
                }
                .map { place ->
                    AutocompleteSuggestion.PlacePrediction(
                        placeId = place.id,
                        primaryText = place.name,
                        secondaryText = place.address,
                        distanceMeters = GeoUtils.haversineMeters(center, place.location).roundToInt()
                    )
                }
            return Result.success(localMatches)
        }

        return runCatching {
            val response = remote.autocomplete(
                input = trimmed,
                center = center,
                sessionToken = sessionToken,
                languageCode = languageCode
            )
            PlacesMapper.toDomainSuggestions(response)
        }
    }

    override suspend fun searchPlaces(
        query: String,
        center: GeoPoint,
        languageCode: String
    ): Result<List<Place>> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return Result.success(emptyList())

        if (!remote.isConfigured) {
            val matches = SamplePlacesDataSource.places.filter { place ->
                place.name.contains(trimmed, ignoreCase = true) ||
                    place.primaryCategoryLabel.contains(trimmed, ignoreCase = true) ||
                    place.address.contains(trimmed, ignoreCase = true)
            }.sortedBy { GeoUtils.haversineMeters(center, it.location) }
            return Result.success(matches)
        }

        val isArabic = languageCode.startsWith("ar", ignoreCase = true)
        return runCatching {
            val response = remote.searchText(
                query = trimmed,
                center = center,
                radiusMeters = 10_000.0,
                includedType = null,
                openNow = null,
                minRating = null,
                languageCode = languageCode
            )
            response.places.mapNotNull { PlacesMapper.toDomainPlace(it, isArabic) }
        }
    }

    override suspend fun getPlaceById(
        placeId: String,
        sessionToken: String?,
        languageCode: String
    ): Result<Place> {
        val sampleMatch = SamplePlacesDataSource.places.find { it.id == placeId }
        if (!remote.isConfigured) {
            return if (sampleMatch != null) {
                Result.success(sampleMatch)
            } else {
                Result.failure(NoSuchElementException("Place not found."))
            }
        }

        val cacheKey = "${placeId}_$languageCode"
        val cachedRich = cacheMutex.withLock {
            val entry = placeDetailsCache[cacheKey]
            if (entry != null && entry.cachedAt.elapsedNow() <= CACHE_TTL) entry.value else null
        }
        if (cachedRich != null && sessionToken == null) {
            return Result.success(cachedRich)
        }

        val isArabic = languageCode.startsWith("ar", ignoreCase = true)
        return runCatching {
            val dto = remote.getPlaceDetails(
                placeId = placeId,
                sessionToken = sessionToken,
                languageCode = languageCode
            )
            val domain = PlacesMapper.toDomainPlace(dto, isArabic)
                ?: throw IllegalStateException("Place details unavailable.")
            cacheMutex.withLock {
                putBounded(placeDetailsCache, cacheKey, CachedEntry(domain, timeSource.markNow()), MAX_DETAILS_CACHE)
            }
            domain
        }
    }

    override suspend fun resolvePhotoUri(
        photoResourceName: String,
        maxWidthPx: Int,
        maxHeightPx: Int
    ): String? {
        if (!remote.isConfigured || photoResourceName.isBlank()) return null
        val key = "${photoResourceName}_${maxWidthPx}x${maxHeightPx}"
        val cached = cacheMutex.withLock {
            val entry = photoUriCache[key]
            if (entry != null && entry.cachedAt.elapsedNow() <= PHOTO_URI_TTL) entry.value else null
        }
        if (cached != null) return cached

        return runCatching {
            remote.fetchPhotoUri(photoResourceName, maxWidthPx, maxHeightPx)
        }.getOrNull()?.also { uri ->
            cacheMutex.withLock {
                putBounded(photoUriCache, key, CachedEntry(uri, timeSource.markNow()), MAX_PHOTO_CACHE)
            }
        }
    }

    override fun getRecentSearches(): Flow<List<String>> = recentSearchStore.getRecentSearches()

    override suspend fun recordSearch(query: String) {
        recentSearchStore.recordSearch(query)
    }

    override suspend fun clearRecentSearches() {
        recentSearchStore.clearRecentSearches()
    }

    override fun getAvailableLocations(): List<LocationContext> = SamplePlacesDataSource.locations

    private suspend fun getCachedNearby(cacheKey: String): List<Place>? = cacheMutex.withLock {
        val entry = nearbyCache[cacheKey] ?: return@withLock null
        if (entry.cachedAt.elapsedNow() <= CACHE_TTL) entry.value else null
    }

    private suspend fun fetchNearbyDeduplicated(
        cacheKey: String,
        center: GeoPoint,
        filter: FilterState,
        languageCode: String
    ): List<Place> {
        val (deferred, isLeader) = cacheMutex.withLock {
            val existing = inFlightNearby[cacheKey]
            if (existing != null) {
                existing to false
            } else {
                val created = CompletableDeferred<List<Place>>()
                inFlightNearby[cacheKey] = created
                created to true
            }
        }

        if (!isLeader) return deferred.await()

        val isArabic = languageCode.startsWith("ar", ignoreCase = true)
        return try {
            val response = remote.searchNearby(
                center = center,
                radiusMeters = filter.maxDistanceMeters,
                includedTypes = CategoryRegistry.includedTypesFor(filter.selectedCategory),
                rankByDistance = filter.sortOption == SortOption.NEAREST,
                languageCode = languageCode
            )
            val mapped = response.places.mapNotNull { PlacesMapper.toDomainPlace(it, isArabic) }
            cacheMutex.withLock {
                putBounded(nearbyCache, cacheKey, CachedEntry(mapped, timeSource.markNow()), MAX_NEARBY_CACHE)
            }
            deferred.complete(mapped)
            mapped
        } catch (t: Throwable) {
            deferred.completeExceptionally(t)
            throw t
        } finally {
            cacheMutex.withLock {
                inFlightNearby.remove(cacheKey)
            }
        }
    }

    private fun buildNearbyCacheKey(
        center: GeoPoint,
        filter: FilterState,
        languageCode: String
    ): String {
        val latBucket = (center.latitude * 1000).roundToInt()
        val lngBucket = (center.longitude * 1000).roundToInt()
        val radiusBucket = (filter.maxDistanceMeters / 250.0).roundToInt()
        return "$latBucket,$lngBucket:$radiusBucket:${filter.selectedCategory?.name ?: "ALL"}:${filter.sortOption.name}:$languageCode"
    }

    private fun applyClientFiltersAndSort(
        places: List<Place>,
        center: GeoPoint,
        filter: FilterState
    ): List<Place> {
        val filtered = places.filter { place ->
            val categoryMatch = filter.selectedCategory == null || place.primaryCategory == filter.selectedCategory
            val openMatch = !filter.openNowOnly || place.isOpenNow == true
            val ratingMatch = filter.minRating == null || (place.rating ?: 0.0) >= filter.minRating
            val distance = GeoUtils.haversineMeters(center, place.location)
            categoryMatch && openMatch && ratingMatch && distance <= filter.maxDistanceMeters
        }

        return when (filter.sortOption) {
            SortOption.NEAREST -> filtered.sortedBy { GeoUtils.haversineMeters(center, it.location) }
            SortOption.HIGHEST_RATED -> filtered.sortedByDescending { it.rating ?: 0.0 }
            SortOption.MOST_REVIEWED -> filtered.sortedByDescending { it.reviewCount ?: 0 }
            SortOption.RECOMMENDED -> filtered.sortedByDescending { place ->
                val ratingScore = (place.rating ?: 3.5) * 2.0
                val distancePenalty = GeoUtils.haversineMeters(center, place.location) / 1000.0
                ratingScore - distancePenalty
            }
        }
    }

    private fun <K, V> putBounded(map: LinkedHashMap<K, V>, key: K, value: V, maxSize: Int) {
        map.remove(key)
        map[key] = value
        while (map.size > maxSize) {
            val eldestKey = map.keys.firstOrNull() ?: break
            map.remove(eldestKey)
        }
    }

    companion object {
        private val CACHE_TTL = kotlin.time.Duration.parse("5m")
        private val PHOTO_URI_TTL = kotlin.time.Duration.parse("20m")
        private const val MAX_NEARBY_CACHE = 40
        private const val MAX_DETAILS_CACHE = 100
        private const val MAX_PHOTO_CACHE = 120
    }
}

package com.zahid.aroundme.data.local

import com.zahid.aroundme.core.platform.currentEpochMillis
import com.zahid.aroundme.data.remote.RecentSearchStore
import com.zahid.aroundme.domain.model.AppLanguage
import com.zahid.aroundme.domain.model.DistanceUnit
import com.zahid.aroundme.domain.model.GeoPoint
import com.zahid.aroundme.domain.model.SavedCollection
import com.zahid.aroundme.domain.model.ThemePreference
import com.zahid.aroundme.domain.model.UserPreferences
import com.zahid.aroundme.domain.repository.SavedPlacesRepository
import com.zahid.aroundme.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RoomRecentSearchStore(
    private val dao: RecentSearchDao,
    private val clockMillis: () -> Long = ::currentEpochMillis
) : RecentSearchStore {
    override fun getRecentSearches(): Flow<List<String>> = dao.observeRecentQueries()

    override suspend fun recordSearch(query: String) {
        val clean = query.trim()
        if (clean.isEmpty()) return
        dao.upsertSearch(
            RecentSearchEntity(
                query = clean,
                searchedAtEpochMs = clockMillis()
            )
        )
        dao.trimOldSearches()
    }

    override suspend fun clearRecentSearches() {
        dao.clearAll()
    }
}

class RoomSavedPlacesRepository(
    private val dao: SavedPlacesDao,
    private val clockMillis: () -> Long = ::currentEpochMillis,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : SavedPlacesRepository {

    private val seedMutex = Mutex()
    private var seeded = false

    private val _collections = MutableStateFlow<List<SavedCollection>>(emptyList())
    override val collections: StateFlow<List<SavedCollection>> = _collections.asStateFlow()

    private val _savedPlaceIds = MutableStateFlow<Set<String>>(emptySet())
    override val savedPlaceIds: StateFlow<Set<String>> = _savedPlaceIds.asStateFlow()

    private val _placeNotes = MutableStateFlow<Map<String, String>>(emptyMap())
    override val placeNotes: StateFlow<Map<String, String>> = _placeNotes.asStateFlow()

    private val cachedGeoByPlaceId = MutableStateFlow<Map<String, Pair<GeoPoint, Long>>>(emptyMap())

    init {
        combine(
            dao.observeCollections(),
            dao.observeAllPlaceRefs()
        ) { colEntities, refEntities ->
            colEntities to refEntities
        }.onEach { (colEntities, refEntities) ->
            if (!seeded && colEntities.isEmpty()) {
                ensureDefaultCollections()
            }

            val refsByCollection = refEntities.groupBy { it.collectionId }
            _collections.value = colEntities.map { col ->
                SavedCollection(
                    id = col.id,
                    name = col.name,
                    nameAr = col.nameAr,
                    placeIds = refsByCollection[col.id].orEmpty().map { it.placeId },
                    isDefault = col.isDefault
                )
            }

            _savedPlaceIds.value = refEntities.map { it.placeId }.toSet()

            val notes = LinkedHashMap<String, String>()
            val geoMap = LinkedHashMap<String, Pair<GeoPoint, Long>>()
            val now = clockMillis()
            refEntities.forEach { ref ->
                if (ref.userNote.isNotBlank() && !notes.containsKey(ref.placeId)) {
                    notes[ref.placeId] = ref.userNote
                }
                val lat = ref.cachedLat
                val lng = ref.cachedLng
                val exp = ref.geoExpiresAtEpochMs
                if (lat != null && lng != null && exp != null && now <= exp) {
                    geoMap[ref.placeId] = GeoPoint(lat, lng) to exp
                }
            }
            _placeNotes.value = notes
            cachedGeoByPlaceId.value = geoMap
        }.launchIn(scope)
    }

    override fun getCachedCoordinates(placeId: String): GeoPoint? {
        val entry = cachedGeoByPlaceId.value[placeId] ?: return null
        return if (clockMillis() <= entry.second) entry.first else null
    }

    override suspend fun toggleSavePlace(
        placeId: String,
        collectionId: String,
        coordinates: GeoPoint?
    ) {
        ensureDefaultCollections()
        val existingRefs = dao.getPlaceRefsForPlace(placeId)
        if (existingRefs.isNotEmpty()) {
            dao.deletePlaceFromAllCollections(placeId)
        } else {
            val now = clockMillis()
            dao.upsertPlaceRef(
                SavedPlaceRefEntity(
                    placeId = placeId,
                    collectionId = collectionId,
                    userNote = "",
                    cachedLat = coordinates?.latitude,
                    cachedLng = coordinates?.longitude,
                    savedAtEpochMs = now,
                    geoExpiresAtEpochMs = coordinates?.let { now + GEO_TTL_MS }
                )
            )
        }
        dao.clearExpiredCoordinates(clockMillis())
    }

    override suspend fun assignPlaceToCollection(
        placeId: String,
        collectionId: String,
        coordinates: GeoPoint?
    ) {
        ensureDefaultCollections()
        val existingRefs = dao.getPlaceRefsForPlace(placeId)
        val existingInCollection = existingRefs.find { it.collectionId == collectionId }
        if (existingInCollection != null) {
            dao.deletePlaceFromCollection(placeId, collectionId)
            return
        }
        val preservedNote = existingRefs.firstOrNull { it.userNote.isNotBlank() }?.userNote.orEmpty()
        val now = clockMillis()
        dao.upsertPlaceRef(
            SavedPlaceRefEntity(
                placeId = placeId,
                collectionId = collectionId,
                userNote = preservedNote,
                cachedLat = coordinates?.latitude,
                cachedLng = coordinates?.longitude,
                savedAtEpochMs = now,
                geoExpiresAtEpochMs = coordinates?.let { now + GEO_TTL_MS }
            )
        )
    }

    override suspend fun createCollection(name: String) {
        ensureDefaultCollections()
        val clean = name.trim()
        if (clean.isEmpty()) return
        val slug = clean.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifEmpty { "list" }
        val id = "custom_${slug}_${clockMillis()}"
        dao.upsertCollection(
            SavedCollectionEntity(
                id = id,
                name = clean,
                nameAr = clean,
                isDefault = false,
                createdAtEpochMs = clockMillis()
            )
        )
    }

    override suspend fun renameCollection(collectionId: String, newName: String) {
        val clean = newName.trim()
        if (clean.isEmpty()) return
        dao.renameCustomCollection(collectionId, clean)
    }

    override suspend fun deleteCollection(collectionId: String) {
        dao.deleteCustomCollection(collectionId)
    }

    override suspend fun updatePlaceNote(placeId: String, note: String) {
        ensureDefaultCollections()
        val trimmed = note.trim()
        val existingRefs = dao.getPlaceRefsForPlace(placeId)
        if (existingRefs.isEmpty()) {
            val now = clockMillis()
            dao.upsertPlaceRef(
                SavedPlaceRefEntity(
                    placeId = placeId,
                    collectionId = DEFAULT_COLLECTION_ID,
                    userNote = trimmed,
                    cachedLat = null,
                    cachedLng = null,
                    savedAtEpochMs = now,
                    geoExpiresAtEpochMs = null
                )
            )
        } else {
            dao.updateNoteForPlace(placeId, trimmed)
        }
    }

    private suspend fun ensureDefaultCollections() {
        if (seeded) return
        seedMutex.withLock {
            if (seeded) return@withLock
            if (dao.countCollections() == 0) {
                val now = clockMillis()
                dao.insertCollectionsIgnore(
                    listOf(
                        SavedCollectionEntity("want_to_visit", "Want to Visit", "أرغب في زيارتها", true, now),
                        SavedCollectionEntity("favorite_restaurants", "Favorite Restaurants", "المطاعم المفضلة", true, now + 1),
                        SavedCollectionEntity("weekend_plans", "Weekend Plans", "خطط نهاية الأسبوع", true, now + 2),
                        SavedCollectionEntity("travel", "Travel", "السفر", true, now + 3)
                    )
                )
            }
            seeded = true
        }
    }

    companion object {
        const val DEFAULT_COLLECTION_ID = "want_to_visit"
        const val GEO_TTL_MS: Long = 30L * 24 * 60 * 60 * 1000 // Google Maps Platform ToS max 30 days
    }
}

class RoomUserPreferencesRepository(
    private val dao: UserPreferencesDao,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : UserPreferencesRepository {

    private val writeMutex = Mutex()
    private val _preferences = MutableStateFlow(UserPreferences())
    override val preferences: StateFlow<UserPreferences> = _preferences.asStateFlow()

    init {
        dao.observePreferences()
            .onEach { entity ->
                if (entity != null) {
                    _preferences.value = entity.toDomain()
                }
            }
            .launchIn(scope)
    }

    override suspend fun updatePreferences(transform: (UserPreferences) -> UserPreferences) {
        writeMutex.withLock {
            val current = dao.getPreferences()?.toDomain() ?: _preferences.value
            val updated = transform(current)
            _preferences.value = updated
            dao.upsertPreferences(updated.toEntity())
        }
    }

    private fun UserPreferencesEntity.toDomain(): UserPreferences {
        val theme = ThemePreference.entries.find { it.name == themePreference } ?: ThemePreference.SYSTEM
        val lang = AppLanguage.entries.find { it.code == languageCode } ?: AppLanguage.ENGLISH
        val unit = DistanceUnit.entries.find { it.name == distanceUnit } ?: DistanceUnit.METRIC
        val radius = defaultRadiusMeters.coerceIn(1000.0, 50_000.0)
        return UserPreferences(
            themePreference = theme,
            language = lang,
            distanceUnit = unit,
            defaultRadiusMeters = radius
        )
    }

    private fun UserPreferences.toEntity(): UserPreferencesEntity = UserPreferencesEntity(
        id = UserPreferencesEntity.SINGLETON_ID,
        themePreference = themePreference.name,
        languageCode = language.code,
        distanceUnit = distanceUnit.name,
        defaultRadiusMeters = defaultRadiusMeters
    )
}

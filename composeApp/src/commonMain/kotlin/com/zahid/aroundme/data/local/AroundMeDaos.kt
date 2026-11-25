package com.zahid.aroundme.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPlacesDao {
    @Query("SELECT * FROM saved_collections ORDER BY isDefault DESC, createdAtEpochMs ASC")
    fun observeCollections(): Flow<List<SavedCollectionEntity>>

    @Query("SELECT * FROM saved_place_refs ORDER BY savedAtEpochMs DESC")
    fun observeAllPlaceRefs(): Flow<List<SavedPlaceRefEntity>>

    @Query("SELECT COUNT(*) FROM saved_collections")
    suspend fun countCollections(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCollectionsIgnore(collections: List<SavedCollectionEntity>)

    @Upsert
    suspend fun upsertCollection(collection: SavedCollectionEntity)

    @Query("UPDATE saved_collections SET name = :newName, nameAr = :newName WHERE id = :collectionId AND isDefault = 0")
    suspend fun renameCustomCollection(collectionId: String, newName: String)

    @Query("DELETE FROM saved_collections WHERE id = :collectionId AND isDefault = 0")
    suspend fun deleteCustomCollection(collectionId: String)

    @Upsert
    suspend fun upsertPlaceRef(ref: SavedPlaceRefEntity)

    @Query("SELECT * FROM saved_place_refs WHERE placeId = :placeId")
    suspend fun getPlaceRefsForPlace(placeId: String): List<SavedPlaceRefEntity>

    @Query("DELETE FROM saved_place_refs WHERE placeId = :placeId")
    suspend fun deletePlaceFromAllCollections(placeId: String)

    @Query("DELETE FROM saved_place_refs WHERE placeId = :placeId AND collectionId = :collectionId")
    suspend fun deletePlaceFromCollection(placeId: String, collectionId: String)

    @Query("UPDATE saved_place_refs SET userNote = :note WHERE placeId = :placeId")
    suspend fun updateNoteForPlace(placeId: String, note: String)

    @Query("UPDATE saved_place_refs SET cachedLat = NULL, cachedLng = NULL, geoExpiresAtEpochMs = NULL WHERE geoExpiresAtEpochMs IS NOT NULL AND geoExpiresAtEpochMs < :nowEpochMs")
    suspend fun clearExpiredCoordinates(nowEpochMs: Long)
}

@Dao
interface RecentSearchDao {
    @Query("SELECT `query` FROM recent_searches ORDER BY searchedAtEpochMs DESC LIMIT 8")
    fun observeRecentQueries(): Flow<List<String>>

    @Upsert
    suspend fun upsertSearch(entity: RecentSearchEntity)

    @Query("DELETE FROM recent_searches WHERE `query` NOT IN (SELECT `query` FROM recent_searches ORDER BY searchedAtEpochMs DESC LIMIT 8)")
    suspend fun trimOldSearches()

    @Query("DELETE FROM recent_searches")
    suspend fun clearAll()
}

@Dao
interface UserPreferencesDao {
    @Query("SELECT * FROM user_preferences WHERE id = :id LIMIT 1")
    fun observePreferences(id: String = UserPreferencesEntity.SINGLETON_ID): Flow<UserPreferencesEntity?>

    @Query("SELECT * FROM user_preferences WHERE id = :id LIMIT 1")
    suspend fun getPreferences(id: String = UserPreferencesEntity.SINGLETON_ID): UserPreferencesEntity?

    @Upsert
    suspend fun upsertPreferences(entity: UserPreferencesEntity)
}

package com.zahid.aroundme.data.local

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "saved_collections")
data class SavedCollectionEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val nameAr: String,
    val isDefault: Boolean,
    val createdAtEpochMs: Long
)

/**
 * Stores strictly user-generated bookmarks/notes, Google Places `placeId`, and optional
 * temporary coordinates capped at 30 days per Google Maps Platform ToS Section 3.2.3.
 * Never persists Google Place names, addresses, ratings, reviews, hours, or photo names.
 */
@Entity(
    tableName = "saved_place_refs",
    primaryKeys = ["placeId", "collectionId"],
    foreignKeys = [
        ForeignKey(
            entity = SavedCollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["collectionId"]),
        Index(value = ["placeId"])
    ]
)
data class SavedPlaceRefEntity(
    val placeId: String,
    val collectionId: String,
    val userNote: String,
    val cachedLat: Double?,
    val cachedLng: Double?,
    val savedAtEpochMs: Long,
    val geoExpiresAtEpochMs: Long?
)

@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey
    val query: String,
    val searchedAtEpochMs: Long
)

@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey
    val id: String = SINGLETON_ID,
    val themePreference: String,
    val languageCode: String,
    val distanceUnit: String,
    val defaultRadiusMeters: Double
) {
    companion object {
        const val SINGLETON_ID = "default_prefs"
    }
}

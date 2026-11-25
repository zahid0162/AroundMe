package com.zahid.aroundme.data.local

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    entities = [
        SavedCollectionEntity::class,
        SavedPlaceRefEntity::class,
        RecentSearchEntity::class,
        UserPreferencesEntity::class
    ],
    version = 1,
    exportSchema = true
)
@ConstructedBy(AroundMeDatabaseConstructor::class)
abstract class AroundMeDatabase : RoomDatabase() {
    abstract fun savedPlacesDao(): SavedPlacesDao
    abstract fun recentSearchDao(): RecentSearchDao
    abstract fun userPreferencesDao(): UserPreferencesDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AroundMeDatabaseConstructor : RoomDatabaseConstructor<AroundMeDatabase> {
    override fun initialize(): AroundMeDatabase
}

fun buildAroundMeDatabase(builder: RoomDatabase.Builder<AroundMeDatabase>): AroundMeDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
}

package com.zahid.aroundme.data.local

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase

fun createAndroidDatabaseBuilder(context: Context): RoomDatabase.Builder<AroundMeDatabase> {
    val dbFile = context.applicationContext.getDatabasePath("aroundme.db")
    return Room.databaseBuilder<AroundMeDatabase>(
        context = context.applicationContext,
        name = dbFile.absolutePath
    )
}

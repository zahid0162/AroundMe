package com.zahid.aroundme.data.local

import androidx.room3.Room
import androidx.room3.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
fun createIosDatabaseBuilder(): RoomDatabase.Builder<AroundMeDatabase> {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null
    )
    val dbPath = requireNotNull(documentDirectory?.path) + "/aroundme.db"
    return Room.databaseBuilder<AroundMeDatabase>(
        name = dbPath
    )
}

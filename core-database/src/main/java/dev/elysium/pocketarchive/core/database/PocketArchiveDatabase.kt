package dev.elysium.pocketarchive.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ArchiveItemEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class PocketArchiveDatabase : RoomDatabase() {
    abstract fun archiveItemDao(): ArchiveItemDao

    companion object {
        const val DATABASE_NAME = "pocketarchive.db"
    }
}

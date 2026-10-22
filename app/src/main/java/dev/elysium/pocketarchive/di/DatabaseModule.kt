package dev.elysium.pocketarchive.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.elysium.pocketarchive.core.database.ArchiveItemDao
import dev.elysium.pocketarchive.core.database.PocketArchiveDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun providePocketArchiveDatabase(
        @ApplicationContext context: Context,
    ): PocketArchiveDatabase = Room.databaseBuilder(
        context,
        PocketArchiveDatabase::class.java,
        PocketArchiveDatabase.DATABASE_NAME,
    ).build()

    @Provides
    @Singleton
    fun provideArchiveItemDao(
        database: PocketArchiveDatabase,
    ): ArchiveItemDao = database.archiveItemDao()
}

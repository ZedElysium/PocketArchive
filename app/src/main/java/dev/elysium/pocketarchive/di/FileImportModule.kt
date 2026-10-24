package dev.elysium.pocketarchive.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.elysium.pocketarchive.core.data.ArchiveRepository
import dev.elysium.pocketarchive.core.data.PersistingDocumentImporter
import dev.elysium.pocketarchive.core.files.AppPrivateDocumentStore
import dev.elysium.pocketarchive.core.files.ArchiveItemIdGenerator
import dev.elysium.pocketarchive.core.files.ContentResolverDocumentMetadataReader
import dev.elysium.pocketarchive.core.files.DocumentImporter
import dev.elysium.pocketarchive.core.files.DocumentMetadataReader
import dev.elysium.pocketarchive.core.files.ImportDocumentUseCase
import dev.elysium.pocketarchive.core.files.PrivateDocumentStore
import dev.elysium.pocketarchive.core.files.UuidArchiveItemIdGenerator
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FileImportModule {
    @Provides
    @Singleton
    fun provideDocumentMetadataReader(
        @ApplicationContext context: Context,
    ): DocumentMetadataReader = ContentResolverDocumentMetadataReader(context.contentResolver)

    @Provides
    @Singleton
    fun providePrivateDocumentStore(
        @ApplicationContext context: Context,
    ): PrivateDocumentStore = AppPrivateDocumentStore(context)

    @Provides
    @Singleton
    fun provideArchiveItemIdGenerator(): ArchiveItemIdGenerator =
        UuidArchiveItemIdGenerator()

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemUTC()

    @Provides
    @Singleton
    fun provideFileDocumentImporter(
        metadataReader: DocumentMetadataReader,
        privateDocumentStore: PrivateDocumentStore,
        archiveItemIdGenerator: ArchiveItemIdGenerator,
        clock: Clock,
    ): ImportDocumentUseCase = ImportDocumentUseCase(
        metadataReader = metadataReader,
        privateDocumentStore = privateDocumentStore,
        archiveItemIdGenerator = archiveItemIdGenerator,
        clock = clock,
    )

    @Provides
    @Singleton
    fun provideDocumentImporter(
        fileImporter: ImportDocumentUseCase,
        archiveRepository: ArchiveRepository,
        privateDocumentStore: PrivateDocumentStore,
    ): DocumentImporter = PersistingDocumentImporter(
        fileImporter = fileImporter,
        archiveRepository = archiveRepository,
        privateDocumentStore = privateDocumentStore,
    )
}

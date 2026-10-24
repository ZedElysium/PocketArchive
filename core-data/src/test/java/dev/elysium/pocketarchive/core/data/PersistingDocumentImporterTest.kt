package dev.elysium.pocketarchive.core.data

import dev.elysium.pocketarchive.core.files.DocumentImporter
import dev.elysium.pocketarchive.core.files.DocumentMetadata
import dev.elysium.pocketarchive.core.files.ImportDocumentFailure
import dev.elysium.pocketarchive.core.files.ImportDocumentResult
import dev.elysium.pocketarchive.core.files.PrivateDocumentKey
import dev.elysium.pocketarchive.core.files.PrivateDocumentStore
import dev.elysium.pocketarchive.core.files.StagedDocument
import dev.elysium.pocketarchive.core.files.StoredDocument
import dev.elysium.pocketarchive.core.model.ArchiveItem
import dev.elysium.pocketarchive.core.model.ArchiveItemId
import dev.elysium.pocketarchive.core.model.ArchiveItemType
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PersistingDocumentImporterTest {
    @Test
    fun successfulFileImport_isPersistedAndReturned() = runBlocking {
        val repository = RecordingArchiveRepository()
        val store = RecordingPrivateDocumentStore()
        val importer = importer(repository = repository, store = store)

        val result = importer.import(DOCUMENT_URI)

        assertEquals(SUCCESS_RESULT, result)
        assertEquals(listOf(ARCHIVE_ITEM to STORED_DOCUMENT), repository.addedItems)
        assertEquals(emptyList<StoredDocument>(), store.deletedDocuments)
    }

    @Test
    fun fileImportFailure_isReturnedWithoutDatabaseOrCleanupWork() = runBlocking {
        val repository = RecordingArchiveRepository()
        val store = RecordingPrivateDocumentStore()
        val failure = ImportDocumentResult.Failure(ImportDocumentFailure.CopyFailed)
        val importer = importer(
            fileResult = failure,
            repository = repository,
            store = store,
        )

        assertEquals(failure, importer.import(DOCUMENT_URI))
        assertEquals(emptyList<Pair<ArchiveItem, StoredDocument>>(), repository.addedItems)
        assertEquals(emptyList<StoredDocument>(), store.deletedDocuments)
    }

    @Test
    fun persistenceFailure_deletesCommittedFileAndReportsFailure() = runBlocking {
        val repository = RecordingArchiveRepository(
            addFailure = IllegalStateException("database failed"),
        )
        val store = RecordingPrivateDocumentStore()
        val importer = importer(repository = repository, store = store)

        assertEquals(
            ImportDocumentResult.Failure(ImportDocumentFailure.PersistenceFailed),
            importer.import(DOCUMENT_URI),
        )
        assertEquals(listOf(STORED_DOCUMENT), store.deletedDocuments)
    }

    @Test
    fun persistenceAndCleanupFailure_reportsInconsistentCleanup() = runBlocking {
        val repository = RecordingArchiveRepository(
            addFailure = IllegalStateException("database failed"),
        )
        val store = RecordingPrivateDocumentStore(
            deleteFailure = IllegalStateException("delete failed"),
        )
        val importer = importer(repository = repository, store = store)

        assertEquals(
            ImportDocumentResult.Failure(ImportDocumentFailure.PersistenceCleanupFailed),
            importer.import(DOCUMENT_URI),
        )
        assertEquals(listOf(STORED_DOCUMENT), store.deletedDocuments)
    }

    @Test
    fun cancellationDuringPersistence_deletesCommittedFileAndPropagates() {
        val cancellation = CancellationException("cancelled")
        val repository = RecordingArchiveRepository(addFailure = cancellation)
        val store = RecordingPrivateDocumentStore()
        val importer = importer(repository = repository, store = store)

        val thrown = assertThrows(CancellationException::class.java) {
            runBlocking { importer.import(DOCUMENT_URI) }
        }

        assertEquals(cancellation, thrown)
        assertEquals(listOf(STORED_DOCUMENT), store.deletedDocuments)
    }

    private fun importer(
        fileResult: ImportDocumentResult = SUCCESS_RESULT,
        repository: RecordingArchiveRepository,
        store: RecordingPrivateDocumentStore,
    ) = PersistingDocumentImporter(
        fileImporter = DocumentImporter { fileResult },
        archiveRepository = repository,
        privateDocumentStore = store,
    )

    private class RecordingArchiveRepository(
        private val addFailure: Exception? = null,
    ) : ArchiveRepository {
        val addedItems = mutableListOf<Pair<ArchiveItem, StoredDocument>>()

        override suspend fun addImportedItem(
            archiveItem: ArchiveItem,
            storedDocument: StoredDocument,
        ) {
            addedItems += archiveItem to storedDocument
            addFailure?.let { throw it }
        }

        override fun observeAll(): Flow<List<ArchiveItem>> = flowOf(emptyList())
    }

    private class RecordingPrivateDocumentStore(
        private val deleteFailure: Exception? = null,
    ) : PrivateDocumentStore {
        val deletedDocuments = mutableListOf<StoredDocument>()

        override suspend fun stage(
            documentUri: String,
            archiveItemId: ArchiveItemId,
            metadata: DocumentMetadata,
        ): StagedDocument = error("Not used by persistence coordinator")

        override suspend fun commit(stagedDocument: StagedDocument): StoredDocument =
            error("Not used by persistence coordinator")

        override suspend fun discard(stagedDocument: StagedDocument) =
            error("Not used by persistence coordinator")

        override suspend fun delete(storedDocument: StoredDocument) {
            deletedDocuments += storedDocument
            deleteFailure?.let { throw it }
        }
    }

    private companion object {
        const val DOCUMENT_URI = "content://documents/guide.pdf"
        val ARCHIVE_ITEM = ArchiveItem(
            id = ArchiveItemId("item-1"),
            displayName = "guide.pdf",
            type = ArchiveItemType.PDF,
            mimeType = "application/pdf",
            sizeBytes = 321L,
            importedAt = Instant.parse("2026-10-24T08:00:00Z"),
        )
        val STORED_DOCUMENT = StoredDocument(
            key = PrivateDocumentKey("items/document-1"),
            sizeBytes = 321L,
        )
        val SUCCESS_RESULT = ImportDocumentResult.Success(
            archiveItem = ARCHIVE_ITEM,
            storedDocument = STORED_DOCUMENT,
        )
    }
}

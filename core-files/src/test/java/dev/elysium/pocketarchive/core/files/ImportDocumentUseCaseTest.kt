package dev.elysium.pocketarchive.core.files

import dev.elysium.pocketarchive.core.model.ArchiveItemId
import dev.elysium.pocketarchive.core.model.ArchiveItemType
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ImportDocumentUseCaseTest {
    @Test
    fun successfulImport_commitsStagedFileAndUsesCopiedSize() = runBlocking {
        val store = FakePrivateDocumentStore()
        val useCase = useCase(
            metadataReader = successfulMetadataReader(sizeBytes = null),
            store = store,
        )

        val result = useCase(DOCUMENT_URI)

        assertEquals(
            ImportDocumentResult.Success(
                archiveItem = dev.elysium.pocketarchive.core.model.ArchiveItem(
                    id = ArchiveItemId("item-1"),
                    displayName = "guide.pdf",
                    type = ArchiveItemType.PDF,
                    mimeType = "application/pdf",
                    sizeBytes = 321L,
                    importedAt = IMPORTED_AT,
                ),
                storedDocument = STORED_DOCUMENT,
            ),
            result,
        )
        assertEquals(listOf("stage", "commit"), store.operations)
    }

    @Test
    fun rejectedMetadata_doesNotTouchPrivateStorage() = runBlocking {
        val store = FakePrivateDocumentStore()
        val useCase = useCase(
            metadataReader = DocumentMetadataReader {
                DocumentMetadataReadResult.Failure(
                    DocumentMetadataFailureReason.UNSUPPORTED_DOCUMENT_TYPE,
                )
            },
            store = store,
        )

        val result = useCase(DOCUMENT_URI)

        assertEquals(
            failure(
                ImportDocumentFailure.MetadataRejected(
                    DocumentMetadataFailureReason.UNSUPPORTED_DOCUMENT_TYPE,
                ),
            ),
            result,
        )
        assertEquals(emptyList<String>(), store.operations)
    }

    @Test
    fun unexpectedMetadataException_isReportedWithoutTouchingStorage() = runBlocking {
        val store = FakePrivateDocumentStore()
        val useCase = useCase(
            metadataReader = DocumentMetadataReader {
                throw IllegalStateException("reader failed")
            },
            store = store,
        )

        assertEquals(
            failure(ImportDocumentFailure.MetadataReadFailed),
            useCase(DOCUMENT_URI),
        )
        assertEquals(emptyList<String>(), store.operations)
    }

    @Test
    fun idGenerationFailure_isReportedBeforeStaging() = runBlocking {
        val store = FakePrivateDocumentStore()
        val useCase = useCase(
            store = store,
            idGenerator = ArchiveItemIdGenerator {
                throw IllegalStateException("ID generation failed")
            },
        )

        assertEquals(
            failure(ImportDocumentFailure.PreparationFailed),
            useCase(DOCUMENT_URI),
        )
        assertEquals(emptyList<String>(), store.operations)
    }

    @Test
    fun stagingFailure_isReportedWithoutCallingDiscard() = runBlocking {
        val store = FakePrivateDocumentStore(
            stageFailure = IllegalStateException("copy failed"),
        )
        val useCase = useCase(store = store)

        assertEquals(
            failure(ImportDocumentFailure.CopyFailed),
            useCase(DOCUMENT_URI),
        )
        assertEquals(listOf("stage"), store.operations)
    }

    @Test
    fun domainPreparationFailureAfterStaging_discardsStagedFile() = runBlocking {
        val store = FakePrivateDocumentStore()
        val useCase = useCase(
            store = store,
            clock = FailingClock,
        )

        assertEquals(
            failure(ImportDocumentFailure.FinalizationFailed),
            useCase(DOCUMENT_URI),
        )
        assertEquals(listOf("stage", "discard"), store.operations)
    }

    @Test
    fun commitFailure_discardsStagedFile() = runBlocking {
        val store = FakePrivateDocumentStore(
            commitFailure = IllegalStateException("commit failed"),
        )
        val useCase = useCase(store = store)

        assertEquals(
            failure(ImportDocumentFailure.FinalizationFailed),
            useCase(DOCUMENT_URI),
        )
        assertEquals(listOf("stage", "commit", "discard"), store.operations)
    }

    @Test
    fun cleanupFailure_isReportedExplicitly() = runBlocking {
        val store = FakePrivateDocumentStore(
            commitFailure = IllegalStateException("commit failed"),
            discardFailure = IllegalStateException("discard failed"),
        )
        val useCase = useCase(store = store)

        assertEquals(
            failure(ImportDocumentFailure.CleanupFailed),
            useCase(DOCUMENT_URI),
        )
        assertEquals(listOf("stage", "commit", "discard"), store.operations)
    }

    @Test
    fun cancellationDuringCommit_discardsStagedFileAndPropagatesCancellation() {
        val cancellation = CancellationException("cancelled")
        val store = FakePrivateDocumentStore(commitFailure = cancellation)
        val useCase = useCase(store = store)

        val thrown = assertThrows(CancellationException::class.java) {
            runBlocking { useCase(DOCUMENT_URI) }
        }

        assertEquals(cancellation, thrown)
        assertEquals(listOf("stage", "commit", "discard"), store.operations)
    }

    private fun useCase(
        metadataReader: DocumentMetadataReader = successfulMetadataReader(),
        store: FakePrivateDocumentStore,
        idGenerator: ArchiveItemIdGenerator = ArchiveItemIdGenerator {
            ArchiveItemId("item-1")
        },
        clock: Clock = Clock.fixed(IMPORTED_AT, ZoneOffset.UTC),
    ) = ImportDocumentUseCase(
        metadataReader = metadataReader,
        privateDocumentStore = store,
        archiveItemIdGenerator = idGenerator,
        clock = clock,
    )

    private fun successfulMetadataReader(
        sizeBytes: Long? = 100L,
    ) = DocumentMetadataReader {
        DocumentMetadataReadResult.Success(
            DocumentMetadata(
                displayName = "guide.pdf",
                mimeType = "application/pdf",
                sizeBytes = sizeBytes,
                type = ArchiveItemType.PDF,
            ),
        )
    }

    private fun failure(reason: ImportDocumentFailure) =
        ImportDocumentResult.Failure(reason)

    private class FakePrivateDocumentStore(
        private val stageFailure: Exception? = null,
        private val commitFailure: Exception? = null,
        private val discardFailure: Exception? = null,
    ) : PrivateDocumentStore {
        val operations = mutableListOf<String>()

        override suspend fun stage(
            documentUri: String,
            archiveItemId: ArchiveItemId,
            metadata: DocumentMetadata,
        ): StagedDocument {
            operations += "stage"
            stageFailure?.let { throw it }
            return STAGED_DOCUMENT
        }

        override suspend fun commit(
            stagedDocument: StagedDocument,
        ): StoredDocument {
            operations += "commit"
            commitFailure?.let { throw it }
            return STORED_DOCUMENT
        }

        override suspend fun discard(stagedDocument: StagedDocument) {
            operations += "discard"
            discardFailure?.let { throw it }
        }
    }

    private companion object {
        const val DOCUMENT_URI = "content://documents/guide.pdf"
        val IMPORTED_AT: Instant = Instant.parse("2026-10-13T08:00:00Z")
        val STAGED_DOCUMENT = StagedDocument(
            token = StagedDocumentToken("stage-1"),
            destinationKey = PrivateDocumentKey("documents/item-1.pdf"),
            sizeBytes = 321L,
        )
        val STORED_DOCUMENT = StoredDocument(
            key = PrivateDocumentKey("documents/item-1.pdf"),
            sizeBytes = 321L,
        )
    }

    private object FailingClock : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = this

        override fun instant(): Instant = throw IllegalStateException("clock failed")
    }
}

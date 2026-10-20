package dev.elysium.pocketarchive.core.files

import dev.elysium.pocketarchive.core.model.ArchiveItem
import java.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class ImportDocumentUseCase(
    private val metadataReader: DocumentMetadataReader,
    private val privateDocumentStore: PrivateDocumentStore,
    private val archiveItemIdGenerator: ArchiveItemIdGenerator,
    private val clock: Clock,
) : DocumentImporter {
    override suspend fun import(documentUri: String): ImportDocumentResult {
        val metadata = when (val result = readMetadata(documentUri)) {
            is MetadataStepResult.Success -> result.metadata
            is MetadataStepResult.Failure -> return ImportDocumentResult.Failure(result.reason)
        }
        val archiveItemId = try {
            archiveItemIdGenerator.generate()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            return ImportDocumentResult.Failure(
                ImportDocumentFailure.PreparationFailed,
            )
        }
        val stagedDocument = try {
            privateDocumentStore.stage(
                documentUri = documentUri,
                archiveItemId = archiveItemId,
                metadata = metadata,
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            return ImportDocumentResult.Failure(
                ImportDocumentFailure.CopyFailed,
            )
        }

        return try {
            val archiveItem = ArchiveItem(
                id = archiveItemId,
                displayName = metadata.displayName,
                type = metadata.type,
                mimeType = metadata.mimeType,
                sizeBytes = stagedDocument.sizeBytes,
                importedAt = clock.instant(),
            )
            val storedDocument = privateDocumentStore.commit(stagedDocument)
            ImportDocumentResult.Success(
                archiveItem = archiveItem,
                storedDocument = storedDocument,
            )
        } catch (cancellation: CancellationException) {
            discardAfterFailure(stagedDocument)?.let(cancellation::addSuppressed)
            throw cancellation
        } catch (_: Exception) {
            val cleanupFailure = discardAfterFailure(stagedDocument)
            ImportDocumentResult.Failure(
                if (cleanupFailure == null) {
                    ImportDocumentFailure.FinalizationFailed
                } else {
                    ImportDocumentFailure.CleanupFailed
                },
            )
        }
    }

    suspend operator fun invoke(documentUri: String): ImportDocumentResult =
        import(documentUri)

    private suspend fun readMetadata(documentUri: String): MetadataStepResult = try {
        when (val result = metadataReader.read(documentUri)) {
            is DocumentMetadataReadResult.Success -> MetadataStepResult.Success(result.metadata)
            is DocumentMetadataReadResult.Failure -> MetadataStepResult.Failure(
                ImportDocumentFailure.MetadataRejected(result.reason),
            )
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        MetadataStepResult.Failure(ImportDocumentFailure.MetadataReadFailed)
    }

    private suspend fun discardAfterFailure(
        stagedDocument: StagedDocument,
    ): Exception? = withContext(NonCancellable) {
        try {
            privateDocumentStore.discard(stagedDocument)
            null
        } catch (failure: Exception) {
            failure
        }
    }

    private sealed interface MetadataStepResult {
        data class Success(
            val metadata: DocumentMetadata,
        ) : MetadataStepResult

        data class Failure(
            val reason: ImportDocumentFailure,
        ) : MetadataStepResult
    }
}

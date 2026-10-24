package dev.elysium.pocketarchive.core.data

import dev.elysium.pocketarchive.core.files.DocumentImporter
import dev.elysium.pocketarchive.core.files.ImportDocumentFailure
import dev.elysium.pocketarchive.core.files.ImportDocumentResult
import dev.elysium.pocketarchive.core.files.PrivateDocumentStore
import dev.elysium.pocketarchive.core.files.StoredDocument
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class PersistingDocumentImporter(
    private val fileImporter: DocumentImporter,
    private val archiveRepository: ArchiveRepository,
    private val privateDocumentStore: PrivateDocumentStore,
) : DocumentImporter {
    override suspend fun import(documentUri: String): ImportDocumentResult {
        val fileResult = fileImporter.import(documentUri)
        if (fileResult !is ImportDocumentResult.Success) return fileResult

        return try {
            archiveRepository.addImportedItem(
                archiveItem = fileResult.archiveItem,
                storedDocument = fileResult.storedDocument,
            )
            fileResult
        } catch (cancellation: CancellationException) {
            deleteAfterFailure(fileResult.storedDocument)?.let(cancellation::addSuppressed)
            throw cancellation
        } catch (_: Exception) {
            val cleanupFailure = deleteAfterFailure(fileResult.storedDocument)
            ImportDocumentResult.Failure(
                if (cleanupFailure == null) {
                    ImportDocumentFailure.PersistenceFailed
                } else {
                    ImportDocumentFailure.PersistenceCleanupFailed
                },
            )
        }
    }

    private suspend fun deleteAfterFailure(
        storedDocument: StoredDocument,
    ): Exception? = withContext(NonCancellable) {
        try {
            privateDocumentStore.delete(storedDocument)
            null
        } catch (failure: Exception) {
            failure
        }
    }
}

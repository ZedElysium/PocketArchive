package dev.elysium.pocketarchive.core.files

import dev.elysium.pocketarchive.core.model.ArchiveItem

sealed interface ImportDocumentResult {
    data class Success(
        val archiveItem: ArchiveItem,
        val storedDocument: StoredDocument,
    ) : ImportDocumentResult

    data class Failure(
        val reason: ImportDocumentFailure,
    ) : ImportDocumentResult
}

sealed interface ImportDocumentFailure {
    data class MetadataRejected(
        val reason: DocumentMetadataFailureReason,
    ) : ImportDocumentFailure

    data object MetadataReadFailed : ImportDocumentFailure
    data object PreparationFailed : ImportDocumentFailure
    data object CopyFailed : ImportDocumentFailure
    data object FinalizationFailed : ImportDocumentFailure
    data object CleanupFailed : ImportDocumentFailure
}

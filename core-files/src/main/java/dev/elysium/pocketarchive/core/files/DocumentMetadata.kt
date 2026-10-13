package dev.elysium.pocketarchive.core.files

import dev.elysium.pocketarchive.core.model.ArchiveItemType

/**
 * Metadata reported by an external document provider.
 *
 * This value does not mean PocketArchive owns a stable copy of the document.
 */
data class DocumentMetadata(
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long?,
    val type: ArchiveItemType,
) {
    init {
        require(displayName.isNotBlank()) { "Display name must not be blank" }
        require(mimeType.isNotBlank()) { "MIME type must not be blank" }
        require(sizeBytes == null || sizeBytes >= 0) {
            "File size must be unknown or non-negative"
        }
    }
}

sealed interface DocumentMetadataReadResult {
    data class Success(
        val metadata: DocumentMetadata,
    ) : DocumentMetadataReadResult

    data class Failure(
        val reason: DocumentMetadataFailureReason,
    ) : DocumentMetadataReadResult
}

enum class DocumentMetadataFailureReason {
    INVALID_DOCUMENT_URI,
    ACCESS_DENIED,
    MIME_TYPE_UNAVAILABLE,
    UNSUPPORTED_DOCUMENT_TYPE,
    METADATA_UNAVAILABLE,
}

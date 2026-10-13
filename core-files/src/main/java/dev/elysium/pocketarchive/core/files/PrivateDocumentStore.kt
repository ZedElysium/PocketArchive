package dev.elysium.pocketarchive.core.files

import dev.elysium.pocketarchive.core.model.ArchiveItemId

interface PrivateDocumentStore {
    /**
     * Copies provider-owned content into a temporary app-private file.
     *
     * If this call fails, the implementation owns cleanup because no token is returned.
     */
    suspend fun stage(
        documentUri: String,
        archiveItemId: ArchiveItemId,
        metadata: DocumentMetadata,
    ): StagedDocument

    /**
     * Publishes a completed temporary file. If this fails, [stagedDocument] must remain
     * discardable.
     */
    suspend fun commit(stagedDocument: StagedDocument): StoredDocument

    /** Removes a temporary file and may be called while its parent import is cancelled. */
    suspend fun discard(stagedDocument: StagedDocument)
}

@JvmInline
value class StagedDocumentToken(val value: String) {
    init {
        require(value.isNotBlank()) { "Staged document token must not be blank" }
    }
}

data class StagedDocument(
    val token: StagedDocumentToken,
    val sizeBytes: Long,
) {
    init {
        require(sizeBytes >= 0) { "Staged document size must not be negative" }
    }
}

@JvmInline
value class PrivateDocumentKey(val value: String) {
    init {
        require(value.isNotBlank()) { "Private document key must not be blank" }
    }
}

/** An opaque app-private storage reference; it intentionally exposes no absolute path. */
data class StoredDocument(
    val key: PrivateDocumentKey,
    val sizeBytes: Long,
) {
    init {
        require(sizeBytes >= 0) { "Stored document size must not be negative" }
    }
}

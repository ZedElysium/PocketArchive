package dev.elysium.pocketarchive.core.files

/**
 * Reads provider-owned document metadata for the duration of a call.
 *
 * A successful result neither persists URI access nor copies content into app-private storage.
 */
fun interface DocumentMetadataReader {
    suspend fun read(documentUri: String): DocumentMetadataReadResult
}

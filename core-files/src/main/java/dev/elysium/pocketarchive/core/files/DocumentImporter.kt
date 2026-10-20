package dev.elysium.pocketarchive.core.files

fun interface DocumentImporter {
    suspend fun import(documentUri: String): ImportDocumentResult
}

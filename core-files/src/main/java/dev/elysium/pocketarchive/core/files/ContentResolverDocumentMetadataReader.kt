package dev.elysium.pocketarchive.core.files

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import dev.elysium.pocketarchive.core.model.ArchiveItemType
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContentResolverDocumentMetadataReader internal constructor(
    private val metadataQuery: DocumentMetadataQuery,
    private val ioDispatcher: CoroutineDispatcher,
) : DocumentMetadataReader {
    constructor(contentResolver: ContentResolver) : this(
        metadataQuery = ContentResolverDocumentMetadataQuery(contentResolver),
        ioDispatcher = Dispatchers.IO,
    )

    override suspend fun read(documentUri: String): DocumentMetadataReadResult {
        if (!documentUri.isContentUri()) {
            return DocumentMetadataReadResult.Failure(
                DocumentMetadataFailureReason.INVALID_DOCUMENT_URI,
            )
        }

        return withContext(ioDispatcher) {
            try {
                metadataQuery.query(documentUri).toReadResult()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: SecurityException) {
                DocumentMetadataReadResult.Failure(
                    DocumentMetadataFailureReason.ACCESS_DENIED,
                )
            } catch (_: Exception) {
                DocumentMetadataReadResult.Failure(
                    DocumentMetadataFailureReason.METADATA_UNAVAILABLE,
                )
            }
        }
    }

    private fun String.isContentUri(): Boolean =
        startsWith(CONTENT_URI_PREFIX, ignoreCase = true) &&
            length > CONTENT_URI_PREFIX.length

    private fun QueriedDocumentMetadata?.toReadResult(): DocumentMetadataReadResult {
        val queriedMetadata = this ?: return DocumentMetadataReadResult.Failure(
            DocumentMetadataFailureReason.METADATA_UNAVAILABLE,
        )
        val displayName = queriedMetadata.displayName?.takeIf(String::isNotBlank)
            ?: return DocumentMetadataReadResult.Failure(
                DocumentMetadataFailureReason.METADATA_UNAVAILABLE,
            )
        val mimeType = queriedMetadata.mimeType
            ?.trim()
            ?.lowercase(Locale.ROOT)
            ?.takeIf(String::isNotBlank)
            ?: return DocumentMetadataReadResult.Failure(
                DocumentMetadataFailureReason.MIME_TYPE_UNAVAILABLE,
            )
        val type = mimeType.toArchiveItemType()
            ?: return DocumentMetadataReadResult.Failure(
                DocumentMetadataFailureReason.UNSUPPORTED_DOCUMENT_TYPE,
            )

        return DocumentMetadataReadResult.Success(
            DocumentMetadata(
                displayName = displayName,
                mimeType = mimeType,
                sizeBytes = queriedMetadata.sizeBytes?.takeIf { it >= 0 },
                type = type,
            ),
        )
    }

    private fun String.toArchiveItemType(): ArchiveItemType? = when {
        this == PDF_MIME_TYPE -> ArchiveItemType.PDF
        startsWith(IMAGE_MIME_TYPE_PREFIX) -> ArchiveItemType.IMAGE
        else -> null
    }

    private companion object {
        const val CONTENT_URI_PREFIX = "content://"
        const val PDF_MIME_TYPE = "application/pdf"
        const val IMAGE_MIME_TYPE_PREFIX = "image/"
    }
}

internal fun interface DocumentMetadataQuery {
    fun query(documentUri: String): QueriedDocumentMetadata?
}

internal data class QueriedDocumentMetadata(
    val displayName: String?,
    val mimeType: String?,
    val sizeBytes: Long?,
)

private class ContentResolverDocumentMetadataQuery(
    private val contentResolver: ContentResolver,
) : DocumentMetadataQuery {
    override fun query(documentUri: String): QueriedDocumentMetadata? {
        val uri = Uri.parse(documentUri)
        val mimeType = contentResolver.getType(uri)
        val cursor = contentResolver.query(
            uri,
            PROJECTION,
            null,
            null,
            null,
        ) ?: return null

        return cursor.use {
            if (!it.moveToFirst()) return null

            val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
            QueriedDocumentMetadata(
                displayName = displayNameIndex
                    .takeIf { index -> index >= 0 }
                    ?.let(it::getString),
                mimeType = mimeType,
                sizeBytes = sizeIndex
                    .takeIf { index -> index >= 0 && !it.isNull(index) }
                    ?.let(it::getLong),
            )
        }
    }

    private companion object {
        val PROJECTION = arrayOf(
            OpenableColumns.DISPLAY_NAME,
            OpenableColumns.SIZE,
        )
    }
}

package dev.elysium.pocketarchive.core.files

import dev.elysium.pocketarchive.core.model.ArchiveItemType
import java.util.concurrent.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ContentResolverDocumentMetadataReaderTest {
    @Test
    fun pdfMetadata_isMappedToSuccess() = runBlocking {
        val reader = readerReturning(
            QueriedDocumentMetadata(
                displayName = "guide.pdf",
                mimeType = "APPLICATION/PDF",
                sizeBytes = 128L,
            ),
        )

        val result = reader.read(DOCUMENT_URI)

        assertEquals(
            DocumentMetadataReadResult.Success(
                DocumentMetadata(
                    displayName = "guide.pdf",
                    mimeType = "application/pdf",
                    sizeBytes = 128L,
                    type = ArchiveItemType.PDF,
                ),
            ),
            result,
        )
    }

    @Test
    fun imageWithUnknownSize_isMappedToSuccess() = runBlocking {
        val reader = readerReturning(
            QueriedDocumentMetadata(
                displayName = "photo.jpg",
                mimeType = "image/jpeg",
                sizeBytes = -1L,
            ),
        )

        val result = reader.read(DOCUMENT_URI)

        val metadata = (result as DocumentMetadataReadResult.Success).metadata
        assertEquals(ArchiveItemType.IMAGE, metadata.type)
        assertNull(metadata.sizeBytes)
    }

    @Test
    fun nonContentUri_isRejectedWithoutQueryingProvider() = runBlocking {
        val query = FakeDocumentMetadataQuery(result = null)
        val reader = reader(query)

        val result = reader.read("file:///tmp/guide.pdf")

        assertEquals(
            failure(DocumentMetadataFailureReason.INVALID_DOCUMENT_URI),
            result,
        )
        assertEquals(0, query.callCount)
    }

    @Test
    fun missingMimeType_isReported() = runBlocking {
        val reader = readerReturning(
            QueriedDocumentMetadata(
                displayName = "guide.pdf",
                mimeType = null,
                sizeBytes = 128L,
            ),
        )

        assertEquals(
            failure(DocumentMetadataFailureReason.MIME_TYPE_UNAVAILABLE),
            reader.read(DOCUMENT_URI),
        )
    }

    @Test
    fun unsupportedMimeType_isReported() = runBlocking {
        val reader = readerReturning(
            QueriedDocumentMetadata(
                displayName = "notes.txt",
                mimeType = "text/plain",
                sizeBytes = 128L,
            ),
        )

        assertEquals(
            failure(DocumentMetadataFailureReason.UNSUPPORTED_DOCUMENT_TYPE),
            reader.read(DOCUMENT_URI),
        )
    }

    @Test
    fun missingDisplayName_isReportedAsUnavailableMetadata() = runBlocking {
        val reader = readerReturning(
            QueriedDocumentMetadata(
                displayName = "   ",
                mimeType = "application/pdf",
                sizeBytes = 128L,
            ),
        )

        assertEquals(
            failure(DocumentMetadataFailureReason.METADATA_UNAVAILABLE),
            reader.read(DOCUMENT_URI),
        )
    }

    @Test
    fun providerSecurityException_isReportedAsAccessDenied() = runBlocking {
        val reader = reader(
            FakeDocumentMetadataQuery(failure = SecurityException("denied")),
        )

        assertEquals(
            failure(DocumentMetadataFailureReason.ACCESS_DENIED),
            reader.read(DOCUMENT_URI),
        )
    }

    @Test
    fun providerException_isReportedAsUnavailableMetadata() = runBlocking {
        val reader = reader(
            FakeDocumentMetadataQuery(failure = IllegalStateException("provider failed")),
        )

        assertEquals(
            failure(DocumentMetadataFailureReason.METADATA_UNAVAILABLE),
            reader.read(DOCUMENT_URI),
        )
    }

    @Test
    fun coroutineCancellation_isNotConvertedToFailure() {
        val reader = reader(
            FakeDocumentMetadataQuery(failure = CancellationException("cancelled")),
        )

        assertThrows(CancellationException::class.java) {
            runBlocking { reader.read(DOCUMENT_URI) }
        }
    }

    private fun readerReturning(
        metadata: QueriedDocumentMetadata?,
    ): ContentResolverDocumentMetadataReader = reader(
        FakeDocumentMetadataQuery(result = metadata),
    )

    private fun reader(
        query: DocumentMetadataQuery,
    ) = ContentResolverDocumentMetadataReader(
        metadataQuery = query,
        ioDispatcher = Dispatchers.Unconfined,
    )

    private fun failure(
        reason: DocumentMetadataFailureReason,
    ) = DocumentMetadataReadResult.Failure(reason)

    private class FakeDocumentMetadataQuery(
        private val result: QueriedDocumentMetadata? = null,
        private val failure: Exception? = null,
    ) : DocumentMetadataQuery {
        var callCount = 0
            private set

        override fun query(documentUri: String): QueriedDocumentMetadata? {
            callCount += 1
            failure?.let { throw it }
            return result
        }
    }

    private companion object {
        const val DOCUMENT_URI = "content://documents/guide.pdf"
    }
}

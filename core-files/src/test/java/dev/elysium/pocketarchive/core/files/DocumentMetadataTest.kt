package dev.elysium.pocketarchive.core.files

import dev.elysium.pocketarchive.core.model.ArchiveItemType
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class DocumentMetadataTest {
    @Test
    fun unknownFileSize_isValid() {
        val metadata = metadata(sizeBytes = null)

        assertNull(metadata.sizeBytes)
    }

    @Test
    fun blankDisplayName_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            metadata(displayName = "   ")
        }
    }

    @Test
    fun blankMimeType_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            metadata(mimeType = "")
        }
    }

    @Test
    fun negativeFileSize_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            metadata(sizeBytes = -1L)
        }
    }

    private fun metadata(
        displayName: String = "guide.pdf",
        mimeType: String = "application/pdf",
        sizeBytes: Long? = 128L,
    ) = DocumentMetadata(
        displayName = displayName,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        type = ArchiveItemType.PDF,
    )
}

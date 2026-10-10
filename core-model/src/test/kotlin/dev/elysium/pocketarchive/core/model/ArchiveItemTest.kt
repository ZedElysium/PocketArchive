package dev.elysium.pocketarchive.core.model

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ArchiveItemTest {
    @Test
    fun validPdf_preservesMetadata() {
        val importedAt = Instant.parse("2026-10-10T08:00:00Z")

        val item = archiveItem(
            type = ArchiveItemType.PDF,
            mimeType = "application/pdf",
            sizeBytes = 42L,
            importedAt = importedAt,
        )

        assertEquals(ArchiveItemId("item-1"), item.id)
        assertEquals("document.pdf", item.displayName)
        assertEquals(ArchiveItemType.PDF, item.type)
        assertEquals("application/pdf", item.mimeType)
        assertEquals(42L, item.sizeBytes)
        assertEquals(importedAt, item.importedAt)
    }

    @Test
    fun imageAndZeroByteFile_areValidDomainValues() {
        val item = archiveItem(
            type = ArchiveItemType.IMAGE,
            mimeType = "image/png",
            sizeBytes = 0L,
        )

        assertEquals(ArchiveItemType.IMAGE, item.type)
        assertEquals(0L, item.sizeBytes)
    }

    @Test
    fun blankId_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            ArchiveItemId("   ")
        }
    }

    @Test
    fun blankDisplayName_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            archiveItem(displayName = "   ")
        }
    }

    @Test
    fun blankMimeType_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            archiveItem(mimeType = "")
        }
    }

    @Test
    fun negativeFileSize_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            archiveItem(sizeBytes = -1L)
        }
    }

    private fun archiveItem(
        displayName: String = "document.pdf",
        type: ArchiveItemType = ArchiveItemType.PDF,
        mimeType: String = "application/pdf",
        sizeBytes: Long = 42L,
        importedAt: Instant = Instant.parse("2026-10-10T08:00:00Z"),
    ) = ArchiveItem(
        id = ArchiveItemId("item-1"),
        displayName = displayName,
        type = type,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        importedAt = importedAt,
    )
}

package dev.elysium.pocketarchive.core.database

import dev.elysium.pocketarchive.core.model.ArchiveItem
import dev.elysium.pocketarchive.core.model.ArchiveItemId
import dev.elysium.pocketarchive.core.model.ArchiveItemType
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ArchiveItemEntityTest {
    @Test
    fun domainItem_mapsToStableDatabaseColumns() {
        assertEquals(
            ArchiveItemEntity(
                id = "item-1",
                displayName = "guide.pdf",
                type = "PDF",
                mimeType = "application/pdf",
                sizeBytes = 321L,
                importedAtEpochMillis = 1_782_025_323_456L,
                privateDocumentKey = "items/document-1",
            ),
            ARCHIVE_ITEM.toEntity(privateDocumentKey = "items/document-1"),
        )
    }

    @Test
    fun entity_roundTripsToDomainItem() {
        val entity = ARCHIVE_ITEM.toEntity(privateDocumentKey = "items/document-1")

        assertEquals(ARCHIVE_ITEM, entity.toDomain())
        assertEquals("items/document-1", entity.privateDocumentKey)
    }

    @Test
    fun entity_rejectsUnknownArchiveType() {
        assertThrows(IllegalArgumentException::class.java) {
            validEntity().copy(type = "AUDIO")
        }
    }

    @Test
    fun entity_rejectsBlankPrivateDocumentKey() {
        assertThrows(IllegalArgumentException::class.java) {
            ARCHIVE_ITEM.toEntity(privateDocumentKey = " ")
        }
    }

    private fun validEntity() = ARCHIVE_ITEM.toEntity(
        privateDocumentKey = "items/document-1",
    )

    private companion object {
        val ARCHIVE_ITEM = ArchiveItem(
            id = ArchiveItemId("item-1"),
            displayName = "guide.pdf",
            type = ArchiveItemType.PDF,
            mimeType = "application/pdf",
            sizeBytes = 321L,
            importedAt = Instant.ofEpochMilli(1_782_025_323_456L),
        )
    }
}

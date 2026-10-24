package dev.elysium.pocketarchive.core.data

import dev.elysium.pocketarchive.core.database.ArchiveItemDao
import dev.elysium.pocketarchive.core.database.ArchiveItemEntity
import dev.elysium.pocketarchive.core.files.PrivateDocumentKey
import dev.elysium.pocketarchive.core.files.StoredDocument
import dev.elysium.pocketarchive.core.model.ArchiveItem
import dev.elysium.pocketarchive.core.model.ArchiveItemId
import dev.elysium.pocketarchive.core.model.ArchiveItemType
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomArchiveRepositoryTest {
    private val dao = RecordingArchiveItemDao()
    private val repository = RoomArchiveRepository(dao)

    @Test
    fun addImportedItem_mapsDomainAndPrivateDocumentReference() = runBlocking {
        repository.addImportedItem(ARCHIVE_ITEM, STORED_DOCUMENT)

        assertEquals(
            ArchiveItemEntity(
                id = "item-1",
                displayName = "guide.pdf",
                type = "PDF",
                mimeType = "application/pdf",
                sizeBytes = 321L,
                importedAtEpochMillis = 1_782_777_600_000L,
                privateDocumentKey = "items/document-1",
            ),
            dao.insertedItem,
        )
    }

    @Test
    fun observeAll_mapsEntitiesToDomainItems() = runBlocking {
        dao.items.value = listOf(
            ArchiveItemEntity(
                id = "item-1",
                displayName = "guide.pdf",
                type = "PDF",
                mimeType = "application/pdf",
                sizeBytes = 321L,
                importedAtEpochMillis = 1_782_777_600_000L,
                privateDocumentKey = "items/document-1",
            ),
        )

        assertEquals(listOf(ARCHIVE_ITEM), repository.observeAll().first())
    }

    private class RecordingArchiveItemDao : ArchiveItemDao {
        val items = MutableStateFlow<List<ArchiveItemEntity>>(emptyList())
        var insertedItem: ArchiveItemEntity? = null

        override suspend fun insert(item: ArchiveItemEntity) {
            insertedItem = item
        }

        override fun observeAll(): Flow<List<ArchiveItemEntity>> = items

        override suspend fun findById(id: String): ArchiveItemEntity? =
            items.value.firstOrNull { it.id == id }
    }

    private companion object {
        val ARCHIVE_ITEM = ArchiveItem(
            id = ArchiveItemId("item-1"),
            displayName = "guide.pdf",
            type = ArchiveItemType.PDF,
            mimeType = "application/pdf",
            sizeBytes = 321L,
            importedAt = Instant.parse("2026-06-30T00:00:00Z"),
        )
        val STORED_DOCUMENT = StoredDocument(
            key = PrivateDocumentKey("items/document-1"),
            sizeBytes = 321L,
        )
    }
}

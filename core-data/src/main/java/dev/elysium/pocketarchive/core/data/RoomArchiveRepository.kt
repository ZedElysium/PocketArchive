package dev.elysium.pocketarchive.core.data

import dev.elysium.pocketarchive.core.database.ArchiveItemDao
import dev.elysium.pocketarchive.core.database.toDomain
import dev.elysium.pocketarchive.core.database.toEntity
import dev.elysium.pocketarchive.core.files.StoredDocument
import dev.elysium.pocketarchive.core.model.ArchiveItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomArchiveRepository(
    private val archiveItemDao: ArchiveItemDao,
) : ArchiveRepository {
    override suspend fun addImportedItem(
        archiveItem: ArchiveItem,
        storedDocument: StoredDocument,
    ) {
        archiveItemDao.insert(
            archiveItem.toEntity(
                privateDocumentKey = storedDocument.key.value,
            ),
        )
    }

    override fun observeAll(): Flow<List<ArchiveItem>> =
        archiveItemDao.observeAll().map { entities ->
            entities.map { entity -> entity.toDomain() }
        }
}

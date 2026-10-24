package dev.elysium.pocketarchive.core.data

import dev.elysium.pocketarchive.core.files.StoredDocument
import dev.elysium.pocketarchive.core.model.ArchiveItem
import kotlinx.coroutines.flow.Flow

interface ArchiveRepository {
    suspend fun addImportedItem(
        archiveItem: ArchiveItem,
        storedDocument: StoredDocument,
    )

    fun observeAll(): Flow<List<ArchiveItem>>
}

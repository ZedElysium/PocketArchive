package dev.elysium.pocketarchive.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.elysium.pocketarchive.core.model.ArchiveItem
import dev.elysium.pocketarchive.core.model.ArchiveItemId
import dev.elysium.pocketarchive.core.model.ArchiveItemType
import java.time.Instant

@Entity(
    tableName = "archive_items",
    indices = [
        Index(
            value = ["imported_at_epoch_millis", "id"],
            name = "index_archive_items_imported_at_id",
        ),
    ],
)
data class ArchiveItemEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    @ColumnInfo(name = "type")
    val type: String,
    @ColumnInfo(name = "mime_type")
    val mimeType: String,
    @ColumnInfo(name = "size_bytes")
    val sizeBytes: Long,
    @ColumnInfo(name = "imported_at_epoch_millis")
    val importedAtEpochMillis: Long,
    @ColumnInfo(name = "private_document_key")
    val privateDocumentKey: String,
) {
    init {
        require(id.isNotBlank()) { "Archive item ID must not be blank" }
        require(displayName.isNotBlank()) { "Display name must not be blank" }
        require(ArchiveItemType.entries.any { it.name == type }) {
            "Archive item type is not supported"
        }
        require(mimeType.isNotBlank()) { "MIME type must not be blank" }
        require(sizeBytes >= 0) { "File size must not be negative" }
        require(privateDocumentKey.isNotBlank()) { "Private document key must not be blank" }
    }
}

fun ArchiveItem.toEntity(privateDocumentKey: String): ArchiveItemEntity = ArchiveItemEntity(
    id = id.value,
    displayName = displayName,
    type = type.name,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    importedAtEpochMillis = importedAt.toEpochMilli(),
    privateDocumentKey = privateDocumentKey,
)

fun ArchiveItemEntity.toDomain(): ArchiveItem = ArchiveItem(
    id = ArchiveItemId(id),
    displayName = displayName,
    type = ArchiveItemType.valueOf(type),
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    importedAt = Instant.ofEpochMilli(importedAtEpochMillis),
)

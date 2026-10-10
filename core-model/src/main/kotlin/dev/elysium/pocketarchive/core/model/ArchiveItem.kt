package dev.elysium.pocketarchive.core.model

import java.time.Instant

data class ArchiveItem(
    val id: ArchiveItemId,
    val displayName: String,
    val type: ArchiveItemType,
    val mimeType: String,
    val sizeBytes: Long,
    val importedAt: Instant,
) {
    init {
        require(displayName.isNotBlank()) { "Display name must not be blank" }
        require(mimeType.isNotBlank()) { "MIME type must not be blank" }
        require(sizeBytes >= 0) { "File size must not be negative" }
    }
}

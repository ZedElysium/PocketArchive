package dev.elysium.pocketarchive.core.files

import dev.elysium.pocketarchive.core.model.ArchiveItemId
import java.util.UUID

class UuidArchiveItemIdGenerator : ArchiveItemIdGenerator {
    override fun generate(): ArchiveItemId = ArchiveItemId(UUID.randomUUID().toString())
}

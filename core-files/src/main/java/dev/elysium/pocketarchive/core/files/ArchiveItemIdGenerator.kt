package dev.elysium.pocketarchive.core.files

import dev.elysium.pocketarchive.core.model.ArchiveItemId

fun interface ArchiveItemIdGenerator {
    fun generate(): ArchiveItemId
}

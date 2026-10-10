package dev.elysium.pocketarchive.core.model

@JvmInline
value class ArchiveItemId(val value: String) {
    init {
        require(value.isNotBlank()) { "Archive item ID must not be blank" }
    }
}

package dev.elysium.pocketarchive.feature.library

import dev.elysium.pocketarchive.core.model.ArchiveItem

data class LibraryUiState(
    val isImportEnabled: Boolean = true,
    val importStatus: LibraryImportStatus = LibraryImportStatus.Idle,
    val items: List<ArchiveItem> = emptyList(),
    val loadStatus: LibraryLoadStatus = LibraryLoadStatus.Loading,
)

enum class LibraryLoadStatus {
    Loading,
    Ready,
    Failed,
}

sealed interface LibraryImportStatus {
    data object Idle : LibraryImportStatus
    data object Importing : LibraryImportStatus

    data class Succeeded(
        val displayName: String,
    ) : LibraryImportStatus

    data class Failed(
        val error: LibraryImportError,
    ) : LibraryImportStatus
}

enum class LibraryImportError {
    UNSUPPORTED_DOCUMENT,
    ACCESS_DENIED,
    IMPORT_FAILED,
}

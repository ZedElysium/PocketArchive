package dev.elysium.pocketarchive.feature.library

data class LibraryUiState(
    val isImportEnabled: Boolean = true,
    val importStatus: LibraryImportStatus = LibraryImportStatus.Idle,
)

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

package dev.elysium.pocketarchive.feature.library

sealed interface LibraryAction {
    data object ImportClicked : LibraryAction

    data class DocumentPickerFinished(
        val documentUri: String?,
    ) : LibraryAction
}

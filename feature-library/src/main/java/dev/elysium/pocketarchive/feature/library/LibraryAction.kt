package dev.elysium.pocketarchive.feature.library

sealed interface LibraryAction {
    data object ImportClicked : LibraryAction
    data object RetryLoading : LibraryAction

    data class DocumentPickerFinished(
        val documentUri: String?,
    ) : LibraryAction
}

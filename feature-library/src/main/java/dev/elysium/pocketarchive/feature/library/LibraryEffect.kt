package dev.elysium.pocketarchive.feature.library

sealed interface LibraryEffect {
    data object OpenDocumentPicker : LibraryEffect
}

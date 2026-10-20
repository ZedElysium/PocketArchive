package dev.elysium.pocketarchive.feature.library

import dev.elysium.pocketarchive.core.files.DocumentImporter
import dev.elysium.pocketarchive.core.files.ImportDocumentResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class LibraryViewModelTest {
    private val documentImporter = RecordingDocumentImporter()
    private val viewModel = LibraryViewModel(documentImporter)

    @Test
    fun initialState_matchesCurrentLibraryCapability() {
        assertEquals(
            LibraryUiState(
                isImportEnabled = true,
                hasSelectedDocument = false,
            ),
            viewModel.uiState.value,
        )
    }

    @Test
    fun uiState_doesNotExposeMutableStateFlow() {
        assertFalse(viewModel.uiState is MutableStateFlow<*>)
    }

    @Test
    fun importClicked_emitsOpenDocumentPickerAndDisablesRepeatedRequests() = runBlocking {
        viewModel.onAction(LibraryAction.ImportClicked)

        assertEquals(
            LibraryEffect.OpenDocumentPicker,
            viewModel.effects.first(),
        )
        assertFalse(viewModel.uiState.value.isImportEnabled)
        assertFalse(viewModel.uiState.value.hasSelectedDocument)
    }

    @Test
    fun consumedEffect_isNotReplayed() = runBlocking {
        viewModel.onAction(LibraryAction.ImportClicked)
        viewModel.effects.first()

        val replayedEffect = withTimeoutOrNull(100) {
            viewModel.effects.first()
        }

        assertNull(replayedEffect)
    }

    @Test
    fun repeatedImportClicks_emitOnlyOneEffect() = runBlocking {
        viewModel.onAction(LibraryAction.ImportClicked)
        viewModel.onAction(LibraryAction.ImportClicked)

        assertEquals(
            LibraryEffect.OpenDocumentPicker,
            viewModel.effects.first(),
        )
        val duplicateEffect = withTimeoutOrNull(100) {
            viewModel.effects.first()
        }

        assertNull(duplicateEffect)
    }

    @Test
    fun cancelledDocumentPicker_reenablesImportWithoutSelection() = runBlocking {
        viewModel.onAction(LibraryAction.ImportClicked)
        viewModel.effects.first()

        viewModel.onAction(LibraryAction.DocumentPickerFinished(documentUri = null))

        assertEquals(
            LibraryUiState(
                isImportEnabled = true,
                hasSelectedDocument = false,
            ),
            viewModel.uiState.value,
        )
        assertEquals(emptyList<String>(), documentImporter.importedUris)
    }

    @Test
    fun selectedDocument_reenablesImportAndMarksPendingSelection() = runBlocking {
        viewModel.onAction(LibraryAction.ImportClicked)
        viewModel.effects.first()

        viewModel.onAction(
            LibraryAction.DocumentPickerFinished(
                documentUri = "content://documents/example.pdf",
            ),
        )

        assertEquals(
            LibraryUiState(
                isImportEnabled = true,
                hasSelectedDocument = true,
            ),
            viewModel.uiState.value,
        )
        assertEquals(emptyList<String>(), documentImporter.importedUris)
    }

    private class RecordingDocumentImporter : DocumentImporter {
        val importedUris = mutableListOf<String>()

        override suspend fun import(documentUri: String): ImportDocumentResult {
            importedUris += documentUri
            error("Import must not start during the dependency-injection milestone")
        }
    }
}

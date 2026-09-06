package dev.elysium.pocketarchive.feature.library

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class LibraryViewModelTest {
    private val viewModel = LibraryViewModel()

    @Test
    fun initialState_matchesCurrentLibraryCapability() {
        assertEquals(
            LibraryUiState(isImportEnabled = false),
            viewModel.uiState.value,
        )
    }

    @Test
    fun uiState_doesNotExposeMutableStateFlow() {
        assertFalse(viewModel.uiState is MutableStateFlow<*>)
    }

    @Test
    fun importClicked_emitsOpenDocumentPickerAndKeepsState() = runBlocking {
        viewModel.onAction(LibraryAction.ImportClicked)

        assertEquals(
            LibraryEffect.OpenDocumentPicker,
            viewModel.effects.first(),
        )
        assertFalse(viewModel.uiState.value.isImportEnabled)
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
    fun documentPickerFinished_doesNotChangeStateOrEmitEffect() = runBlocking {
        val initialState = viewModel.uiState.value

        viewModel.onAction(LibraryAction.DocumentPickerFinished(documentUri = null))
        viewModel.onAction(
            LibraryAction.DocumentPickerFinished(
                documentUri = "content://documents/example.pdf",
            ),
        )

        assertEquals(initialState, viewModel.uiState.value)
        val unexpectedEffect = withTimeoutOrNull(100) {
            viewModel.effects.first()
        }
        assertNull(unexpectedEffect)
    }
}

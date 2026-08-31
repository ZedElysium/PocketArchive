package dev.elysium.pocketarchive.feature.library

import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}

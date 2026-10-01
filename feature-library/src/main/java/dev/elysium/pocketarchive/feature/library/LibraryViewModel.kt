package dev.elysium.pocketarchive.feature.library

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class LibraryViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _effects = Channel<LibraryEffect>(capacity = Channel.BUFFERED)
    val effects: Flow<LibraryEffect> = _effects.receiveAsFlow()

    fun onAction(action: LibraryAction) {
        when (action) {
            LibraryAction.ImportClicked -> openDocumentPicker()
            is LibraryAction.DocumentPickerFinished -> finishDocumentPicker(action.documentUri)
        }
    }

    private fun openDocumentPicker() {
        if (!_uiState.value.isImportEnabled) return

        _uiState.update {
            it.copy(
                isImportEnabled = false,
                hasSelectedDocument = false,
            )
        }

        if (_effects.trySend(LibraryEffect.OpenDocumentPicker).isFailure) {
            _uiState.update { it.copy(isImportEnabled = true) }
        }
    }

    private fun finishDocumentPicker(documentUri: String?) {
        _uiState.update {
            it.copy(
                isImportEnabled = true,
                hasSelectedDocument = documentUri != null,
            )
        }
    }
}

package dev.elysium.pocketarchive.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.elysium.pocketarchive.core.files.DocumentImporter
import dev.elysium.pocketarchive.core.files.DocumentMetadataFailureReason
import dev.elysium.pocketarchive.core.files.ImportDocumentFailure
import dev.elysium.pocketarchive.core.files.ImportDocumentResult
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val documentImporter: DocumentImporter,
) : ViewModel() {
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
                importStatus = LibraryImportStatus.Idle,
            )
        }

        if (_effects.trySend(LibraryEffect.OpenDocumentPicker).isFailure) {
            _uiState.update { it.copy(isImportEnabled = true) }
        }
    }

    private fun finishDocumentPicker(documentUri: String?) {
        if (_uiState.value.importStatus == LibraryImportStatus.Importing) return

        if (documentUri == null) {
            _uiState.update {
                it.copy(
                    isImportEnabled = true,
                    importStatus = LibraryImportStatus.Idle,
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isImportEnabled = false,
                importStatus = LibraryImportStatus.Importing,
            )
        }

        viewModelScope.launch {
            val importStatus = try {
                documentImporter.import(documentUri).toImportStatus()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                LibraryImportStatus.Failed(LibraryImportError.IMPORT_FAILED)
            }

            _uiState.update {
                it.copy(
                    isImportEnabled = true,
                    importStatus = importStatus,
                )
            }
        }
    }

    private fun ImportDocumentResult.toImportStatus(): LibraryImportStatus = when (this) {
        is ImportDocumentResult.Success -> LibraryImportStatus.Succeeded(
            displayName = archiveItem.displayName,
        )

        is ImportDocumentResult.Failure -> LibraryImportStatus.Failed(reason.toUiError())
    }

    private fun ImportDocumentFailure.toUiError(): LibraryImportError = when (this) {
        is ImportDocumentFailure.MetadataRejected -> when (reason) {
            DocumentMetadataFailureReason.UNSUPPORTED_DOCUMENT_TYPE ->
                LibraryImportError.UNSUPPORTED_DOCUMENT

            DocumentMetadataFailureReason.ACCESS_DENIED -> LibraryImportError.ACCESS_DENIED
            DocumentMetadataFailureReason.INVALID_DOCUMENT_URI,
            DocumentMetadataFailureReason.MIME_TYPE_UNAVAILABLE,
            DocumentMetadataFailureReason.METADATA_UNAVAILABLE,
            -> LibraryImportError.IMPORT_FAILED
        }

        ImportDocumentFailure.MetadataReadFailed,
        ImportDocumentFailure.PreparationFailed,
        ImportDocumentFailure.CopyFailed,
        ImportDocumentFailure.FinalizationFailed,
        ImportDocumentFailure.CleanupFailed,
        ImportDocumentFailure.PersistenceFailed,
        ImportDocumentFailure.PersistenceCleanupFailed,
        -> LibraryImportError.IMPORT_FAILED
    }
}

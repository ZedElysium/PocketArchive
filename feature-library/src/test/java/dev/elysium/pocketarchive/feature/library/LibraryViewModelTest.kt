package dev.elysium.pocketarchive.feature.library

import dev.elysium.pocketarchive.core.files.DocumentImporter
import dev.elysium.pocketarchive.core.files.DocumentMetadataFailureReason
import dev.elysium.pocketarchive.core.files.ImportDocumentFailure
import dev.elysium.pocketarchive.core.files.ImportDocumentResult
import dev.elysium.pocketarchive.core.files.PrivateDocumentKey
import dev.elysium.pocketarchive.core.files.StoredDocument
import dev.elysium.pocketarchive.core.model.ArchiveItem
import dev.elysium.pocketarchive.core.model.ArchiveItemId
import dev.elysium.pocketarchive.core.model.ArchiveItemType
import java.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val documentImporter = RecordingDocumentImporter()
    private val viewModel = LibraryViewModel(documentImporter)

    @Test
    fun initialState_matchesCurrentLibraryCapability() {
        assertEquals(LibraryUiState(), viewModel.uiState.value)
    }

    @Test
    fun uiState_doesNotExposeMutableStateFlow() {
        assertFalse(viewModel.uiState is MutableStateFlow<*>)
    }

    @Test
    fun importClicked_emitsOpenDocumentPickerAndDisablesRepeatedRequests() = runTest {
        viewModel.onAction(LibraryAction.ImportClicked)

        assertEquals(LibraryEffect.OpenDocumentPicker, viewModel.effects.first())
        assertEquals(
            LibraryUiState(
                isImportEnabled = false,
                importStatus = LibraryImportStatus.Idle,
            ),
            viewModel.uiState.value,
        )
    }

    @Test
    fun consumedEffect_isNotReplayed() = runTest {
        viewModel.onAction(LibraryAction.ImportClicked)
        viewModel.effects.first()

        val replayedEffect = withTimeoutOrNull(100) {
            viewModel.effects.first()
        }

        assertNull(replayedEffect)
    }

    @Test
    fun repeatedImportClicks_emitOnlyOneEffect() = runTest {
        viewModel.onAction(LibraryAction.ImportClicked)
        viewModel.onAction(LibraryAction.ImportClicked)

        assertEquals(LibraryEffect.OpenDocumentPicker, viewModel.effects.first())
        val duplicateEffect = withTimeoutOrNull(100) {
            viewModel.effects.first()
        }

        assertNull(duplicateEffect)
    }

    @Test
    fun cancelledDocumentPicker_reenablesImportWithoutStartingImport() = runTest {
        viewModel.onAction(LibraryAction.ImportClicked)
        viewModel.effects.first()

        viewModel.onAction(LibraryAction.DocumentPickerFinished(documentUri = null))

        assertEquals(LibraryUiState(), viewModel.uiState.value)
        assertEquals(emptyList<String>(), documentImporter.importedUris)
    }

    @Test
    fun selectedDocument_entersImportingAndPreventsConcurrentImport() = runTest {
        val gate = CompletableDeferred<Unit>()
        documentImporter.gate = gate

        viewModel.onAction(LibraryAction.DocumentPickerFinished(DOCUMENT_URI))

        assertEquals(
            LibraryUiState(
                isImportEnabled = false,
                importStatus = LibraryImportStatus.Importing,
            ),
            viewModel.uiState.value,
        )
        runCurrent()
        assertEquals(listOf(DOCUMENT_URI), documentImporter.importedUris)

        viewModel.onAction(LibraryAction.DocumentPickerFinished("content://documents/second.pdf"))
        runCurrent()

        assertEquals(listOf(DOCUMENT_URI), documentImporter.importedUris)
        gate.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun successfulImport_reenablesImportAndShowsDocumentName() = runTest {
        viewModel.onAction(LibraryAction.DocumentPickerFinished(DOCUMENT_URI))
        advanceUntilIdle()

        assertEquals(listOf(DOCUMENT_URI), documentImporter.importedUris)
        assertEquals(
            LibraryUiState(
                isImportEnabled = true,
                importStatus = LibraryImportStatus.Succeeded("guide.pdf"),
            ),
            viewModel.uiState.value,
        )
    }

    @Test
    fun unsupportedDocument_showsSpecificFailure() = runTest {
        documentImporter.result = failure(
            ImportDocumentFailure.MetadataRejected(
                DocumentMetadataFailureReason.UNSUPPORTED_DOCUMENT_TYPE,
            ),
        )

        viewModel.onAction(LibraryAction.DocumentPickerFinished(DOCUMENT_URI))
        advanceUntilIdle()

        assertFailedWith(LibraryImportError.UNSUPPORTED_DOCUMENT)
    }

    @Test
    fun accessDenied_showsSpecificFailure() = runTest {
        documentImporter.result = failure(
            ImportDocumentFailure.MetadataRejected(
                DocumentMetadataFailureReason.ACCESS_DENIED,
            ),
        )

        viewModel.onAction(LibraryAction.DocumentPickerFinished(DOCUMENT_URI))
        advanceUntilIdle()

        assertFailedWith(LibraryImportError.ACCESS_DENIED)
    }

    @Test
    fun storageFailure_showsGenericFailure() = runTest {
        documentImporter.result = failure(ImportDocumentFailure.CopyFailed)

        viewModel.onAction(LibraryAction.DocumentPickerFinished(DOCUMENT_URI))
        advanceUntilIdle()

        assertFailedWith(LibraryImportError.IMPORT_FAILED)
    }

    @Test
    fun unexpectedImporterException_showsGenericFailure() = runTest {
        documentImporter.exception = IllegalStateException("unexpected")

        viewModel.onAction(LibraryAction.DocumentPickerFinished(DOCUMENT_URI))
        advanceUntilIdle()

        assertFailedWith(LibraryImportError.IMPORT_FAILED)
    }

    private fun assertFailedWith(error: LibraryImportError) {
        assertEquals(
            LibraryUiState(
                isImportEnabled = true,
                importStatus = LibraryImportStatus.Failed(error),
            ),
            viewModel.uiState.value,
        )
    }

    private class RecordingDocumentImporter : DocumentImporter {
        val importedUris = mutableListOf<String>()
        var result: ImportDocumentResult = SUCCESS_RESULT
        var exception: Exception? = null
        var gate: CompletableDeferred<Unit>? = null

        override suspend fun import(documentUri: String): ImportDocumentResult {
            importedUris += documentUri
            gate?.await()
            exception?.let { throw it }
            return result
        }
    }

    private companion object {
        const val DOCUMENT_URI = "content://documents/guide.pdf"
        val SUCCESS_RESULT = ImportDocumentResult.Success(
            archiveItem = ArchiveItem(
                id = ArchiveItemId("item-1"),
                displayName = "guide.pdf",
                type = ArchiveItemType.PDF,
                mimeType = "application/pdf",
                sizeBytes = 321L,
                importedAt = Instant.parse("2026-10-20T08:00:00Z"),
            ),
            storedDocument = StoredDocument(
                key = PrivateDocumentKey("items/document-1"),
                sizeBytes = 321L,
            ),
        )

        fun failure(reason: ImportDocumentFailure) = ImportDocumentResult.Failure(reason)
    }
}

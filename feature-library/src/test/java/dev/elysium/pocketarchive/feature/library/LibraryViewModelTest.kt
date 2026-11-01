package dev.elysium.pocketarchive.feature.library

import androidx.lifecycle.ViewModelStore
import dev.elysium.pocketarchive.core.data.ArchiveRepository
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
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
    private val repository = RecordingArchiveRepository()
    private val viewModel by lazy { LibraryViewModel(documentImporter, repository) }

    @Test
    fun emptyDatabase_finishesLoadingWithoutAddingItems() = runTest {
        val model = viewModel
        repository.items.emit(emptyList())
        runCurrent()

        assertEquals(LibraryLoadStatus.Ready, model.uiState.value.loadStatus)
        assertEquals(emptyList<ArchiveItem>(), model.uiState.value.items)
    }

    @Test
    fun databaseChanges_updateItemsAndPreserveImportState() = runTest {
        val model = viewModel
        model.onAction(LibraryAction.ImportClicked)
        repository.items.emit(listOf(SUCCESS_RESULT.archiveItem))
        runCurrent()

        assertEquals(listOf(SUCCESS_RESULT.archiveItem), model.uiState.value.items)
        assertFalse(model.uiState.value.isImportEnabled)
        assertEquals(LibraryLoadStatus.Ready, model.uiState.value.loadStatus)

        repository.items.emit(emptyList())
        runCurrent()
        assertEquals(emptyList<ArchiveItem>(), model.uiState.value.items)
    }

    @Test
    fun successfulImport_doesNotInventItemsOutsideDatabase() = runTest {
        viewModel.onAction(LibraryAction.DocumentPickerFinished(DOCUMENT_URI))
        advanceUntilIdle()

        assertEquals(emptyList<ArchiveItem>(), viewModel.uiState.value.items)
        assertEquals(LibraryImportStatus.Succeeded("guide.pdf"), viewModel.uiState.value.importStatus)
    }

    @Test
    fun newViewModel_restoresExistingRepositoryItems() = runTest {
        repository.items.emit(listOf(SUCCESS_RESULT.archiveItem))
        val first = viewModel
        runCurrent()
        val recreated = LibraryViewModel(documentImporter, repository)
        runCurrent()

        assertEquals(first.uiState.value.items, recreated.uiState.value.items)
        assertEquals(listOf(SUCCESS_RESULT.archiveItem), recreated.uiState.value.items)
        assertEquals(LibraryImportStatus.Idle, recreated.uiState.value.importStatus)
    }

    @Test
    fun databaseFailure_canRetryAndLoadItems() = runTest {
        repository.failure = IllegalStateException("read failed")
        val model = viewModel
        runCurrent()
        assertEquals(LibraryLoadStatus.Failed, model.uiState.value.loadStatus)

        repository.failure = null
        repository.items.emit(listOf(SUCCESS_RESULT.archiveItem))
        model.onAction(LibraryAction.RetryLoading)
        assertEquals(LibraryLoadStatus.Loading, model.uiState.value.loadStatus)
        runCurrent()

        assertEquals(LibraryLoadStatus.Ready, model.uiState.value.loadStatus)
        assertEquals(listOf(SUCCESS_RESULT.archiveItem), model.uiState.value.items)
    }

    @Test
    fun clearedViewModel_stopsObservingRepository() = runTest {
        val store = ViewModelStore()
        store.put("library", viewModel)
        runCurrent()
        assertEquals(1, repository.items.subscriptionCount.value)

        store.clear()
        runCurrent()

        assertEquals(0, repository.items.subscriptionCount.value)
    }

    @Test
    fun failedImport_preservesLoadedItems() = runTest {
        val model = viewModel
        repository.items.emit(listOf(SUCCESS_RESULT.archiveItem))
        runCurrent()
        documentImporter.result = failure(ImportDocumentFailure.PersistenceFailed)

        model.onAction(LibraryAction.DocumentPickerFinished(DOCUMENT_URI))
        advanceUntilIdle()

        assertEquals(listOf(SUCCESS_RESULT.archiveItem), model.uiState.value.items)
        assertEquals(LibraryLoadStatus.Ready, model.uiState.value.loadStatus)
        assertEquals(
            LibraryImportStatus.Failed(LibraryImportError.IMPORT_FAILED),
            model.uiState.value.importStatus,
        )
    }

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

    private class RecordingArchiveRepository : ArchiveRepository {
        val items = MutableSharedFlow<List<ArchiveItem>>(replay = 1)
        var failure: Exception? = null

        override suspend fun addImportedItem(archiveItem: ArchiveItem, storedDocument: StoredDocument) =
            error("ViewModel must import through DocumentImporter")

        override fun observeAll(): Flow<List<ArchiveItem>> = flow {
            failure?.let { throw it }
            items.collect { emit(it) }
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

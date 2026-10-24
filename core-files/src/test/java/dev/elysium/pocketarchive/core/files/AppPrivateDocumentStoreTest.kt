package dev.elysium.pocketarchive.core.files

import dev.elysium.pocketarchive.core.model.ArchiveItemId
import dev.elysium.pocketarchive.core.model.ArchiveItemType
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.io.FileAlreadyExistsException

class AppPrivateDocumentStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var rootDirectory: File

    @Before
    fun setUp() {
        rootDirectory = temporaryFolder.newFolder("private-documents")
    }

    @Test
    fun stage_copiesContentAndReportsActualSize() = runBlocking {
        val store = storeReturning(CONTENT)

        val stagedDocument = store.stage(DOCUMENT_URI, ARCHIVE_ITEM_ID, METADATA)

        assertEquals(CONTENT.size.toLong(), stagedDocument.sizeBytes)
        assertEquals(TEST_TOKEN, stagedDocument.token)
        assertTrue(PRIVATE_KEY_PATTERN.matches(stagedDocument.destinationKey.value))
        assertArrayEquals(CONTENT, stagingFile(stagedDocument).readBytes())
        assertFalse(destinationFile(stagedDocument).exists())
    }

    @Test
    fun nonContentUri_isRejectedBeforeOpeningInput() {
        val store = storeOpening {
            throw AssertionError("Input opener must not be called")
        }

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                store.stage("file:///tmp/guide.pdf", ARCHIVE_ITEM_ID, METADATA)
            }
        }
    }

    @Test
    fun commit_atomicallyPublishesStagedContent() = runBlocking {
        val store = storeReturning(CONTENT)
        val stagedDocument = store.stage(DOCUMENT_URI, ARCHIVE_ITEM_ID, METADATA)

        val storedDocument = store.commit(stagedDocument)

        assertEquals(stagedDocument.destinationKey, storedDocument.key)
        assertEquals(CONTENT.size.toLong(), storedDocument.sizeBytes)
        assertFalse(stagingFile(stagedDocument).exists())
        assertArrayEquals(CONTENT, destinationFile(stagedDocument).readBytes())
    }

    @Test
    fun discard_removesStagedContent() = runBlocking {
        val store = storeReturning(CONTENT)
        val stagedDocument = store.stage(DOCUMENT_URI, ARCHIVE_ITEM_ID, METADATA)

        store.discard(stagedDocument)

        assertFalse(stagingFile(stagedDocument).exists())
        assertFalse(destinationFile(stagedDocument).exists())
    }

    @Test
    fun delete_removesCommittedContent() = runBlocking {
        val store = storeReturning(CONTENT)
        val stagedDocument = store.stage(DOCUMENT_URI, ARCHIVE_ITEM_ID, METADATA)
        val storedDocument = store.commit(stagedDocument)

        store.delete(storedDocument)

        assertFalse(destinationFile(stagedDocument).exists())
    }

    @Test
    fun delete_isIdempotentWhenCommittedContentIsMissing() = runBlocking {
        val store = storeReturning(CONTENT)
        val missingDocument = StoredDocument(
            key = PrivateDocumentKey(validPrivateKey()),
            sizeBytes = 0L,
        )

        store.delete(missingDocument)
        store.delete(missingDocument)
    }

    @Test
    fun forgedStoredDocumentKey_cannotEscapePrivateRoot() {
        val outsideFile = File(rootDirectory.parentFile, "outside-stored").apply {
            writeText("keep")
        }
        val forgedDocument = StoredDocument(
            key = PrivateDocumentKey("../outside-stored"),
            sizeBytes = outsideFile.length(),
        )
        val store = storeReturning(CONTENT)

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { store.delete(forgedDocument) }
        }

        assertEquals("keep", outsideFile.readText())
    }

    @Test
    fun inputFailure_removesPartialStagedFile() {
        val store = storeOpening { FailingInputStream() }

        assertThrows(IOException::class.java) {
            runBlocking { store.stage(DOCUMENT_URI, ARCHIVE_ITEM_ID, METADATA) }
        }

        assertStagingDirectoryEmpty()
    }

    @Test
    fun cancellation_removesPartialStagedFileAndPropagates() {
        val cancellation = CancellationException("cancelled")
        val store = storeOpening { FailingInputStream(cancellation) }

        val thrown = assertThrows(CancellationException::class.java) {
            runBlocking { store.stage(DOCUMENT_URI, ARCHIVE_ITEM_ID, METADATA) }
        }

        assertEquals(cancellation, thrown)
        assertStagingDirectoryEmpty()
    }

    @Test
    fun existingDestination_isNotOverwrittenAndStageRemainsDiscardable() = runBlocking {
        val store = storeReturning(CONTENT)
        val stagedDocument = store.stage(DOCUMENT_URI, ARCHIVE_ITEM_ID, METADATA)
        val destination = destinationFile(stagedDocument).apply {
            parentFile?.mkdirs()
            writeBytes(EXISTING_CONTENT)
        }

        assertThrows(FileAlreadyExistsException::class.java) {
            runBlocking { store.commit(stagedDocument) }
        }

        assertArrayEquals(EXISTING_CONTENT, destination.readBytes())
        assertTrue(stagingFile(stagedDocument).exists())
    }

    @Test
    fun changedStagedSize_preventsCommitAndLeavesStageDiscardable() = runBlocking {
        val store = storeReturning(CONTENT)
        val stagedDocument = store.stage(DOCUMENT_URI, ARCHIVE_ITEM_ID, METADATA)
        stagingFile(stagedDocument).appendBytes(byteArrayOf(0x01))

        assertThrows(IOException::class.java) {
            runBlocking { store.commit(stagedDocument) }
        }

        assertTrue(stagingFile(stagedDocument).exists())
        assertFalse(destinationFile(stagedDocument).exists())
    }

    @Test
    fun forgedToken_cannotEscapeStagingDirectory() {
        val outsideFile = File(rootDirectory, "outside").apply { writeText("keep") }
        val forgedDocument = StagedDocument(
            token = StagedDocumentToken("../outside"),
            destinationKey = PrivateDocumentKey(validPrivateKey()),
            sizeBytes = outsideFile.length(),
        )
        val store = storeReturning(CONTENT)

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { store.discard(forgedDocument) }
        }

        assertEquals("keep", outsideFile.readText())
    }

    @Test
    fun forgedDestinationKey_cannotEscapePrivateRoot() = runBlocking {
        val store = storeReturning(CONTENT)
        val stagedDocument = store.stage(DOCUMENT_URI, ARCHIVE_ITEM_ID, METADATA)
        val outsideFile = File(rootDirectory.parentFile, "outside").apply {
            writeText("keep")
        }
        val forgedDocument = stagedDocument.copy(
            destinationKey = PrivateDocumentKey("../outside"),
        )

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { store.commit(forgedDocument) }
        }

        assertEquals("keep", outsideFile.readText())
        assertTrue(stagingFile(stagedDocument).exists())
    }

    private fun storeReturning(content: ByteArray): AppPrivateDocumentStore =
        storeOpening { ByteArrayInputStream(content) }

    private fun storeOpening(
        open: () -> InputStream,
    ) = AppPrivateDocumentStore(
        inputStreamOpener = DocumentInputStreamOpener { open() },
        rootDirectory = rootDirectory,
        ioDispatcher = Dispatchers.Unconfined,
        tokenGenerator = StagedDocumentTokenGenerator { TEST_TOKEN },
    )

    private fun stagingFile(stagedDocument: StagedDocument) = File(
        File(rootDirectory, STAGING_DIRECTORY_NAME),
        stagedDocument.token.value,
    )

    private fun destinationFile(stagedDocument: StagedDocument) =
        File(rootDirectory, stagedDocument.destinationKey.value)

    private fun assertStagingDirectoryEmpty() {
        val files = File(rootDirectory, STAGING_DIRECTORY_NAME).listFiles().orEmpty()
        assertTrue(files.isEmpty())
    }

    private class FailingInputStream(
        private val failure: Exception = IOException("read failed"),
    ) : InputStream() {
        private var hasReturnedContent = false

        override fun read(): Int = throw UnsupportedOperationException()

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (hasReturnedContent) throw failure
            hasReturnedContent = true
            PARTIAL_CONTENT.copyInto(buffer, offset)
            return PARTIAL_CONTENT.size
        }
    }

    private companion object {
        const val DOCUMENT_URI = "content://documents/guide.pdf"
        val ARCHIVE_ITEM_ID = ArchiveItemId("item-1")
        val CONTENT = "private document content".toByteArray()
        val EXISTING_CONTENT = "existing".toByteArray()
        val PARTIAL_CONTENT = "partial".toByteArray()
        val TEST_TOKEN = StagedDocumentToken("stage-1")
        val METADATA = DocumentMetadata(
            displayName = "guide.pdf",
            mimeType = "application/pdf",
            sizeBytes = null,
            type = ArchiveItemType.PDF,
        )
        val PRIVATE_KEY_PATTERN = Regex("items/[0-9a-f]{64}")

        fun validPrivateKey(): String = "items/" + "a".repeat(64)
    }
}

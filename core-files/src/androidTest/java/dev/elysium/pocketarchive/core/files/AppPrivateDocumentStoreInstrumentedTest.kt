package dev.elysium.pocketarchive.core.files

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.elysium.pocketarchive.core.model.ArchiveItemId
import dev.elysium.pocketarchive.core.model.ArchiveItemType
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppPrivateDocumentStoreInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val rootDirectory = File(
        context.noBackupFilesDir,
        APP_PRIVATE_DOCUMENT_ROOT_DIRECTORY_NAME,
    )

    @Before
    fun setUp() {
        rootDirectory.deleteRecursively()
    }

    @After
    fun tearDown() {
        rootDirectory.deleteRecursively()
    }

    @Test
    fun contentResolverDocument_isCopiedAndCommittedToPrivateStorage() = runBlocking {
        val store = AppPrivateDocumentStore(context)

        val stagedDocument = store.stage(
            documentUri = TestDocumentProvider.DOCUMENT_URI,
            archiveItemId = ArchiveItemId("instrumented-item"),
            metadata = METADATA,
        )
        val storedDocument = store.commit(stagedDocument)

        val destinationFile = File(rootDirectory, storedDocument.key.value)
        assertEquals(TestDocumentProvider.CONTENT.size.toLong(), storedDocument.sizeBytes)
        assertArrayEquals(TestDocumentProvider.CONTENT, destinationFile.readBytes())
        assertFalse(
            File(
                File(rootDirectory, STAGING_DIRECTORY_NAME),
                stagedDocument.token.value,
            ).exists(),
        )
    }

    private companion object {
        val METADATA = DocumentMetadata(
            displayName = "provider.pdf",
            mimeType = "application/pdf",
            sizeBytes = null,
            type = ArchiveItemType.PDF,
        )
    }
}

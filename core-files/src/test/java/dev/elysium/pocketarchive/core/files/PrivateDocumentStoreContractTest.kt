package dev.elysium.pocketarchive.core.files

import org.junit.Assert.assertThrows
import org.junit.Test

class PrivateDocumentStoreContractTest {
    @Test
    fun blankStagedToken_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            StagedDocumentToken("   ")
        }
    }

    @Test
    fun negativeStagedSize_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            StagedDocument(
                token = StagedDocumentToken("stage-1"),
                destinationKey = PrivateDocumentKey("items/document-1"),
                sizeBytes = -1L,
            )
        }
    }

    @Test
    fun blankPrivateDocumentKey_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            PrivateDocumentKey("")
        }
    }

    @Test
    fun negativeStoredSize_isRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            StoredDocument(
                key = PrivateDocumentKey("documents/item-1.pdf"),
                sizeBytes = -1L,
            )
        }
    }
}

package dev.elysium.pocketarchive.core.files

import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Test

class UuidArchiveItemIdGeneratorTest {
    @Test
    fun generatedId_containsValidUuid() {
        val generatedId = UuidArchiveItemIdGenerator().generate()

        assertEquals(generatedId.value, UUID.fromString(generatedId.value).toString())
    }
}

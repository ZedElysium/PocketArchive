package dev.elysium.pocketarchive.core.database

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ArchiveItemDaoTest {
    private lateinit var database: PocketArchiveDatabase
    private lateinit var dao: ArchiveItemDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            PocketArchiveDatabase::class.java,
        ).build()
        dao = database.archiveItemDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insert_thenFindById_returnsStoredEntity() = runBlocking {
        val entity = entity(id = "item-1")

        dao.insert(entity)

        assertEquals(entity, dao.findById("item-1"))
    }

    @Test
    fun findById_returnsNullWhenItemDoesNotExist() = runBlocking {
        assertNull(dao.findById("missing"))
    }

    @Test
    fun duplicateId_abortsInsteadOfOverwriting() = runBlocking {
        val original = entity(id = "item-1", displayName = "original.pdf")
        dao.insert(original)

        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking {
                dao.insert(entity(id = "item-1", displayName = "replacement.pdf"))
            }
        }
        assertEquals(original, dao.findById("item-1"))
    }

    @Test
    fun observeAll_ordersNewestFirstAndUsesIdAsStableTieBreaker() = runBlocking {
        dao.insert(entity(id = "item-b", importedAtEpochMillis = 1_000L))
        dao.insert(entity(id = "item-newest", importedAtEpochMillis = 2_000L))
        dao.insert(entity(id = "item-a", importedAtEpochMillis = 1_000L))

        assertEquals(
            listOf("item-newest", "item-a", "item-b"),
            dao.observeAll().first().map(ArchiveItemEntity::id),
        )
    }

    @Test
    fun observeAll_emitsAgainAfterInsert() = runBlocking {
        val emissions = Channel<List<ArchiveItemEntity>>(capacity = Channel.UNLIMITED)
        val collection = launch {
            dao.observeAll().collect(emissions::send)
        }

        try {
            assertEquals(emptyList<ArchiveItemEntity>(), emissions.receive())
            val entity = entity(id = "item-1")

            dao.insert(entity)

            assertEquals(listOf(entity), emissions.receive())
        } finally {
            collection.cancelAndJoin()
            emissions.close()
        }
    }

    private fun entity(
        id: String,
        displayName: String = "$id.pdf",
        importedAtEpochMillis: Long = 1_000L,
    ) = ArchiveItemEntity(
        id = id,
        displayName = displayName,
        type = "PDF",
        mimeType = "application/pdf",
        sizeBytes = 321L,
        importedAtEpochMillis = importedAtEpochMillis,
        privateDocumentKey = "items/$id",
    )
}

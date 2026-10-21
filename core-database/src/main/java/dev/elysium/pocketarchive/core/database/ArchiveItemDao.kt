package dev.elysium.pocketarchive.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ArchiveItemDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: ArchiveItemEntity)

    @Query(
        """
        SELECT * FROM archive_items
        ORDER BY imported_at_epoch_millis DESC, id ASC
        """,
    )
    fun observeAll(): Flow<List<ArchiveItemEntity>>

    @Query("SELECT * FROM archive_items WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): ArchiveItemEntity?
}

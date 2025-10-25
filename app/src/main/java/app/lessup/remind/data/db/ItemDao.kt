package app.lessup.remind.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items ORDER BY CASE WHEN expiryAt IS NULL THEN 1 ELSE 0 END, expiryAt ASC")
    fun observeAll(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getById(id: Long): ItemEntity?

    @Query("SELECT * FROM items")
    suspend fun getAll(): List<ItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ItemEntity): Long

    @Update
    suspend fun update(entity: ItemEntity)

    @Delete
    suspend fun delete(entity: ItemEntity)
}

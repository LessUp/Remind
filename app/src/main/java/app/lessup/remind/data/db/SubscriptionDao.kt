package app.lessup.remind.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions ORDER BY endAt ASC")
    fun observeAll(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE id = :id")
    suspend fun getById(id: Long): SubscriptionEntity?

    @Query("SELECT * FROM subscriptions")
    suspend fun getAll(): List<SubscriptionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SubscriptionEntity): Long

    @Update
    suspend fun update(entity: SubscriptionEntity)

    @Delete
    suspend fun delete(entity: SubscriptionEntity)
}

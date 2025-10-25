package app.lessup.remind.data.repo

import app.lessup.remind.data.db.SubscriptionDao
import app.lessup.remind.data.db.SubscriptionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SubscriptionRepository @Inject constructor(
    private val dao: SubscriptionDao,
) {
    fun observeAll(): Flow<List<SubscriptionEntity>> = dao.observeAll()
    suspend fun get(id: Long) = dao.getById(id)
    suspend fun all(): List<SubscriptionEntity> = dao.getAll()
    suspend fun add(entity: SubscriptionEntity) = dao.insert(entity)
    suspend fun update(entity: SubscriptionEntity) = dao.update(entity)
    suspend fun delete(entity: SubscriptionEntity) = dao.delete(entity)
}

package app.lessup.remind.data.repo

import app.lessup.remind.data.db.ItemDao
import app.lessup.remind.data.db.ItemEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ItemRepository @Inject constructor(
    private val dao: ItemDao,
) {
    fun observeAll(): Flow<List<ItemEntity>> = dao.observeAll()
    suspend fun get(id: Long) = dao.getById(id)
    suspend fun all(): List<ItemEntity> = dao.getAll()
    suspend fun add(entity: ItemEntity) = dao.insert(entity)
    suspend fun update(entity: ItemEntity) = dao.update(entity)
    suspend fun delete(entity: ItemEntity) = dao.delete(entity)
}

package app.lessup.remind.data.db

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeItemDao : ItemDao {
    private val items = linkedMapOf<Long, ItemEntity>()
    private var nextId = 1L
    private val state = MutableStateFlow<List<ItemEntity>>(emptyList())

    override fun observeAll(): Flow<List<ItemEntity>> = state

    override suspend fun getById(id: Long): ItemEntity? = items[id]

    override suspend fun getAll(): List<ItemEntity> = items.values.toList()

    override suspend fun insert(entity: ItemEntity): Long {
        val id = if (entity.id == 0L) nextId++ else entity.id
        val stored = entity.copy(id = id)
        items[id] = stored
        if (id >= nextId) {
            nextId = id + 1
        }
        emit()
        return id
    }

    override suspend fun update(entity: ItemEntity) {
        items[entity.id] = entity
        emit()
    }

    override suspend fun delete(entity: ItemEntity) {
        items.remove(entity.id)
        emit()
    }

    private fun emit() {
        state.value = items.values.toList()
    }
}

class FakeSubscriptionDao : SubscriptionDao {
    private val subs = linkedMapOf<Long, SubscriptionEntity>()
    private var nextId = 1L
    private val state = MutableStateFlow<List<SubscriptionEntity>>(emptyList())

    override fun observeAll(): Flow<List<SubscriptionEntity>> = state

    override suspend fun getById(id: Long): SubscriptionEntity? = subs[id]

    override suspend fun getAll(): List<SubscriptionEntity> = subs.values.toList()

    override suspend fun insert(entity: SubscriptionEntity): Long {
        val id = if (entity.id == 0L) nextId++ else entity.id
        val stored = entity.copy(id = id)
        subs[id] = stored
        if (id >= nextId) {
            nextId = id + 1
        }
        emit()
        return id
    }

    override suspend fun update(entity: SubscriptionEntity) {
        subs[entity.id] = entity
        emit()
    }

    override suspend fun delete(entity: SubscriptionEntity) {
        subs.remove(entity.id)
        emit()
    }

    private fun emit() {
        state.value = subs.values.toList()
    }
}

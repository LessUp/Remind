package app.lessup.remind.ui.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lessup.remind.data.db.ItemEntity
import app.lessup.remind.data.repo.ItemRepository
import app.lessup.remind.reminder.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus

@HiltViewModel
class ItemEditViewModel @Inject constructor(
    private val repo: ItemRepository,
    private val scheduler: ReminderScheduler
) : ViewModel() {

    data class Form(
        val id: Long? = null,
        val name: String = "",
        val purchasedAt: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
        val shelfLifeDays: Int? = null,
        val expiryAt: LocalDate? = null,
        val notes: String? = null,
    )

    suspend fun load(id: Long): ItemEntity? = repo.get(id)

    fun save(form: Form) {
        viewModelScope.launch {
            val now = Clock.System.now()
            val e = ItemEntity(
                id = form.id ?: 0,
                name = form.name.trim(),
                purchasedAt = form.purchasedAt,
                shelfLifeDays = form.shelfLifeDays,
                expiryAt = form.expiryAt ?: form.shelfLifeDays?.let { d -> form.purchasedAt.plus(DatePeriod(days = d)) },
                notes = form.notes?.trim().takeIf { !it.isNullOrEmpty() },
                createdAt = now,
                updatedAt = now,
            )
            if (form.id == null) {
                val newId = repo.add(e)
                scheduler.scheduleForItem(e.copy(id = newId))
            } else {
                repo.update(e)
                scheduler.scheduleForItem(e)
            }
        }
    }
}

package app.lessup.remind.ui.subs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lessup.remind.data.db.SubscriptionEntity
import app.lessup.remind.data.repo.SubscriptionRepository
import app.lessup.remind.reminder.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@HiltViewModel
class SubEditViewModel @Inject constructor(
    private val repo: SubscriptionRepository,
    private val scheduler: ReminderScheduler
) : ViewModel() {

    data class Form(
        val id: Long? = null,
        val name: String = "",
        val provider: String? = null,
        val purchasedAt: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
        val priceCents: Long = 0,
        val endAt: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
        val autoRenew: Boolean = false,
        val notes: String? = null,
    )

    suspend fun load(id: Long): SubscriptionEntity? = repo.get(id)

    fun save(form: Form) {
        viewModelScope.launch {
            val now = Clock.System.now()
            val existing = form.id?.let { repo.get(it) }
            val entity = SubscriptionEntity(
                id = form.id ?: 0,
                name = form.name.trim(),
                provider = form.provider?.trim().takeIf { !it.isNullOrEmpty() },
                purchasedAt = form.purchasedAt,
                priceCents = form.priceCents,
                endAt = form.endAt,
                autoRenew = form.autoRenew,
                notes = form.notes?.trim().takeIf { !it.isNullOrEmpty() },
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            )
            if (form.id == null) {
                val newId = repo.add(entity)
                scheduler.scheduleForSub(entity.copy(id = newId))
            } else {
                repo.update(entity)
                scheduler.scheduleForSub(entity)
            }
        }
    }
}

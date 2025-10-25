package app.lessup.remind.ui.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lessup.remind.data.db.ItemEntity
import app.lessup.remind.data.repo.ItemRepository
import app.lessup.remind.data.settings.SettingsRepository
import app.lessup.remind.reminder.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@HiltViewModel
class ItemsViewModel @Inject constructor(
    private val repo: ItemRepository,
    private val settings: SettingsRepository,
    private val scheduler: ReminderScheduler
) : ViewModel() {

    data class UiItem(
        val entity: ItemEntity,
        val daysSince: Long,
        val daysToExpire: Long?,
        val status: Status
    ) {
        enum class Status { EXPIRED, DUE, NORMAL, NO_EXPIRY }
    }

    val items = repo.observeAll()
        .combine(settings.dueThresholdDays) { list, threshold ->
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            list.map { e ->
                val daysSince = (today.toEpochDays() - e.purchasedAt.toEpochDays()).toLong()
                val daysTo = e.expiryAt?.let { (it.toEpochDays() - today.toEpochDays()).toLong() }
                val status = when {
                    e.expiryAt == null -> UiItem.Status.NO_EXPIRY
                    daysTo != null && daysTo < 0 -> UiItem.Status.EXPIRED
                    daysTo != null && daysTo <= threshold -> UiItem.Status.DUE
                    else -> UiItem.Status.NORMAL
                }
                UiItem(e, daysSince, daysTo, status)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(entity: ItemEntity) {
        viewModelScope.launch {
            repo.delete(entity)
            scheduler.cancelForItem(entity.id)
        }
    }
}

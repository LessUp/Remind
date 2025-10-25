package app.lessup.remind.ui.subs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lessup.remind.data.db.SubscriptionEntity
import app.lessup.remind.data.repo.SubscriptionRepository
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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@HiltViewModel
class SubscriptionsViewModel @Inject constructor(
    private val repo: SubscriptionRepository,
    private val settings: SettingsRepository,
    private val scheduler: ReminderScheduler
) : ViewModel() {

    data class UiSub(
        val entity: SubscriptionEntity,
        val daysLeft: Long,
        val status: Status
    ) {
        enum class Status { EXPIRED, DUE, NORMAL }
    }

    val subs = repo.observeAll()
        .combine(settings.dueThresholdDays) { list, threshold ->
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            list.map { e ->
                val daysLeft = (e.endAt.toEpochDays() - today.toEpochDays()).toLong()
                val status = when {
                    daysLeft < 0 -> UiSub.Status.EXPIRED
                    daysLeft <= threshold -> UiSub.Status.DUE
                    else -> UiSub.Status.NORMAL
                }
                UiSub(e, daysLeft, status)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(entity: SubscriptionEntity) {
        viewModelScope.launch {
            repo.delete(entity)
            scheduler.cancelForSub(entity.id)
        }
    }
}

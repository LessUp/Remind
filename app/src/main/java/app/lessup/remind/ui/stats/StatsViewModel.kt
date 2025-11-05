package app.lessup.remind.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lessup.remind.data.repo.ItemRepository
import app.lessup.remind.data.repo.SubscriptionRepository
import app.lessup.remind.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val itemsRepo: ItemRepository,
    private val subsRepo: SubscriptionRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    data class Stats(
        val threshold: Int,
        val dueItems7: Int,
        val expiredItems: Int,
        val dueSubs7: Int,
        val expiredSubs: Int,
    )

    val stats = combine(
        itemsRepo.observeAll(),
        subsRepo.observeAll(),
        settings.dueThresholdDays
    ) { items, subs, threshold ->
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val dueItems7 = items.count { it.expiryAt != null && (it.expiryAt!!.toEpochDays() - today.toEpochDays()) in 0..threshold }
        val expiredItems = items.count { it.expiryAt != null && (it.expiryAt!!.toEpochDays() - today.toEpochDays()) < 0 }
        val dueSubs7 = subs.count { (it.endAt.toEpochDays() - today.toEpochDays()) in 0..threshold }
        val expiredSubs = subs.count { (it.endAt.toEpochDays() - today.toEpochDays()) < 0 }
        Stats(threshold, dueItems7, expiredItems, dueSubs7, expiredSubs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Stats(7,0,0,0,0))
}

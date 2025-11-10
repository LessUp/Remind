package app.lessup.remind.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lessup.remind.core.analytics.MonthlyTrendCalculator
import app.lessup.remind.core.analytics.MonthlyTrendResult
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
import java.util.Locale

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

    data class MonthlyTrendUi(
        val label: String,
        val itemsExpiring: Int,
        val overdueItems: Int,
        val subscriptionsEnding: Int,
        val subscriptionCostCents: Long
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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Stats(7, 0, 0, 0, 0))

    val monthlyTrends = combine(
        itemsRepo.observeAll(),
        subsRepo.observeAll()
    ) { items, subs ->
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        MonthlyTrendCalculator.calculate(items, subs, 6, today).map { it.toUi() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun buildMonthlyTrendsCsv(months: Int = 6): String {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val trends = MonthlyTrendCalculator.calculate(itemsRepo.all(), subsRepo.all(), months, today)
        val header = listOf("month", "itemsExpiring", "overdueItems", "subscriptionsEnding", "subscriptionCostCents")
        val rows = trends.map { trend ->
            listOf(
                monthLabel(trend.monthStart),
                trend.itemsExpiring.toString(),
                trend.overdueItems.toString(),
                trend.subscriptionsEnding.toString(),
                trend.subscriptionCostCents.toString()
            )
        }
        return (sequenceOf(header) + rows.asSequence()).joinToString("\n") { row ->
            row.joinToString(",")
        }
    }

    private fun MonthlyTrendResult.toUi(): MonthlyTrendUi = MonthlyTrendUi(
        label = monthLabel(monthStart),
        itemsExpiring = itemsExpiring,
        overdueItems = overdueItems,
        subscriptionsEnding = subscriptionsEnding,
        subscriptionCostCents = subscriptionCostCents
    )

    private fun monthLabel(date: kotlinx.datetime.LocalDate): String =
        String.format(Locale.getDefault(), "%04d-%02d", date.year, date.monthNumber)
}

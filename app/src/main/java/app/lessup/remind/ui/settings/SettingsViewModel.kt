package app.lessup.remind.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lessup.remind.data.settings.SettingsRepository
import app.lessup.remind.reminder.ReminderScheduler
import app.lessup.remind.data.repo.ItemRepository
import app.lessup.remind.data.repo.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository,
    private val scheduler: ReminderScheduler,
    private val itemRepo: ItemRepository,
    private val subRepo: SubscriptionRepository,
) : ViewModel() {

    val dueThreshold = repo.dueThresholdDays.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 7)
    val reminderHour = repo.reminderHour.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 9)
    val reminderMinute = repo.reminderMinute.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        viewModelScope.launch { scheduler.scheduleDailyOverview() }
    }

    fun setThreshold(days: Int) {
        viewModelScope.launch { repo.setDueThresholdDays(days) }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            repo.setReminderTime(hour, minute)
            scheduler.scheduleDailyOverview()
        }
    }

    suspend fun buildItemsCsv(): String {
        val items = itemRepo.all()
        val header = listOf(
            "id","name","purchasedAt","shelfLifeDays","expiryAt","notes","createdAt","updatedAt"
        )
        val rows = items.map { e ->
            listOf(
                e.id.toString(),
                e.name,
                e.purchasedAt.toString(),
                e.shelfLifeDays?.toString() ?: "",
                e.expiryAt?.toString() ?: "",
                e.notes?.replace("\n", " ") ?: "",
                e.createdAt.toString(),
                e.updatedAt.toString(),
            )
        }
        return (sequenceOf(header) + rows.asSequence())
            .joinToString("\n") { it.joinToString(",") { v -> escapeCsv(v) } }
    }

    suspend fun buildSubsCsv(): String {
        val subs = subRepo.all()
        val header = listOf(
            "id","name","provider","purchasedAt","priceCNY","endAt","autoRenew","notes","createdAt","updatedAt"
        )
        val rows = subs.map { e ->
            val price = String.format(Locale.US, "%.2f", e.priceCents / 100.0)
            listOf(
                e.id.toString(),
                e.name,
                e.provider ?: "",
                e.purchasedAt.toString(),
                price,
                e.endAt.toString(),
                e.autoRenew.toString(),
                e.notes?.replace("\n", " ") ?: "",
                e.createdAt.toString(),
                e.updatedAt.toString(),
            )
        }
        return (sequenceOf(header) + rows.asSequence())
            .joinToString("\n") { it.joinToString(",") { v -> escapeCsv(v) } }
    }

    private fun escapeCsv(v: String): String {
        val needsQuote = v.contains(',') || v.contains('"') || v.contains('\n')
        val w = v.replace("\"", "\"\"")
        return if (needsQuote) "\"$w\"" else w
    }
}

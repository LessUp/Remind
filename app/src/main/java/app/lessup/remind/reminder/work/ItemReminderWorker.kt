package app.lessup.remind.reminder.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import app.lessup.remind.R
import app.lessup.remind.data.db.ItemDao
import app.lessup.remind.data.settings.SettingsRepository
import app.lessup.remind.reminder.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.flow.first

@HiltWorker
class ItemReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val itemDao: ItemDao,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_ID = "id"
    }

    override suspend fun doWork(): Result {
        val id = inputData.getLong(KEY_ID, -1)
        if (id <= 0) return Result.success()
        val e = itemDao.getById(id) ?: return Result.success()
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val daysTo = e.expiryAt?.let { (it.toEpochDays() - today.toEpochDays()) }
        val context = applicationContext
        val title = context.getString(R.string.notification_item_title)
        val purchaseDays = today.toEpochDays() - e.purchasedAt.toEpochDays()
        val text = when {
            daysTo == null -> context.getString(R.string.notification_item_no_expiry, e.name, purchaseDays)
            daysTo < 0 -> context.getString(R.string.notification_item_overdue, e.name, -daysTo)
            daysTo == 0 -> context.getString(R.string.notification_item_due_today, e.name)
            else -> context.getString(R.string.notification_item_due_future, e.name, daysTo)
        }
        val snoozeMinutes = settingsRepository.snoozeMinutes.first()
        val actions = listOf(
            NotificationHelper.buildSnoozeItemAction(context, e.id, snoozeMinutes),
            NotificationHelper.buildDisableAllAction(context)
        )
        NotificationHelper.notify(
            context,
            (2000 + id).toInt(),
            NotificationHelper.CH_ITEMS,
            title,
            text,
            actions
        )
        return Result.success()
    }
}

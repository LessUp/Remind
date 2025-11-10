package app.lessup.remind.reminder.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.lessup.remind.R
import app.lessup.remind.data.db.SubscriptionDao
import app.lessup.remind.data.settings.SettingsRepository
import app.lessup.remind.reminder.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.flow.first

@HiltWorker
class SubReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val dao: SubscriptionDao,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_ID = "id"
    }

    override suspend fun doWork(): Result {
        val id = inputData.getLong(KEY_ID, -1)
        if (id <= 0) return Result.success()
        val e = dao.getById(id) ?: return Result.success()
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val daysLeft = (e.endAt.toEpochDays() - today.toEpochDays())
        val context = applicationContext
        val title = context.getString(R.string.notification_sub_title)
        val text = when {
            daysLeft < 0 -> context.getString(R.string.notification_sub_overdue, e.name, -daysLeft)
            daysLeft == 0 -> context.getString(R.string.notification_sub_due_today, e.name)
            else -> context.getString(R.string.notification_sub_due_future, e.name, daysLeft)
        }
        val snoozeMinutes = settingsRepository.snoozeMinutes.first()
        val actions = listOf(
            NotificationHelper.buildSnoozeSubAction(context, e.id, snoozeMinutes),
            NotificationHelper.buildDisableAllAction(context)
        )
        NotificationHelper.notify(
            context,
            (3000 + id).toInt(),
            NotificationHelper.CH_SUBS,
            title,
            text,
            actions
        )
        return Result.success()
    }
}

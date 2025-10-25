package app.lessup.remind.reminder.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.lessup.remind.data.db.SubscriptionDao
import app.lessup.remind.reminder.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@HiltWorker
class SubReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val dao: SubscriptionDao
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
        val title = "会员到期提醒"
        val text = when {
            daysLeft < 0 -> "${e.name} 已过期 ${-daysLeft} 天"
            daysLeft == 0 -> "${e.name} 今天到期"
            else -> "${e.name} 剩余 ${daysLeft} 天"
        }
        NotificationHelper.notify(applicationContext, (3000 + id).toInt(), NotificationHelper.CH_SUBS, title, text)
        return Result.success()
    }
}

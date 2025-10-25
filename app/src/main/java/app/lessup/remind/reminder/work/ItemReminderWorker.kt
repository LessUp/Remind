package app.lessup.remind.reminder.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import app.lessup.remind.data.db.ItemDao
import app.lessup.remind.reminder.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@HiltWorker
class ItemReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val itemDao: ItemDao
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
        val title = "物品到期提醒"
        val text = if (daysTo == null) {
            "${e.name} 无保质期；已购 ${(today.toEpochDays() - e.purchasedAt.toEpochDays())} 天"
        } else if (daysTo < 0) {
            "${e.name} 已过期 ${-daysTo} 天"
        } else if (daysTo == 0) {
            "${e.name} 今天到期"
        } else {
            "${e.name} 距到期 ${daysTo} 天"
        }
        NotificationHelper.notify(applicationContext, (2000 + id).toInt(), NotificationHelper.CH_ITEMS, title, text)
        return Result.success()
    }
}

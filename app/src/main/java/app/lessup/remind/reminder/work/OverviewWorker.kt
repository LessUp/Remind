package app.lessup.remind.reminder.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.lessup.remind.data.db.ItemDao
import app.lessup.remind.data.db.SubscriptionDao
import app.lessup.remind.data.settings.SettingsRepository
import app.lessup.remind.reminder.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@HiltWorker
class OverviewWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val itemDao: ItemDao,
    private val subDao: SubscriptionDao,
    private val settings: SettingsRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val threshold = settings.dueThresholdDays.first()
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val items = itemDao.getAll()
        val subs = subDao.getAll()

        val dueItems = items.filter { it.expiryAt != null && (it.expiryAt!!.toEpochDays() - today.toEpochDays()) in 0..threshold }
        val expiredItems = items.filter { it.expiryAt != null && (it.expiryAt!!.toEpochDays() - today.toEpochDays()) < 0 }
        val dueSubs = subs.filter { (it.endAt.toEpochDays() - today.toEpochDays()) in 0..threshold }
        val expiredSubs = subs.filter { (it.endAt.toEpochDays() - today.toEpochDays()) < 0 }

        val title = "今日概览"
        val text = buildString {
            append("临期物品：").append(dueItems.size).append("  ·  临期会员：").append(dueSubs.size)
            append("  ·  已过期物品：").append(expiredItems.size).append("  ·  已过期会员：").append(expiredSubs.size)
        }
        NotificationHelper.notify(applicationContext, 1000, NotificationHelper.CH_OVERVIEW, title, text)
        return Result.success()
    }
}

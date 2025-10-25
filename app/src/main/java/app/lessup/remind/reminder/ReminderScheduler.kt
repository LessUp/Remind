package app.lessup.remind.reminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import app.lessup.remind.data.db.ItemDao
import app.lessup.remind.data.db.ItemEntity
import app.lessup.remind.data.db.SubscriptionDao
import app.lessup.remind.data.db.SubscriptionEntity
import app.lessup.remind.reminder.work.ItemReminderWorker
import app.lessup.remind.reminder.work.OverviewWorker
import app.lessup.remind.reminder.work.SubReminderWorker
import app.lessup.remind.data.settings.SettingsRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager,
    private val settings: SettingsRepository,
    private val itemDao: ItemDao,
    private val subDao: SubscriptionDao,
) {
    companion object {
        private const val WORK_DAILY_OVERVIEW = "work_daily_overview"
        private fun itemTag(id: Long) = "item_$id"
        private fun subTag(id: Long) = "sub_$id"
    }

    suspend fun scheduleDailyOverview() {
        val hour = settings.reminderHour.first()
        val minute = settings.reminderMinute.first()
        val initialDelay = millisUntilNextTime(hour, minute)
        val req = PeriodicWorkRequestBuilder<OverviewWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag(WORK_DAILY_OVERVIEW)
            .build()
        workManager.enqueueUniquePeriodicWork(WORK_DAILY_OVERVIEW, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    suspend fun scheduleForItem(e: ItemEntity) {
        workManager.cancelAllWorkByTag(itemTag(e.id))
        val hour = settings.reminderHour.first()
        val minute = settings.reminderMinute.first()
        val offsets = listOf(7, 3, 1, 0, -1)
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val expiry = e.expiryAt ?: return
        offsets.forEach { d ->
            val date = when {
                d > 0 -> expiry.plus(DatePeriod(days = -d))
                d == 0 -> expiry
                else -> expiry.plus(DatePeriod(days = -d))
            }
            if (date >= today) {
                enqueueItemAt(e.id, date, hour, minute)
            }
        }
    }

    suspend fun scheduleForSub(s: SubscriptionEntity) {
        workManager.cancelAllWorkByTag(subTag(s.id))
        val hour = settings.reminderHour.first()
        val minute = settings.reminderMinute.first()
        val offsets = listOf(7, 3, 1, 0, -1)
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val end = s.endAt
        offsets.forEach { d ->
            val date = when {
                d > 0 -> end.plus(DatePeriod(days = -d))
                d == 0 -> end
                else -> end.plus(DatePeriod(days = -d))
            }
            if (date >= today) {
                enqueueSubAt(s.id, date, hour, minute)
            }
        }
    }

    suspend fun cancelForItem(id: Long) {
        workManager.cancelAllWorkByTag(itemTag(id))
    }

    suspend fun cancelForSub(id: Long) {
        workManager.cancelAllWorkByTag(subTag(id))
    }

    suspend fun rebuildAll() {
        scheduleDailyOverview()
        itemDao.getAll().forEach { scheduleForItem(it) }
        subDao.getAll().forEach { scheduleForSub(it) }
    }

    private fun enqueueItemAt(id: Long, date: LocalDate, hour: Int, minute: Int) {
        val delayMs = delayMsTo(date, hour, minute)
        if (delayMs <= 0) return
        val req = OneTimeWorkRequestBuilder<ItemReminderWorker>()
            .setInputData(workDataOf(ItemReminderWorker.KEY_ID to id))
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .addTag(itemTag(id))
            .build()
        workManager.enqueueUniqueWork("item_${id}_${date}", ExistingWorkPolicy.REPLACE, req)
    }

    private fun enqueueSubAt(id: Long, date: LocalDate, hour: Int, minute: Int) {
        val delayMs = delayMsTo(date, hour, minute)
        if (delayMs <= 0) return
        val req = OneTimeWorkRequestBuilder<SubReminderWorker>()
            .setInputData(workDataOf(SubReminderWorker.KEY_ID to id))
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .addTag(subTag(id))
            .build()
        workManager.enqueueUniqueWork("sub_${id}_${date}", ExistingWorkPolicy.REPLACE, req)
    }

    private fun millisUntilNextTime(hour: Int, minute: Int): Long {
        val now = Clock.System.now()
        val tz = TimeZone.currentSystemDefault()
        val today = now.toLocalDateTime(tz).date
        val candidate = LocalDateTime(today.year, today.monthNumber, today.dayOfMonth, hour, minute)
            .toInstant(tz)
        val target = if (candidate > now) candidate else {
            val nextDay = today.plus(DatePeriod(days = 1))
            LocalDateTime(nextDay.year, nextDay.monthNumber, nextDay.dayOfMonth, hour, minute).toInstant(tz)
        }
        return target.toEpochMilliseconds() - now.toEpochMilliseconds()
    }

    private fun delayMsTo(date: LocalDate, hour: Int, minute: Int): Long {
        val now = Clock.System.now()
        val tz = TimeZone.currentSystemDefault()
        val dt = LocalDateTime(date.year, date.monthNumber, date.dayOfMonth, hour, minute).toInstant(tz)
        return dt.toEpochMilliseconds() - now.toEpochMilliseconds()
    }
}

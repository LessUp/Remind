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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
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
        if (!settings.notificationsEnabled.first() || !settings.dailyOverviewEnabled.first()) {
            workManager.cancelUniqueWork(WORK_DAILY_OVERVIEW)
            return
        }
        val hour = settings.reminderHour.first()
        val minute = settings.reminderMinute.first()
        val delay = ReminderPlanner.millisUntilNextTrigger(hour, minute, Clock.System.now(), TimeZone.currentSystemDefault())
        val req = PeriodicWorkRequestBuilder<OverviewWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(WORK_DAILY_OVERVIEW)
            .build()
        workManager.enqueueUniquePeriodicWork(WORK_DAILY_OVERVIEW, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    suspend fun scheduleForItem(e: ItemEntity) {
        workManager.cancelAllWorkByTag(itemTag(e.id))
        if (!settings.notificationsEnabled.first() || !settings.itemRemindersEnabled.first()) {
            return
        }
        val hour = settings.reminderHour.first()
        val minute = settings.reminderMinute.first()
        val expiry = e.expiryAt ?: return
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        ReminderPlanner.computeScheduleDates(expiry, listOf(7, 3, 1, 0, -1), today).forEach { date ->
            enqueueItemAt(e.id, date, hour, minute)
        }
    }

    suspend fun scheduleForSub(s: SubscriptionEntity) {
        workManager.cancelAllWorkByTag(subTag(s.id))
        if (!settings.notificationsEnabled.first() || !settings.subRemindersEnabled.first()) {
            return
        }
        val hour = settings.reminderHour.first()
        val minute = settings.reminderMinute.first()
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        ReminderPlanner.computeScheduleDates(s.endAt, listOf(7, 3, 1, 0, -1), today).forEach { date ->
            enqueueSubAt(s.id, date, hour, minute)
        }
    }

    suspend fun cancelForItem(id: Long) {
        workManager.cancelAllWorkByTag(itemTag(id))
    }

    suspend fun cancelForSub(id: Long) {
        workManager.cancelAllWorkByTag(subTag(id))
    }

    suspend fun rebuildAll() {
        if (settings.notificationsEnabled.first()) {
            scheduleDailyOverview()
            itemDao.getAll().forEach { scheduleForItem(it) }
            subDao.getAll().forEach { scheduleForSub(it) }
        } else {
            workManager.cancelUniqueWork(WORK_DAILY_OVERVIEW)
            itemDao.getAll().forEach { workManager.cancelAllWorkByTag(itemTag(it.id)) }
            subDao.getAll().forEach { workManager.cancelAllWorkByTag(subTag(it.id)) }
        }
    }

    private fun enqueueItemAt(id: Long, date: LocalDate, hour: Int, minute: Int) {
        val delayMs = ReminderPlanner.delayTo(date, hour, minute, Clock.System.now(), TimeZone.currentSystemDefault())
        if (delayMs <= 0) return
        val req = OneTimeWorkRequestBuilder<ItemReminderWorker>()
            .setInputData(workDataOf(ItemReminderWorker.KEY_ID to id))
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .addTag(itemTag(id))
            .build()
        workManager.enqueueUniqueWork("item_${id}_${date}", ExistingWorkPolicy.REPLACE, req)
    }

    private fun enqueueSubAt(id: Long, date: LocalDate, hour: Int, minute: Int) {
        val delayMs = ReminderPlanner.delayTo(date, hour, minute, Clock.System.now(), TimeZone.currentSystemDefault())
        if (delayMs <= 0) return
        val req = OneTimeWorkRequestBuilder<SubReminderWorker>()
            .setInputData(workDataOf(SubReminderWorker.KEY_ID to id))
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .addTag(subTag(id))
            .build()
        workManager.enqueueUniqueWork("sub_${id}_${date}", ExistingWorkPolicy.REPLACE, req)
    }

    suspend fun snoozeItem(id: Long) {
        if (!settings.notificationsEnabled.first() || !settings.itemRemindersEnabled.first()) return
        val minutes = settings.snoozeMinutes.first().coerceAtLeast(1)
        val req = OneTimeWorkRequestBuilder<ItemReminderWorker>()
            .setInputData(workDataOf(ItemReminderWorker.KEY_ID to id))
            .setInitialDelay(minutes.toLong(), TimeUnit.MINUTES)
            .addTag(itemTag(id))
            .build()
        workManager.enqueueUniqueWork("item_${id}_snooze", ExistingWorkPolicy.REPLACE, req)
    }

    suspend fun snoozeSub(id: Long) {
        if (!settings.notificationsEnabled.first() || !settings.subRemindersEnabled.first()) return
        val minutes = settings.snoozeMinutes.first().coerceAtLeast(1)
        val req = OneTimeWorkRequestBuilder<SubReminderWorker>()
            .setInputData(workDataOf(SubReminderWorker.KEY_ID to id))
            .setInitialDelay(minutes.toLong(), TimeUnit.MINUTES)
            .addTag(subTag(id))
            .build()
        workManager.enqueueUniqueWork("sub_${id}_snooze", ExistingWorkPolicy.REPLACE, req)
    }
}

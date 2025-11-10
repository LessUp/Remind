package app.lessup.remind.reminder.work

import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.RemoteViews
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.lessup.remind.R
import app.lessup.remind.data.db.ItemDao
import app.lessup.remind.data.db.SubscriptionDao
import app.lessup.remind.data.settings.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@HiltWorker
class ReminderWidgetUpdateWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters,
    private val itemDao: ItemDao,
    private val subscriptionDao: SubscriptionDao,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val ids = inputData.getIntArray(KEY_WIDGET_IDS) ?: return Result.success()
        val manager = AppWidgetManager.getInstance(appContext)
        val threshold = settingsRepository.dueThresholdDays.first()
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val items = itemDao.getAll()
        val subs = subscriptionDao.getAll()
        val dueItems = items.count { it.expiryAt != null && (it.expiryAt!!.toEpochDays() - today.toEpochDays()) in 0..threshold }
        val dueSubs = subs.count { (it.endAt.toEpochDays() - today.toEpochDays()) in 0..threshold }
        val views = RemoteViews(appContext.packageName, R.layout.widget_reminder_overview).apply {
            setTextViewText(R.id.widgetItems, appContext.getString(R.string.widget_items_value, dueItems))
            setTextViewText(R.id.widgetSubs, appContext.getString(R.string.widget_subs_value, dueSubs))
        }
        ids.forEach { manager.updateAppWidget(it, views) }
        return Result.success()
    }

    companion object {
        private const val KEY_WIDGET_IDS = "widget_ids"
        private const val UNIQUE_WORK = "reminder_widget_update"

        fun enqueue(context: Context, ids: IntArray) {
            val request = OneTimeWorkRequestBuilder<ReminderWidgetUpdateWorker>()
                .setInputData(Data.Builder().putIntArray(KEY_WIDGET_IDS, ids).build())
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(UNIQUE_WORK, ExistingWorkPolicy.REPLACE, request)
        }
    }
}

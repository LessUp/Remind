package app.lessup.remind.reminder

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import app.lessup.remind.reminder.work.ReminderWidgetUpdateWorker

class ReminderWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        ReminderWidgetUpdateWorker.enqueue(context, appWidgetIds)
    }
}

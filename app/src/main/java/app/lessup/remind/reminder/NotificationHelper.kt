package app.lessup.remind.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.lessup.remind.MainActivity
import app.lessup.remind.R

object NotificationHelper {
    const val CH_ITEMS = "items"
    const val CH_SUBS = "subs"
    const val CH_OVERVIEW = "overview"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(CH_ITEMS, "物品提醒", NotificationManager.IMPORTANCE_DEFAULT)
            )
            nm.createNotificationChannel(
                NotificationChannel(CH_SUBS, "会员提醒", NotificationManager.IMPORTANCE_DEFAULT)
            )
            nm.createNotificationChannel(
                NotificationChannel(CH_OVERVIEW, "每日概览", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    fun notify(
        context: Context,
        id: Int,
        channel: String,
        title: String,
        text: String,
        actions: List<NotificationCompat.Action> = emptyList()
    ) {
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openAppPendingIntent(context))
        actions.forEach { builder.addAction(it) }
        NotificationManagerCompat.from(context).notify(id, builder.build())
    }

    fun buildSnoozeItemAction(context: Context, id: Long, minutes: Int): NotificationCompat.Action =
        NotificationCompat.Action.Builder(
            0,
            context.getString(R.string.notification_snooze_action, minutes),
            actionPendingIntent(context, ReminderActionReceiver.ACTION_SNOOZE_ITEM, id)
        ).build()

    fun buildSnoozeSubAction(context: Context, id: Long, minutes: Int): NotificationCompat.Action =
        NotificationCompat.Action.Builder(
            0,
            context.getString(R.string.notification_snooze_action, minutes),
            actionPendingIntent(context, ReminderActionReceiver.ACTION_SNOOZE_SUB, id)
        ).build()

    fun buildDisableAllAction(context: Context): NotificationCompat.Action =
        NotificationCompat.Action.Builder(
            0,
            context.getString(R.string.notification_disable_action),
            actionPendingIntent(context, ReminderActionReceiver.ACTION_DISABLE_NOTIFICATIONS, null)
        ).build()

    private fun openAppPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun actionPendingIntent(context: Context, action: String, id: Long?): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java).apply {
            this.action = action
            if (id != null) putExtra(ReminderActionReceiver.EXTRA_ID, id)
        }
        val requestCode = (action.hashCode() xor (id?.hashCode() ?: 0))
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

package app.lessup.remind.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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

    fun notify(context: Context, id: Int, channel: String, title: String, text: String) {
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        NotificationManagerCompat.from(context).notify(id, builder.build())
    }
}

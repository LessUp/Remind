package app.lessup.remind.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.lessup.remind.data.settings.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReminderActionReceiver : BroadcastReceiver() {

    @Inject lateinit var scheduler: ReminderScheduler
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_SNOOZE_ITEM -> {
                        val id = intent.getLongExtra(EXTRA_ID, -1)
                        if (id > 0) scheduler.snoozeItem(id)
                    }
                    ACTION_SNOOZE_SUB -> {
                        val id = intent.getLongExtra(EXTRA_ID, -1)
                        if (id > 0) scheduler.snoozeSub(id)
                    }
                    ACTION_DISABLE_NOTIFICATIONS -> {
                        settingsRepository.setNotificationsEnabled(false)
                        scheduler.rebuildAll()
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_SNOOZE_ITEM = "app.lessup.remind.action.SNOOZE_ITEM"
        const val ACTION_SNOOZE_SUB = "app.lessup.remind.action.SNOOZE_SUB"
        const val ACTION_DISABLE_NOTIFICATIONS = "app.lessup.remind.action.DISABLE_NOTIFICATIONS"
        const val EXTRA_ID = "extra_id"
    }
}

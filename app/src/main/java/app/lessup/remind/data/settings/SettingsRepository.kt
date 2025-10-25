package app.lessup.remind.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val DUE_THRESHOLD_DAYS = intPreferencesKey("due_threshold_days")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
    }

    val dueThresholdDays: Flow<Int> = dataStore.data.map { it[Keys.DUE_THRESHOLD_DAYS] ?: 7 }
    val reminderHour: Flow<Int> = dataStore.data.map { it[Keys.REMINDER_HOUR] ?: 9 }
    val reminderMinute: Flow<Int> = dataStore.data.map { it[Keys.REMINDER_MINUTE] ?: 0 }

    suspend fun setDueThresholdDays(value: Int) {
        dataStore.edit { it[Keys.DUE_THRESHOLD_DAYS] = value }
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        dataStore.edit {
            it[Keys.REMINDER_HOUR] = hour
            it[Keys.REMINDER_MINUTE] = minute
        }
    }
}

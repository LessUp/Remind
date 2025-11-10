package app.lessup.remind.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val ITEM_REMINDERS_ENABLED = booleanPreferencesKey("item_reminders_enabled")
        val SUB_REMINDERS_ENABLED = booleanPreferencesKey("sub_reminders_enabled")
        val DAILY_OVERVIEW_ENABLED = booleanPreferencesKey("daily_overview_enabled")
        val SNOOZE_MINUTES = intPreferencesKey("snooze_minutes")
        val THEME_MODE = intPreferencesKey("theme_mode")
    }

    val dueThresholdDays: Flow<Int> = dataStore.data.map { it[Keys.DUE_THRESHOLD_DAYS] ?: 7 }
    val reminderHour: Flow<Int> = dataStore.data.map { it[Keys.REMINDER_HOUR] ?: 9 }
    val reminderMinute: Flow<Int> = dataStore.data.map { it[Keys.REMINDER_MINUTE] ?: 0 }
    val notificationsEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.NOTIFICATIONS_ENABLED] ?: true }
    val itemRemindersEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.ITEM_REMINDERS_ENABLED] ?: true }
    val subRemindersEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.SUB_REMINDERS_ENABLED] ?: true }
    val dailyOverviewEnabled: Flow<Boolean> = dataStore.data.map { it[Keys.DAILY_OVERVIEW_ENABLED] ?: true }
    val snoozeMinutes: Flow<Int> = dataStore.data.map { it[Keys.SNOOZE_MINUTES] ?: 30 }
    val themeMode: Flow<ThemeMode> = dataStore.data.map {
        val stored = it[Keys.THEME_MODE]
        ThemeMode.fromOrdinal(stored ?: ThemeMode.SYSTEM.ordinal)
    }

    suspend fun setDueThresholdDays(value: Int) {
        dataStore.edit { it[Keys.DUE_THRESHOLD_DAYS] = value }
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        dataStore.edit {
            it[Keys.REMINDER_HOUR] = hour
            it[Keys.REMINDER_MINUTE] = minute
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setItemRemindersEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.ITEM_REMINDERS_ENABLED] = enabled }
    }

    suspend fun setSubRemindersEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.SUB_REMINDERS_ENABLED] = enabled }
    }

    suspend fun setDailyOverviewEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.DAILY_OVERVIEW_ENABLED] = enabled }
    }

    suspend fun setSnoozeMinutes(minutes: Int) {
        dataStore.edit { it[Keys.SNOOZE_MINUTES] = minutes }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.ordinal }
    }
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromOrdinal(value: Int): ThemeMode = entries.getOrElse(value) { SYSTEM }
    }
}

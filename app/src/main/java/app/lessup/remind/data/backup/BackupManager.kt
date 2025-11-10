package app.lessup.remind.data.backup

import app.lessup.remind.data.db.ItemEntity
import app.lessup.remind.data.db.SubscriptionEntity
import app.lessup.remind.data.repo.ItemRepository
import app.lessup.remind.data.repo.SubscriptionRepository
import app.lessup.remind.data.settings.SettingsRepository
import app.lessup.remind.data.settings.ThemeMode
import app.lessup.remind.reminder.ReminderScheduler
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.text.Charsets
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class BackupManager @Inject constructor(
    private val itemRepository: ItemRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val settingsRepository: SettingsRepository,
    private val scheduler: ReminderScheduler
) {
    suspend fun export(password: String?): ByteArray {
        val items = itemRepository.all()
        val subs = subscriptionRepository.all()
        val settings = JSONObject().apply {
            put("dueThreshold", settingsRepository.dueThresholdDays.first())
            put("reminderHour", settingsRepository.reminderHour.first())
            put("reminderMinute", settingsRepository.reminderMinute.first())
            put("notificationsEnabled", settingsRepository.notificationsEnabled.first())
            put("itemRemindersEnabled", settingsRepository.itemRemindersEnabled.first())
            put("subRemindersEnabled", settingsRepository.subRemindersEnabled.first())
            put("dailyOverviewEnabled", settingsRepository.dailyOverviewEnabled.first())
            put("snoozeMinutes", settingsRepository.snoozeMinutes.first())
            put("themeMode", settingsRepository.themeMode.first().ordinal)
        }
        val payload = JSONObject().apply {
            put("version", 1)
            put("generatedAt", Clock.System.now().toString())
            put("settings", settings)
            put("items", JSONArray().apply { items.forEach { put(it.toJson()) } })
            put("subscriptions", JSONArray().apply { subs.forEach { put(it.toJson()) } })
        }
        val data = payload.toString().toByteArray(Charsets.UTF_8)
        return if (password.isNullOrEmpty()) {
            BackupCrypto.wrapPlain(data)
        } else {
            BackupCrypto.encrypt(data, password)
        }
    }

    suspend fun import(bytes: ByteArray, password: String?) {
        val decoded = BackupCrypto.decrypt(bytes, password)
        val payload = JSONObject(String(decoded, Charsets.UTF_8))
        val items = payload.optJSONArray("items")?.toItemEntities().orEmpty()
        val subs = payload.optJSONArray("subscriptions")?.toSubscriptionEntities().orEmpty()
        val settings = payload.optJSONObject("settings")
        if (items.isNotEmpty()) {
            itemRepository.replaceAll(items)
        } else {
            itemRepository.replaceAll(emptyList())
        }
        if (subs.isNotEmpty()) {
            subscriptionRepository.replaceAll(subs)
        } else {
            subscriptionRepository.replaceAll(emptyList())
        }
        settings?.let { json ->
            settingsRepository.setDueThresholdDays(json.optInt("dueThreshold", 7))
            settingsRepository.setReminderTime(
                json.optInt("reminderHour", 9),
                json.optInt("reminderMinute", 0)
            )
            settingsRepository.setNotificationsEnabled(json.optBoolean("notificationsEnabled", true))
            settingsRepository.setItemRemindersEnabled(json.optBoolean("itemRemindersEnabled", true))
            settingsRepository.setSubRemindersEnabled(json.optBoolean("subRemindersEnabled", true))
            settingsRepository.setDailyOverviewEnabled(json.optBoolean("dailyOverviewEnabled", true))
            settingsRepository.setSnoozeMinutes(json.optInt("snoozeMinutes", 30))
            val themeOrdinal = json.optInt("themeMode", ThemeMode.SYSTEM.ordinal)
            settingsRepository.setThemeMode(ThemeMode.fromOrdinal(themeOrdinal))
        }
        scheduler.rebuildAll()
    }
}

private fun JSONArray.toItemEntities(): List<ItemEntity> = buildList {
    for (index in 0 until length()) {
        val obj = getJSONObject(index)
        val shelfLife = if (obj.has("shelfLifeDays") && !obj.isNull("shelfLifeDays")) obj.getInt("shelfLifeDays") else null
        val notes = if (obj.has("notes") && !obj.isNull("notes")) obj.getString("notes").takeIf { it.isNotBlank() } else null
        val expiryRaw = obj.optString("expiryAt")
        add(
            ItemEntity(
                id = obj.getLong("id"),
                name = obj.getString("name"),
                purchasedAt = LocalDate.parse(obj.getString("purchasedAt")),
                shelfLifeDays = shelfLife,
                expiryAt = expiryRaw.takeIf { it.isNotBlank() }?.let(LocalDate::parse),
                notes = notes,
                createdAt = Instant.parse(obj.getString("createdAt")),
                updatedAt = Instant.parse(obj.getString("updatedAt"))
            )
        )
    }
}

private fun JSONArray.toSubscriptionEntities(): List<SubscriptionEntity> = buildList {
    for (index in 0 until length()) {
        val obj = getJSONObject(index)
        val provider = if (obj.has("provider") && !obj.isNull("provider")) obj.getString("provider").takeIf { it.isNotBlank() } else null
        val notes = if (obj.has("notes") && !obj.isNull("notes")) obj.getString("notes").takeIf { it.isNotBlank() } else null
        add(
            SubscriptionEntity(
                id = obj.getLong("id"),
                name = obj.getString("name"),
                provider = provider,
                purchasedAt = LocalDate.parse(obj.getString("purchasedAt")),
                priceCents = obj.getLong("priceCents"),
                endAt = LocalDate.parse(obj.getString("endAt")),
                autoRenew = obj.optBoolean("autoRenew", false),
                notes = notes,
                createdAt = Instant.parse(obj.getString("createdAt")),
                updatedAt = Instant.parse(obj.getString("updatedAt"))
            )
        )
    }
}

private fun ItemEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("purchasedAt", purchasedAt.toString())
    if (shelfLifeDays != null) put("shelfLifeDays", shelfLifeDays)
    put("expiryAt", expiryAt?.toString() ?: "")
    put("notes", notes ?: "")
    put("createdAt", createdAt.toString())
    put("updatedAt", updatedAt.toString())
}

private fun SubscriptionEntity.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("provider", provider ?: "")
    put("purchasedAt", purchasedAt.toString())
    put("priceCents", priceCents)
    put("endAt", endAt.toString())
    put("autoRenew", autoRenew)
    put("notes", notes ?: "")
    put("createdAt", createdAt.toString())
    put("updatedAt", updatedAt.toString())
}

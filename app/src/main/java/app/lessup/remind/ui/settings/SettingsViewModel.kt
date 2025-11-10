package app.lessup.remind.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lessup.remind.data.backup.BackupManager
import app.lessup.remind.data.db.ItemEntity
import app.lessup.remind.data.db.SubscriptionEntity
import app.lessup.remind.data.repo.ItemRepository
import app.lessup.remind.data.repo.SubscriptionRepository
import app.lessup.remind.data.settings.SettingsRepository
import app.lessup.remind.data.settings.ThemeMode
import app.lessup.remind.reminder.ReminderScheduler
import app.lessup.remind.util.CsvParser
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.InputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.text.Charsets

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository,
    private val scheduler: ReminderScheduler,
    private val itemRepo: ItemRepository,
    private val subRepo: SubscriptionRepository,
    private val backupManager: BackupManager
) : ViewModel() {

    data class SettingsUiState(
        val threshold: Int = 7,
        val reminderHour: Int = 9,
        val reminderMinute: Int = 0,
        val notificationsEnabled: Boolean = true,
        val itemRemindersEnabled: Boolean = true,
        val subRemindersEnabled: Boolean = true,
        val dailyOverviewEnabled: Boolean = true,
        val snoozeMinutes: Int = 30,
        val themeMode: ThemeMode = ThemeMode.SYSTEM
    )

    val uiState = combine(
        repo.dueThresholdDays,
        repo.reminderHour,
        repo.reminderMinute,
        repo.notificationsEnabled,
        repo.itemRemindersEnabled,
        repo.subRemindersEnabled,
        repo.dailyOverviewEnabled,
        repo.snoozeMinutes,
        repo.themeMode
    ) { threshold, hour, minute, notif, item, sub, overview, snooze, theme ->
        SettingsUiState(
            threshold = threshold,
            reminderHour = hour,
            reminderMinute = minute,
            notificationsEnabled = notif,
            itemRemindersEnabled = item,
            subRemindersEnabled = sub,
            dailyOverviewEnabled = overview,
            snoozeMinutes = snooze,
            themeMode = theme
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    init {
        viewModelScope.launch { scheduler.scheduleDailyOverview() }
    }

    fun setThreshold(days: Int) {
        viewModelScope.launch { repo.setDueThresholdDays(days) }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            repo.setReminderTime(hour, minute)
            scheduler.scheduleDailyOverview()
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repo.setNotificationsEnabled(enabled)
            scheduler.rebuildAll()
        }
    }

    fun setItemRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repo.setItemRemindersEnabled(enabled)
            scheduler.rebuildAll()
        }
    }

    fun setSubRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repo.setSubRemindersEnabled(enabled)
            scheduler.rebuildAll()
        }
    }

    fun setDailyOverviewEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repo.setDailyOverviewEnabled(enabled)
            scheduler.rebuildAll()
        }
    }

    fun setSnoozeMinutes(minutes: Int) {
        viewModelScope.launch { repo.setSnoozeMinutes(minutes) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repo.setThemeMode(mode) }
    }

    suspend fun exportBackup(password: String?): ByteArray = withContext(Dispatchers.IO) {
        val normalized = password?.takeIf { it.isNotBlank() }
        backupManager.export(normalized)
    }

    suspend fun importBackup(bytes: ByteArray, password: String?) {
        withContext(Dispatchers.IO) {
            val normalized = password?.takeIf { it.isNotBlank() }
            backupManager.import(bytes, normalized)
        }
    }

    suspend fun importItemsCsv(stream: InputStream): Int {
        val count = withContext(Dispatchers.IO) {
            val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val rows = CsvParser.parse(text)
            if (rows.isEmpty()) {
                throw IllegalArgumentException("CSV is empty")
            }
            val header = rows.first().mapIndexed { index, value ->
                val cleaned = if (index == 0) value.removePrefix("\uFEFF") else value
                cleaned.trim()
            }
            val required = listOf("id", "name", "purchasedAt", "expiryAt", "createdAt", "updatedAt")
            if (!required.all { header.contains(it) }) {
                throw IllegalArgumentException("CSV missing columns: ${required.filterNot { header.contains(it) }.joinToString()}" )
            }
            val indexMap = header.withIndex().associate { it.value to it.index }
            var imported = 0
            rows.drop(1).forEachIndexed { line, row ->
                if (row.all { it.isBlank() }) return@forEachIndexed
                val idStr = row.value(indexMap, "id", line).trim()
                val nameStr = row.value(indexMap, "name", line).trim()
                val purchasedAtStr = row.value(indexMap, "purchasedAt", line)
                val expiryAtStr = row.value(indexMap, "expiryAt", line)
                val createdAtStr = row.value(indexMap, "createdAt", line)
                val updatedAtStr = row.value(indexMap, "updatedAt", line)
                val entity = ItemEntity(
                    id = idStr.toLongOrNull()
                        ?: throw IllegalArgumentException("Cannot parse id on line ${line + 2}"),
                    name = nameStr.takeIf { it.isNotBlank() }
                        ?: throw IllegalArgumentException("Name is blank on line ${line + 2}"),
                    purchasedAt = purchasedAtStr.toLocalDate(line),
                    shelfLifeDays = row.getOptional(indexMap, "shelfLifeDays")?.trim()?.let {
                        if (it.isBlank()) null else it.toIntOrNull()
                            ?: throw IllegalArgumentException("Cannot parse shelfLifeDays on line ${line + 2}")
                    },
                    expiryAt = expiryAtStr.let {
                        if (it.isBlank()) null else it.toLocalDate(line)
                    },
                    notes = row.getOptional(indexMap, "notes")?.trim()?.takeUnless { it.isEmpty() },
                    createdAt = createdAtStr.toInstant(line),
                    updatedAt = updatedAtStr.toInstant(line),
                )
                itemRepo.add(entity)
                imported++
            }
            imported
        }
        scheduler.rebuildAll()
        return count
    }

    suspend fun importSubsCsv(stream: InputStream): Int {
        val count = withContext(Dispatchers.IO) {
            val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val rows = CsvParser.parse(text)
            if (rows.isEmpty()) {
                throw IllegalArgumentException("CSV is empty")
            }
            val header = rows.first().mapIndexed { index, value ->
                val cleaned = if (index == 0) value.removePrefix("\uFEFF") else value
                cleaned.trim()
            }
            val required = listOf("id", "name", "purchasedAt", "priceCNY", "endAt", "autoRenew", "createdAt", "updatedAt")
            if (!required.all { header.contains(it) }) {
                throw IllegalArgumentException("CSV missing columns: ${required.filterNot { header.contains(it) }.joinToString()}" )
            }
            val indexMap = header.withIndex().associate { it.value to it.index }
            var imported = 0
            rows.drop(1).forEachIndexed { line, row ->
                if (row.all { it.isBlank() }) return@forEachIndexed
                val idStr = row.value(indexMap, "id", line).trim()
                val nameStr = row.value(indexMap, "name", line).trim()
                val purchasedAtStr = row.value(indexMap, "purchasedAt", line)
                val priceStr = row.value(indexMap, "priceCNY", line).trim()
                val endAtStr = row.value(indexMap, "endAt", line)
                val autoRenewStr = row.value(indexMap, "autoRenew", line)
                val createdAtStr = row.value(indexMap, "createdAt", line)
                val updatedAtStr = row.value(indexMap, "updatedAt", line)
                val priceCents = try {
                    BigDecimal(priceStr).multiply(BigDecimal(100)).setScale(0, RoundingMode.HALF_UP).longValueExact()
                } catch (ex: Exception) {
                    throw IllegalArgumentException("Cannot parse priceCNY on line ${line + 2}")
                }
                val autoRenew = autoRenewStr.trim().lowercase(Locale.ROOT).let {
                    when (it) {
                        "true", "1", "yes", "y" -> true
                        "false", "0", "no", "n" -> false
                        else -> throw IllegalArgumentException("Cannot parse autoRenew on line ${line + 2}")
                    }
                }
                val entity = SubscriptionEntity(
                    id = idStr.toLongOrNull()
                        ?: throw IllegalArgumentException("Cannot parse id on line ${line + 2}"),
                    name = nameStr.takeIf { it.isNotBlank() }
                        ?: throw IllegalArgumentException("Name is blank on line ${line + 2}"),
                    provider = row.getOptional(indexMap, "provider")?.trim()?.takeUnless { it.isEmpty() },
                    purchasedAt = purchasedAtStr.toLocalDate(line),
                    priceCents = priceCents,
                    endAt = endAtStr.toLocalDate(line),
                    autoRenew = autoRenew,
                    notes = row.getOptional(indexMap, "notes")?.trim()?.takeUnless { it.isEmpty() },
                    createdAt = createdAtStr.toInstant(line),
                    updatedAt = updatedAtStr.toInstant(line),
                )
                subRepo.add(entity)
                imported++
            }
            imported
        }
        scheduler.rebuildAll()
        return count
    }

    suspend fun buildItemsCsv(): String {
        val items = itemRepo.all()
        val header = listOf(
            "id","name","purchasedAt","shelfLifeDays","expiryAt","notes","createdAt","updatedAt"
        )
        val rows = items.map { e ->
            listOf(
                e.id.toString(),
                e.name,
                e.purchasedAt.toString(),
                e.shelfLifeDays?.toString() ?: "",
                e.expiryAt?.toString() ?: "",
                e.notes?.replace("\n", " ") ?: "",
                e.createdAt.toString(),
                e.updatedAt.toString(),
            )
        }
        return (sequenceOf(header) + rows.asSequence())
            .joinToString("\n") { it.joinToString(",") { v -> escapeCsv(v) } }
    }

    suspend fun buildSubsCsv(): String {
        val subs = subRepo.all()
        val header = listOf(
            "id","name","provider","purchasedAt","priceCNY","endAt","autoRenew","notes","createdAt","updatedAt"
        )
        val rows = subs.map { e ->
            val price = String.format(Locale.US, "%.2f", e.priceCents / 100.0)
            listOf(
                e.id.toString(),
                e.name,
                e.provider ?: "",
                e.purchasedAt.toString(),
                price,
                e.endAt.toString(),
                e.autoRenew.toString(),
                e.notes?.replace("\n", " ") ?: "",
                e.createdAt.toString(),
                e.updatedAt.toString(),
            )
        }
        return (sequenceOf(header) + rows.asSequence())
            .joinToString("\n") { it.joinToString(",") { v -> escapeCsv(v) } }
    }

    private fun escapeCsv(v: String): String {
        val needsQuote = v.contains(',') || v.contains('"') || v.contains('\n')
        val w = v.replace("\"", "\"\"")
        return if (needsQuote) "\"$w\"" else w
    }
}

private fun List<String>.value(indexes: Map<String, Int>, key: String, line: Int): String {
    val idx = indexes[key] ?: throw IllegalArgumentException("CSV missing column: $key")
    if (idx >= size) {
        throw IllegalArgumentException("Column $key missing on line ${line + 2}")
    }
    return this[idx]
}

private fun List<String>.getOptional(indexes: Map<String, Int>, key: String): String? {
    val idx = indexes[key] ?: return null
    return if (idx < size) this[idx] else null
}

private fun String.toLocalDate(line: Int): LocalDate = try {
    LocalDate.parse(this.trim())
} catch (ex: Exception) {
    throw IllegalArgumentException("Invalid date on line ${line + 2}: $this")
}

private fun String.toInstant(line: Int): Instant = try {
    Instant.parse(this.trim())
} catch (ex: Exception) {
    throw IllegalArgumentException("Invalid timestamp on line ${line + 2}: $this")
}

package app.lessup.remind.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lessup.remind.data.db.ItemEntity
import app.lessup.remind.data.db.SubscriptionEntity
import app.lessup.remind.data.repo.ItemRepository
import app.lessup.remind.data.repo.SubscriptionRepository
import app.lessup.remind.data.settings.SettingsRepository
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
) : ViewModel() {

    val dueThreshold = repo.dueThresholdDays.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 7)
    val reminderHour = repo.reminderHour.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 9)
    val reminderMinute = repo.reminderMinute.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

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

    suspend fun importItemsCsv(stream: InputStream): Int {
        val count = withContext(Dispatchers.IO) {
            val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val rows = CsvParser.parse(text)
            if (rows.isEmpty()) {
                throw IllegalArgumentException("CSV 内容为空")
            }
            val header = rows.first().mapIndexed { index, value ->
                val cleaned = if (index == 0) value.removePrefix("\uFEFF") else value
                cleaned.trim()
            }
            val required = listOf("id", "name", "purchasedAt", "expiryAt", "createdAt", "updatedAt")
            if (!required.all { header.contains(it) }) {
                throw IllegalArgumentException("CSV 缺少必要列：${required.filterNot { header.contains(it) }.joinToString()}" )
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
                        ?: throw IllegalArgumentException("第 ${line + 2} 行 id 无法解析"),
                    name = nameStr.takeIf { it.isNotBlank() }
                        ?: throw IllegalArgumentException("第 ${line + 2} 行名称为空"),
                    purchasedAt = purchasedAtStr.toLocalDate(line),
                    shelfLifeDays = row.getOptional(indexMap, "shelfLifeDays")?.trim()?.let {
                        if (it.isBlank()) null else it.toIntOrNull()
                            ?: throw IllegalArgumentException("第 ${line + 2} 行 shelfLifeDays 无法解析")
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
                throw IllegalArgumentException("CSV 内容为空")
            }
            val header = rows.first().mapIndexed { index, value ->
                val cleaned = if (index == 0) value.removePrefix("\uFEFF") else value
                cleaned.trim()
            }
            val required = listOf("id", "name", "purchasedAt", "priceCNY", "endAt", "autoRenew", "createdAt", "updatedAt")
            if (!required.all { header.contains(it) }) {
                throw IllegalArgumentException("CSV 缺少必要列：${required.filterNot { header.contains(it) }.joinToString()}" )
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
                    throw IllegalArgumentException("第 ${line + 2} 行 priceCNY 无法解析")
                }
                val autoRenew = autoRenewStr.trim().lowercase(Locale.ROOT).let {
                    when (it) {
                        "true", "1", "yes", "y" -> true
                        "false", "0", "no", "n" -> false
                        else -> throw IllegalArgumentException("第 ${line + 2} 行 autoRenew 无法解析")
                    }
                }
                val entity = SubscriptionEntity(
                    id = idStr.toLongOrNull()
                        ?: throw IllegalArgumentException("第 ${line + 2} 行 id 无法解析"),
                    name = nameStr.takeIf { it.isNotBlank() }
                        ?: throw IllegalArgumentException("第 ${line + 2} 行名称为空"),
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
    val idx = indexes[key] ?: throw IllegalArgumentException("CSV 缺少列：$key")
    if (idx >= size) {
        throw IllegalArgumentException("第 ${line + 2} 行列 $key 缺失")
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
    throw IllegalArgumentException("第 ${line + 2} 行日期格式错误：$this")
}

private fun String.toInstant(line: Int): Instant = try {
    Instant.parse(this.trim())
} catch (ex: Exception) {
    throw IllegalArgumentException("第 ${line + 2} 行时间格式错误：$this")
}

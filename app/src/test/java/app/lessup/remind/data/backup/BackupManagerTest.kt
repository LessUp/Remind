package app.lessup.remind.data.backup

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import app.lessup.remind.data.db.FakeItemDao
import app.lessup.remind.data.db.FakeSubscriptionDao
import app.lessup.remind.data.db.ItemEntity
import app.lessup.remind.data.db.SubscriptionEntity
import app.lessup.remind.data.repo.ItemRepository
import app.lessup.remind.data.repo.SubscriptionRepository
import app.lessup.remind.data.settings.SettingsRepository
import app.lessup.remind.data.settings.ThemeMode
import app.lessup.remind.reminder.ReminderScheduler
import io.mockk.coVerify
import io.mockk.mockk
import java.nio.file.Files
import kotlin.io.path.createTempFile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.runBlocking
import kotlin.io.use
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import okio.Path.Companion.toPath
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BackupManagerTest {

    @Test
    fun `export and import restore repositories and settings`() = runTest {
        val itemRepo = ItemRepository(FakeItemDao())
        val subRepo = SubscriptionRepository(FakeSubscriptionDao())
        createSettingsHarness(this).use { harness ->
            val scheduler = mockk<ReminderScheduler>(relaxed = true)
            val manager = BackupManager(itemRepo, subRepo, harness.repository, scheduler)

            val createdAt = Instant.parse("2024-01-01T00:00:00Z")
            val updatedAt = Instant.parse("2024-01-05T00:00:00Z")
            itemRepo.add(
                ItemEntity(
                    id = 0,
                    name = "Tea",
                    purchasedAt = LocalDate(2024, 1, 1),
                    shelfLifeDays = 10,
                    expiryAt = LocalDate(2024, 1, 11),
                    notes = "Green",
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                )
            )
            subRepo.add(
                SubscriptionEntity(
                    id = 0,
                    name = "Music",
                    provider = "Provider",
                    purchasedAt = LocalDate(2023, 12, 1),
                    priceCents = 999,
                    endAt = LocalDate(2024, 12, 1),
                    autoRenew = true,
                    notes = "Family",
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                )
            )
            harness.repository.setDueThresholdDays(5)
            harness.repository.setReminderTime(8, 45)
            harness.repository.setNotificationsEnabled(true)
            harness.repository.setItemRemindersEnabled(false)
            harness.repository.setSubRemindersEnabled(true)
            harness.repository.setDailyOverviewEnabled(false)
            harness.repository.setSnoozeMinutes(90)
            harness.repository.setThemeMode(ThemeMode.DARK)

            val exported = manager.export(password = "secret")
            val expectedItems = itemRepo.all()
            val expectedSubs = subRepo.all()

            itemRepo.replaceAll(emptyList())
            subRepo.replaceAll(emptyList())
            harness.repository.setDueThresholdDays(21)
            harness.repository.setReminderTime(6, 0)
            harness.repository.setNotificationsEnabled(false)
            harness.repository.setItemRemindersEnabled(true)
            harness.repository.setSubRemindersEnabled(false)
            harness.repository.setDailyOverviewEnabled(true)
            harness.repository.setSnoozeMinutes(10)
            harness.repository.setThemeMode(ThemeMode.LIGHT)

            manager.import(exported, password = "secret")

            assertContentEquals(expectedItems, itemRepo.all())
            assertContentEquals(expectedSubs, subRepo.all())
            assertEquals(5, harness.repository.dueThresholdDays.first())
            assertEquals(8, harness.repository.reminderHour.first())
            assertEquals(45, harness.repository.reminderMinute.first())
            assertTrue(harness.repository.notificationsEnabled.first())
            assertFalse(harness.repository.itemRemindersEnabled.first())
            assertTrue(harness.repository.subRemindersEnabled.first())
            assertFalse(harness.repository.dailyOverviewEnabled.first())
            assertEquals(90, harness.repository.snoozeMinutes.first())
            assertEquals(ThemeMode.DARK, harness.repository.themeMode.first())

            coVerify { scheduler.rebuildAll() }
        }
    }

    private fun createSettingsHarness(scope: TestScope): SettingsHarness {
        val file = createTempFile("settings", ".preferences_pb")
        val store = PreferenceDataStoreFactory.createWithPath(
            scope = scope.backgroundScope,
            produceFile = { file.toString().toPath() }
        )
        return SettingsHarness(SettingsRepository(store), store, file)
    }

    private data class SettingsHarness(
        val repository: SettingsRepository,
        val store: DataStore<Preferences>,
        val file: java.nio.file.Path,
    ) : AutoCloseable {
        override fun close() {
            runBlocking { store.close() }
            Files.deleteIfExists(file)
        }
    }
}

package app.lessup.remind.ui.items

import app.lessup.remind.data.db.FakeItemDao
import app.lessup.remind.data.db.ItemEntity
import app.lessup.remind.data.repo.ItemRepository
import app.lessup.remind.reminder.ReminderScheduler
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ItemEditViewModelTest {

    @Test
    fun `updating item preserves original createdAt`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repo = ItemRepository(FakeItemDao())
            val scheduler = mockk<ReminderScheduler>(relaxed = true)
            val viewModel = ItemEditViewModel(repo, scheduler)

            val createdAt = Instant.fromEpochMilliseconds(1_000)
            val initial = ItemEntity(
                id = 0,
                name = "Milk",
                purchasedAt = LocalDate(2024, 1, 1),
                shelfLifeDays = 7,
                expiryAt = LocalDate(2024, 1, 8),
                notes = "note",
                createdAt = createdAt,
                updatedAt = createdAt,
            )
            val id = repo.add(initial)
            val form = ItemEditViewModel.Form(
                id = id,
                name = "Milk updated",
                purchasedAt = initial.purchasedAt,
                shelfLifeDays = initial.shelfLifeDays,
                expiryAt = initial.expiryAt,
                notes = initial.notes,
            )

            viewModel.save(form)
            advanceUntilIdle()

            val updated = repo.get(id)!!
            assertEquals(createdAt, updated.createdAt)
            assertNotEquals(updated.createdAt, updated.updatedAt)
            assertEquals("Milk updated", updated.name)

            coVerify {
                scheduler.scheduleForItem(match {
                    it.id == id && it.createdAt == createdAt && it.name == "Milk updated"
                })
            }
        } finally {
            Dispatchers.resetMain()
        }
    }
}

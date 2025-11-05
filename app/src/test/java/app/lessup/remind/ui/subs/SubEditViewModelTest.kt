package app.lessup.remind.ui.subs

import app.lessup.remind.data.db.FakeSubscriptionDao
import app.lessup.remind.data.db.SubscriptionEntity
import app.lessup.remind.data.repo.SubscriptionRepository
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
class SubEditViewModelTest {

    @Test
    fun `updating subscription preserves original createdAt`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repo = SubscriptionRepository(FakeSubscriptionDao())
            val scheduler = mockk<ReminderScheduler>(relaxed = true)
            val viewModel = SubEditViewModel(repo, scheduler)

            val createdAt = Instant.fromEpochMilliseconds(2_000)
            val initial = SubscriptionEntity(
                id = 0,
                name = "Prime",
                provider = "Amazon",
                purchasedAt = LocalDate(2024, 1, 1),
                priceCents = 1299,
                endAt = LocalDate(2024, 2, 1),
                autoRenew = false,
                notes = "note",
                createdAt = createdAt,
                updatedAt = createdAt,
            )
            val id = repo.add(initial)
            val form = SubEditViewModel.Form(
                id = id,
                name = "Prime+",
                provider = initial.provider,
                purchasedAt = initial.purchasedAt,
                priceCents = initial.priceCents,
                endAt = initial.endAt,
                autoRenew = initial.autoRenew,
                notes = initial.notes,
            )

            viewModel.save(form)
            advanceUntilIdle()

            val updated = repo.get(id)!!
            assertEquals(createdAt, updated.createdAt)
            assertNotEquals(updated.createdAt, updated.updatedAt)
            assertEquals("Prime+", updated.name)

            coVerify {
                scheduler.scheduleForSub(match {
                    it.id == id && it.createdAt == createdAt && it.name == "Prime+"
                })
            }
        } finally {
            Dispatchers.resetMain()
        }
    }
}

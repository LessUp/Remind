package app.lessup.remind.reminder

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class ReminderPlannerTest {
    @Test
    fun `compute schedule dates respects offsets and today`() {
        val target = LocalDate(2024, 12, 10)
        val today = LocalDate(2024, 12, 1)
        val dates = ReminderPlanner.computeScheduleDates(target, listOf(7, 3, 1, 0, -1), today)
        assertEquals(listOf(LocalDate(2024, 12, 3), LocalDate(2024, 12, 7), LocalDate(2024, 12, 9), LocalDate(2024, 12, 10), LocalDate(2024, 12, 11)), dates)
    }

    @Test
    fun `schedule dates exclude past entries`() {
        val target = LocalDate(2024, 5, 5)
        val today = LocalDate(2024, 5, 5)
        val dates = ReminderPlanner.computeScheduleDates(target, listOf(10, 1, 0, -1), today)
        assertEquals(listOf(LocalDate(2024, 5, 5), LocalDate(2024, 5, 6)), dates)
    }

    @Test
    fun `millis until next trigger moves to next day when passed`() {
        val tz = TimeZone.UTC
        val now = LocalDate(2024, 5, 1).toInstant(tz)
        val twoPm = ReminderPlanner.millisUntilNextTrigger(14, 0, now, tz)
        val expected = LocalDate(2024, 5, 1).atTime(14, 0, tz) - now
        assertEquals(expected, twoPm)

        val afterInstant = LocalDate(2024, 5, 1).atTime(16, 0, tz)
        val nextDayMillis = ReminderPlanner.millisUntilNextTrigger(10, 0, afterInstant, tz)
        val expectedNext = LocalDate(2024, 5, 2).atTime(10, 0, tz) - afterInstant
        assertEquals(expectedNext, nextDayMillis)
    }

    private fun LocalDate.atTime(hour: Int, minute: Int, tz: TimeZone): Instant =
        kotlinx.datetime.LocalDateTime(year, monthNumber, dayOfMonth, hour, minute).toInstant(tz)

    private operator fun Instant.minus(other: Instant): Long = this.toEpochMilliseconds() - other.toEpochMilliseconds()
}

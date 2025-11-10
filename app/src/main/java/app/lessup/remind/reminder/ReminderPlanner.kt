package app.lessup.remind.reminder

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

object ReminderPlanner {
    fun computeScheduleDates(target: LocalDate, offsets: List<Int>, today: LocalDate): List<LocalDate> {
        return offsets.mapNotNull { offset ->
            val date = when {
                offset > 0 -> target.plus(DatePeriod(days = -offset))
                offset == 0 -> target
                else -> target.plus(DatePeriod(days = -offset))
            }
            if (date >= today) date else null
        }.distinct().sorted()
    }

    fun millisUntilNextTrigger(hour: Int, minute: Int, now: Instant, timeZone: TimeZone): Long {
        val today = now.toLocalDateTime(timeZone).date
        val candidate = LocalDateTime(today.year, today.monthNumber, today.dayOfMonth, hour, minute).toInstant(timeZone)
        val target = if (candidate > now) {
            candidate
        } else {
            val nextDay = today.plus(DatePeriod(days = 1))
            LocalDateTime(nextDay.year, nextDay.monthNumber, nextDay.dayOfMonth, hour, minute).toInstant(timeZone)
        }
        return target.toEpochMilliseconds() - now.toEpochMilliseconds()
    }

    fun delayTo(date: LocalDate, hour: Int, minute: Int, now: Instant, timeZone: TimeZone): Long {
        val target = LocalDateTime(date.year, date.monthNumber, date.dayOfMonth, hour, minute).toInstant(timeZone)
        return target.toEpochMilliseconds() - now.toEpochMilliseconds()
    }
}

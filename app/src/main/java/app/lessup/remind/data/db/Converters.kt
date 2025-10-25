package app.lessup.remind.data.db

import androidx.room.TypeConverter
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDate

class Converters {
    @TypeConverter
    fun fromEpochDay(value: Long?): LocalDate? = value?.let { LocalDate.fromEpochDays(it.toInt()) }

    @TypeConverter
    fun localDateToEpochDay(date: LocalDate?): Long? = date?.toEpochDays()?.toLong()

    @TypeConverter
    fun fromEpochMilli(value: Long?): Instant? = value?.let { Instant.fromEpochMilliseconds(it) }

    @TypeConverter
    fun instantToEpochMilli(instant: Instant?): Long? = instant?.toEpochMilliseconds()
}

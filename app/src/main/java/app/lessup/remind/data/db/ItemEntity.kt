package app.lessup.remind.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val purchasedAt: LocalDate,
    val shelfLifeDays: Int?,
    val expiryAt: LocalDate?,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

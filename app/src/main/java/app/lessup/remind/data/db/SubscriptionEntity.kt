package app.lessup.remind.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val provider: String?,
    val purchasedAt: LocalDate,
    val priceCents: Long,
    val currency: String = "CNY",
    val endAt: LocalDate,
    val autoRenew: Boolean,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

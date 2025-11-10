package app.lessup.remind.core.analytics

import app.lessup.remind.data.db.ItemEntity
import app.lessup.remind.data.db.SubscriptionEntity
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

data class MonthlyTrendResult(
    val monthStart: LocalDate,
    val itemsExpiring: Int,
    val overdueItems: Int,
    val subscriptionsEnding: Int,
    val subscriptionCostCents: Long
)

object MonthlyTrendCalculator {
    fun calculate(
        items: List<ItemEntity>,
        subscriptions: List<SubscriptionEntity>,
        months: Int,
        referenceDate: LocalDate
    ): List<MonthlyTrendResult> {
        require(months > 0)
        val startOfCurrentMonth = LocalDate(referenceDate.year, referenceDate.monthNumber, 1)
        val monthStarts = (months - 1 downTo 0).map { offset ->
            startOfCurrentMonth.plus(DatePeriod(months = -offset))
        }
        return monthStarts.map { monthStart ->
            val nextMonth = monthStart.plus(DatePeriod(months = 1))
            val itemsExpiring = items.count { entity ->
                val expiry = entity.expiryAt ?: return@count false
                expiry >= monthStart && expiry < nextMonth
            }
            val overdue = items.count { entity ->
                val expiry = entity.expiryAt ?: return@count false
                expiry < monthStart
            }
            val subsEnding = subscriptions.filter { sub ->
                sub.endAt >= monthStart && sub.endAt < nextMonth
            }
            val subsCost = subsEnding.sumOf { it.priceCents }
            MonthlyTrendResult(
                monthStart = monthStart,
                itemsExpiring = itemsExpiring,
                overdueItems = overdue,
                subscriptionsEnding = subsEnding.size,
                subscriptionCostCents = subsCost
            )
        }
    }
}

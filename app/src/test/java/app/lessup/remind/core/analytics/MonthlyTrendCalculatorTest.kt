package app.lessup.remind.core.analytics

import app.lessup.remind.data.db.ItemEntity
import app.lessup.remind.data.db.SubscriptionEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

class MonthlyTrendCalculatorTest {
    @Test
    fun `calculates trends for recent months`() {
        val items = listOf(
            item(expiry = LocalDate(2024, 5, 10)),
            item(expiry = LocalDate(2024, 6, 1)),
            item(expiry = LocalDate(2024, 4, 30))
        )
        val subs = listOf(
            sub(endAt = LocalDate(2024, 5, 15), price = 1000),
            sub(endAt = LocalDate(2024, 4, 20), price = 500)
        )
        val trends = MonthlyTrendCalculator.calculate(items, subs, 3, LocalDate(2024, 6, 5))
        assertEquals(3, trends.size)
        val may = trends[1]
        assertEquals(1, may.itemsExpiring)
        assertEquals(1, may.overdueItems)
        assertEquals(1, may.subscriptionsEnding)
        assertEquals(1000, may.subscriptionCostCents)
    }

    private fun item(expiry: LocalDate) = ItemEntity(
        id = 0,
        name = "",
        purchasedAt = LocalDate(2024, 1, 1),
        shelfLifeDays = null,
        expiryAt = expiry,
        notes = null,
        createdAt = Instant.DISTANT_PAST,
        updatedAt = Instant.DISTANT_PAST
    )

    private fun sub(endAt: LocalDate, price: Long) = SubscriptionEntity(
        id = 0,
        name = "",
        provider = null,
        purchasedAt = LocalDate(2024, 1, 1),
        priceCents = price,
        endAt = endAt,
        autoRenew = false,
        notes = null,
        createdAt = Instant.DISTANT_PAST,
        updatedAt = Instant.DISTANT_PAST
    )
}

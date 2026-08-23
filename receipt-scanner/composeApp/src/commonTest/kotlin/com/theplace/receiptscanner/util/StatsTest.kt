package com.theplace.receiptscanner.util

import com.theplace.receiptscanner.data.Receipt
import com.theplace.receiptscanner.data.ReceiptCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class StatsTest {

    private val utc = TimeZone.UTC
    private val now = LocalDateTime(2026, 5, 14, 12, 0).toInstant(utc).toEpochMilliseconds()

    @Test
    fun statsByCategoryForMonth_aggregates_current_month() {
        val receipts = listOf(
            receipt(id = 1, ms(2026, 5, 1), 1000, ReceiptCategory.Groceries),
            receipt(id = 2, ms(2026, 5, 5), 500, ReceiptCategory.Groceries),
            receipt(id = 3, ms(2026, 5, 7), 1500, ReceiptCategory.Restaurant),
            receipt(id = 4, ms(2026, 4, 30), 9999, ReceiptCategory.Groceries), // mois précédent
            receipt(id = 5, ms(2026, 5, 10), null, ReceiptCategory.Other), // sans montant
        )
        val stats = receipts.statsByCategoryForMonth(now, utc)
        // Tri par total décroissant. En cas d'égalité (Restaurant 1500 vs
        // Groceries 1000+500=1500), sortedByDescending est stable et garde
        // l'ordre d'insertion → Groceries (id 1) apparaît avant Restaurant (id 3).
        assertEquals(
            listOf(
                ReceiptCategory.Groceries to 1500L, // 1000 + 500
                ReceiptCategory.Restaurant to 1500L,
            ),
            stats.map { it.category to it.totalCents },
        )
    }

    @Test
    fun statsByCategoryForMonth_includes_uncategorised() {
        val receipts = listOf(
            receipt(id = 1, ms(2026, 5, 1), 1200, null),
        )
        val stats = receipts.statsByCategoryForMonth(now, utc)
        assertEquals(1, stats.size)
        assertEquals(null, stats[0].category)
        assertEquals(1200L, stats[0].totalCents)
    }

    @Test
    fun statsByLast12Months_returns_12_entries_in_chronological_order() {
        val stats = emptyList<Receipt>().statsByLast12Months(now, utc)
        assertEquals(12, stats.size)
        // Du plus ancien au plus récent.
        assertEquals(2025 to 6, stats.first().year to stats.first().month)
        assertEquals(2026 to 5, stats.last().year to stats.last().month)
        assertTrue(stats.all { it.totalCents == 0L })
    }

    @Test
    fun statsByLast12Months_sums_in_correct_buckets() {
        val receipts = listOf(
            receipt(id = 1, ms(2026, 5, 1), 1000, null),
            receipt(id = 2, ms(2026, 5, 31), 200, null),
            receipt(id = 3, ms(2026, 3, 15), 700, null),
        )
        val stats = receipts.statsByLast12Months(now, utc)
        val may = stats.firstOrNull { it.year == 2026 && it.month == 5 }
        val march = stats.firstOrNull { it.year == 2026 && it.month == 3 }
        assertEquals(1200L, may?.totalCents)
        assertEquals(700L, march?.totalCents)
    }

    @Test
    fun monthShortLabel_covers_all_months() {
        assertEquals("janv.", monthShortLabel(1))
        assertEquals("mai", monthShortLabel(5))
        assertEquals("déc.", monthShortLabel(12))
        assertEquals("?", monthShortLabel(13))
    }

    private fun ms(year: Int, month: Int, day: Int): Long =
        LocalDateTime(year, month, day, 12, 0).toInstant(utc).toEpochMilliseconds()

    private fun receipt(
        id: Long,
        createdAt: Long,
        totalCents: Long?,
        category: ReceiptCategory?,
    ) = Receipt(
        id = id,
        name = "Ticket $id",
        fileName = "$id.pdf",
        pageCount = 1,
        sizeBytes = 100,
        createdAt = createdAt,
        category = category,
        totalCents = totalCents,
    )
}

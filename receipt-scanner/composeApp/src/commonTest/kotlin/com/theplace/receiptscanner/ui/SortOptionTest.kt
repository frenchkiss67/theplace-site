package com.theplace.receiptscanner.ui

import com.theplace.receiptscanner.data.Receipt
import com.theplace.receiptscanner.data.ReceiptCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class SortOptionTest {

    private val carrefour = receipt(id = 1, name = "Carrefour", createdAt = 100, sizeBytes = 50_000)
    private val auchan = receipt(id = 2, name = "Auchan", createdAt = 300, sizeBytes = 10_000)
    private val biocoop = receipt(id = 3, name = "Biocoop", createdAt = 200, sizeBytes = 80_000)
    private val all = listOf(carrefour, auchan, biocoop)

    @Test
    fun sortedBy_DateDesc_orders_recent_first() {
        assertEquals(
            listOf(auchan, biocoop, carrefour),
            all.sortedBy(SortOption.DateDesc),
        )
    }

    @Test
    fun sortedBy_DateAsc_orders_oldest_first() {
        assertEquals(
            listOf(carrefour, biocoop, auchan),
            all.sortedBy(SortOption.DateAsc),
        )
    }

    @Test
    fun sortedBy_NameAsc_is_case_insensitive() {
        val mixedCase = listOf(
            receipt(id = 1, name = "carrefour"),
            receipt(id = 2, name = "Auchan"),
            receipt(id = 3, name = "biocoop"),
        )
        val sorted = mixedCase.sortedBy(SortOption.NameAsc).map { it.name }
        assertEquals(listOf("Auchan", "biocoop", "carrefour"), sorted)
    }

    @Test
    fun sortedBy_SizeDesc_orders_largest_first() {
        assertEquals(
            listOf(biocoop, carrefour, auchan),
            all.sortedBy(SortOption.SizeDesc),
        )
    }

    @Test
    fun filteredByQuery_empty_returns_all() {
        assertEquals(all, all.filteredByQuery(""))
        assertEquals(all, all.filteredByQuery("   "))
    }

    @Test
    fun filteredByQuery_is_case_insensitive_and_substring() {
        assertEquals(listOf(carrefour), all.filteredByQuery("CARRE"))
        assertEquals(listOf(carrefour), all.filteredByQuery("four"))
        assertEquals(listOf(auchan), all.filteredByQuery("au"))
    }

    @Test
    fun filteredByQuery_trims_input() {
        assertEquals(listOf(biocoop), all.filteredByQuery("  Bio  "))
    }

    @Test
    fun filteredByQuery_empty_match_returns_empty_list() {
        assertTrue(all.filteredByQuery("xyz").isEmpty())
    }

    @Test
    fun filteredBy_All_returns_everything() {
        assertEquals(all, all.filteredBy(CategoryFilter.All))
    }

    @Test
    fun filteredBy_Uncategorised_keeps_only_null_category() {
        val list = listOf(
            receipt(id = 1, name = "A", category = null),
            receipt(id = 2, name = "B", category = ReceiptCategory.Groceries),
            receipt(id = 3, name = "C", category = null),
        )
        val filtered = list.filteredBy(CategoryFilter.Uncategorised)
        assertEquals(listOf(1L, 3L), filtered.map { it.id })
    }

    @Test
    fun filteredBy_Of_keeps_only_matching_category() {
        val list = listOf(
            receipt(id = 1, name = "A", category = ReceiptCategory.Groceries),
            receipt(id = 2, name = "B", category = ReceiptCategory.Restaurant),
            receipt(id = 3, name = "C", category = ReceiptCategory.Groceries),
        )
        val filtered = list.filteredBy(CategoryFilter.Of(ReceiptCategory.Groceries))
        assertEquals(listOf(1L, 3L), filtered.map { it.id })
    }

    @Test
    fun monthTotalCents_sums_only_receipts_in_current_month() {
        // « Maintenant » fixé au 14 mai 2026 12h UTC.
        val now = LocalDateTime(2026, 5, 14, 12, 0).toInstant(TimeZone.UTC).toEpochMilliseconds()
        val sameMonth1 = LocalDateTime(2026, 5, 1, 9, 0).toInstant(TimeZone.UTC).toEpochMilliseconds()
        val sameMonth2 = LocalDateTime(2026, 5, 31, 23, 0).toInstant(TimeZone.UTC).toEpochMilliseconds()
        val prevMonth = LocalDateTime(2026, 4, 30, 12, 0).toInstant(TimeZone.UTC).toEpochMilliseconds()
        val list = listOf(
            receipt(id = 1, name = "A", createdAt = sameMonth1, totalCents = 1000),
            receipt(id = 2, name = "B", createdAt = sameMonth2, totalCents = 2500),
            receipt(id = 3, name = "C", createdAt = prevMonth, totalCents = 9999),
            receipt(id = 4, name = "D", createdAt = sameMonth1, totalCents = null),
        )
        assertEquals(3500L, list.monthTotalCents(now, TimeZone.UTC))
    }

    private fun receipt(
        id: Long,
        name: String,
        createdAt: Long = 0,
        sizeBytes: Long = 0,
        category: ReceiptCategory? = null,
        totalCents: Long? = null,
    ) = Receipt(
        id = id,
        name = name,
        fileName = "$id.pdf",
        pageCount = 1,
        sizeBytes = sizeBytes,
        createdAt = createdAt,
        category = category,
        totalCents = totalCents,
    )
}

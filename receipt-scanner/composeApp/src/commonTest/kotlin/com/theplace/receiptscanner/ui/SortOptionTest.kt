package com.theplace.receiptscanner.ui

import com.theplace.receiptscanner.data.Receipt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    private fun receipt(
        id: Long,
        name: String,
        createdAt: Long = 0,
        sizeBytes: Long = 0,
    ) = Receipt(
        id = id,
        name = name,
        fileName = "$id.pdf",
        pageCount = 1,
        sizeBytes = sizeBytes,
        createdAt = createdAt,
    )
}

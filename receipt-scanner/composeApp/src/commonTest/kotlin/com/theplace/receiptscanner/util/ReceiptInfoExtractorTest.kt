package com.theplace.receiptscanner.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReceiptInfoExtractorTest {

    @Test
    fun extractTotalCents_finds_total_in_line() {
        val text = """
            CARREFOUR CITY
            Baguette          1,20
            Eau              0,80
            TOTAL            12,30
        """.trimIndent()
        assertEquals(1230L, ReceiptInfoExtractor.extractTotalCents(text))
    }

    @Test
    fun extractTotalCents_handles_comma_or_dot_decimal() {
        assertEquals(1500L, ReceiptInfoExtractor.extractTotalCents("Total : 15.00 €"))
        assertEquals(1500L, ReceiptInfoExtractor.extractTotalCents("Total : 15,00 €"))
    }

    @Test
    fun extractTotalCents_uses_keyword_montant() {
        val text = """
            Lait                 2,00 €
            Pâtes                3,50 €
            Montant TTC          5,50 €
        """.trimIndent()
        assertEquals(550L, ReceiptInfoExtractor.extractTotalCents(text))
    }

    @Test
    fun extractTotalCents_takes_largest_when_multiple() {
        // Si plusieurs montants sur la ligne « total », on prend le plus grand.
        val text = "Total 1,20 + 3,80 = 5,00"
        assertEquals(500L, ReceiptInfoExtractor.extractTotalCents(text))
    }

    @Test
    fun extractTotalCents_returns_null_when_no_match() {
        assertNull(ReceiptInfoExtractor.extractTotalCents("Pas de total ici"))
        assertNull(ReceiptInfoExtractor.extractTotalCents(""))
        assertNull(ReceiptInfoExtractor.extractTotalCents(null))
    }

    @Test
    fun extractTotalCents_uses_next_line_for_amount() {
        val text = """
            TOTAL
            12,30 €
        """.trimIndent()
        assertEquals(1230L, ReceiptInfoExtractor.extractTotalCents(text))
    }
}

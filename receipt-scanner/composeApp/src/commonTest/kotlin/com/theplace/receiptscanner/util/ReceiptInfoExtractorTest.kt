package com.theplace.receiptscanner.util

import com.theplace.receiptscanner.data.ReceiptCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

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

    @Test
    fun extractMerchantName_returns_first_significant_line() {
        val text = """

            CARREFOUR CITY
            32 rue de Rivoli
            75001 Paris
        """.trimIndent()
        assertEquals("CARREFOUR CITY", ReceiptInfoExtractor.extractMerchantName(text))
    }

    @Test
    fun extractMerchantName_skips_pure_numbers_and_siret() {
        val text = """
            123456789
            SIRET 38493823800012
            BIOCOOP — La Marche
        """.trimIndent()
        assertEquals("BIOCOOP — La Marche", ReceiptInfoExtractor.extractMerchantName(text))
    }

    @Test
    fun extractMerchantName_returns_null_for_blank_or_short() {
        assertNull(ReceiptInfoExtractor.extractMerchantName(""))
        assertNull(ReceiptInfoExtractor.extractMerchantName("a\nb\n"))
        assertNull(ReceiptInfoExtractor.extractMerchantName(null))
    }

    @Test
    fun extractPurchasedAtMs_matches_french_date() {
        val text = "Ticket émis le 14/05/2026 à 10h32"
        val expected = LocalDateTime(2026, 5, 14, 0, 0).toInstant(TimeZone.UTC).toEpochMilliseconds()
        assertEquals(expected, ReceiptInfoExtractor.extractPurchasedAtMs(text, TimeZone.UTC))
    }

    @Test
    fun extractPurchasedAtMs_matches_two_digit_year() {
        val text = "Date: 01-02-26"
        val expected = LocalDateTime(2026, 2, 1, 0, 0).toInstant(TimeZone.UTC).toEpochMilliseconds()
        assertEquals(expected, ReceiptInfoExtractor.extractPurchasedAtMs(text, TimeZone.UTC))
    }

    @Test
    fun extractPurchasedAtMs_rejects_impossible_dates() {
        assertNull(ReceiptInfoExtractor.extractPurchasedAtMs("99/99/2026", TimeZone.UTC))
        assertNull(ReceiptInfoExtractor.extractPurchasedAtMs("Pas de date ici", TimeZone.UTC))
        assertNull(ReceiptInfoExtractor.extractPurchasedAtMs(null, TimeZone.UTC))
    }

    @Test
    fun extractPurchasedAtMs_rejects_year_out_of_century() {
        assertNull(ReceiptInfoExtractor.extractPurchasedAtMs("14/05/1899", TimeZone.UTC))
        assertNull(ReceiptInfoExtractor.extractPurchasedAtMs("14/05/2100", TimeZone.UTC))
    }

    @Test
    fun categoryForMerchant_maps_known_brands() {
        assertEquals(ReceiptCategory.Groceries, ReceiptInfoExtractor.categoryForMerchant("CARREFOUR CITY"))
        assertEquals(ReceiptCategory.Groceries, ReceiptInfoExtractor.categoryForMerchant("AUCHAN"))
        assertEquals(ReceiptCategory.Restaurant, ReceiptInfoExtractor.categoryForMerchant("McDonald's Bastille"))
        assertEquals(ReceiptCategory.Fuel, ReceiptInfoExtractor.categoryForMerchant("TOTAL ENERGIES"))
        assertEquals(ReceiptCategory.Health, ReceiptInfoExtractor.categoryForMerchant("Pharmacie du marché"))
        assertEquals(ReceiptCategory.Shopping, ReceiptInfoExtractor.categoryForMerchant("FNAC Paris"))
    }

    @Test
    fun categoryForMerchant_returns_null_when_unknown_or_blank() {
        assertNull(ReceiptInfoExtractor.categoryForMerchant("Cordonnerie du coin"))
        assertNull(ReceiptInfoExtractor.categoryForMerchant(""))
        assertNull(ReceiptInfoExtractor.categoryForMerchant(null))
    }
}

package com.theplace.receiptscanner.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class FormattingTest {

    @Test
    fun formatDate_pads_day_month_hour_and_minute() {
        // 5 mai 2026, 07h09 UTC.
        val instant = LocalDateTime(2026, 5, 5, 7, 9).toInstant(TimeZone.UTC)
        val formatted = formatDate(instant.toEpochMilliseconds(), zone = TimeZone.UTC)
        assertEquals("05/05/2026 07:09", formatted)
    }

    @Test
    fun formatDate_renders_two_digit_minute_at_midnight() {
        val instant = LocalDateTime(2030, 12, 31, 0, 0).toInstant(TimeZone.UTC)
        assertEquals("31/12/2030 00:00", formatDate(instant.toEpochMilliseconds(), TimeZone.UTC))
    }

    @Test
    fun formatSize_bytes_under_kilobyte() {
        assertEquals("0 o", formatSize(0))
        assertEquals("1 o", formatSize(1))
        assertEquals("1023 o", formatSize(1023))
    }

    @Test
    fun formatSize_kilobytes_between_1024_and_1MB() {
        assertEquals("1 Ko", formatSize(1024))
        assertEquals("38 Ko", formatSize(38L * 1024))
        assertEquals("1023 Ko", formatSize(1023L * 1024))
    }

    @Test
    fun formatSize_megabytes_use_french_decimal() {
        // 1,5 Mo
        val bytes = (1.5 * 1024 * 1024).toLong()
        assertEquals("1,5 Mo", formatSize(bytes))
    }

    @Test
    fun formatSize_megabytes_truncates_to_one_decimal() {
        // 1,29 Mo doit être affiché « 1,2 Mo » (troncature, pas arrondi).
        val bytes = (1.29 * 1024 * 1024).toLong()
        assertEquals("1,2 Mo", formatSize(bytes))
    }

    @Test
    fun formatSize_gigabytes() {
        val bytes = (2.3 * 1024 * 1024 * 1024).toLong()
        assertEquals("2,3 Go", formatSize(bytes))
    }

    @Test
    fun formatAmount_returns_french_format_with_two_decimals() {
        assertEquals("", formatAmount(null))
        assertEquals("0,00 €", formatAmount(0))
        assertEquals("0,07 €", formatAmount(7))
        assertEquals("12,30 €", formatAmount(1230))
        assertEquals("1234,56 €", formatAmount(123_456))
        assertEquals("-2,50 €", formatAmount(-250))
    }

    @Test
    fun parseAmountCents_accepts_french_and_english_decimals() {
        assertEquals(null, parseAmountCents(""))
        assertEquals(null, parseAmountCents("   "))
        assertEquals(1200L, parseAmountCents("12"))
        assertEquals(1230L, parseAmountCents("12,30"))
        assertEquals(1230L, parseAmountCents("12.30"))
        assertEquals(1200L, parseAmountCents("12 €"))
        assertEquals(1230L, parseAmountCents(" 12,3 ")) // 12.3 → 1230
        assertEquals(null, parseAmountCents("abc"))
        assertEquals(null, parseAmountCents("12,3,4"))
    }

    @Test
    fun defaultReceiptName_uses_formatted_date_prefix() {
        val instant = LocalDateTime(2026, 1, 2, 3, 4).toInstant(TimeZone.UTC)
        // On vérifie le préfixe pour ne pas dépendre de la TZ de la JVM de test.
        val name = defaultReceiptName(instant.toEpochMilliseconds())
        kotlin.test.assertTrue(name.startsWith("Ticket du "), "got: $name")
    }
}

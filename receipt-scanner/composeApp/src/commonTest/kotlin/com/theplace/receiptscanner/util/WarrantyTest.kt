package com.theplace.receiptscanner.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class WarrantyTest {

    @Test
    fun warrantyEndMs_returns_null_for_missing_inputs() {
        assertNull(warrantyEndMs(null, 12, TimeZone.UTC))
        assertNull(warrantyEndMs(0L, null, TimeZone.UTC))
        assertNull(warrantyEndMs(0L, 0, TimeZone.UTC))
    }

    @Test
    fun warrantyEndMs_adds_months_on_calendar_date() {
        val purchased = LocalDateTime(2026, 1, 15, 12, 0).toInstant(TimeZone.UTC).toEpochMilliseconds()
        val end = warrantyEndMs(purchased, 12, TimeZone.UTC)!!
        // 15 janvier 2027 à minuit UTC.
        val expected = LocalDateTime(2027, 1, 15, 0, 0).toInstant(TimeZone.UTC).toEpochMilliseconds()
        assertEquals(expected, end)
    }

    @Test
    fun warrantyEndMs_handles_end_of_month_correctly() {
        // 31 janvier + 1 mois → 28/29 février (gère automatiquement par LocalDate).
        val purchased = LocalDateTime(2026, 1, 31, 0, 0).toInstant(TimeZone.UTC).toEpochMilliseconds()
        val end = warrantyEndMs(purchased, 1, TimeZone.UTC)!!
        val expected = LocalDateTime(2026, 2, 28, 0, 0).toInstant(TimeZone.UTC).toEpochMilliseconds()
        assertEquals(expected, end)
    }

    @Test
    fun daysUntilWarrantyEnd_counts_full_days() {
        val purchased = LocalDateTime(2026, 1, 1, 0, 0).toInstant(TimeZone.UTC).toEpochMilliseconds()
        // Fin = 1er février 2026.
        val now = LocalDateTime(2026, 1, 15, 12, 0).toInstant(TimeZone.UTC).toEpochMilliseconds()
        val days = daysUntilWarrantyEnd(now, purchased, 1, TimeZone.UTC)!!
        // 1er février - 15 janvier 12:00 = 16 jours 12h → 16 jours (troncature).
        assertEquals(16, days)
    }

    @Test
    fun daysUntilWarrantyEnd_negative_after_expiry() {
        val purchased = LocalDateTime(2024, 1, 1, 0, 0).toInstant(TimeZone.UTC).toEpochMilliseconds()
        val now = LocalDateTime(2026, 5, 14, 0, 0).toInstant(TimeZone.UTC).toEpochMilliseconds()
        val days = daysUntilWarrantyEnd(now, purchased, 12, TimeZone.UTC)!!
        // Fin = 1er janvier 2025 ; 2026-05-14 est ~498 jours après.
        assertEquals(-498, days)
    }

    @Test
    fun formatDateOnly_does_not_include_time() {
        val ts = LocalDateTime(2026, 5, 14, 10, 32).toInstant(TimeZone.UTC).toEpochMilliseconds()
        assertEquals("14/05/2026", formatDateOnly(ts, TimeZone.UTC))
    }
}

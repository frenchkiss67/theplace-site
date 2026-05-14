package com.theplace.receiptscanner.util

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Mise en forme « 14/05/2026 10:32 » indépendante de la locale système. */
fun formatDate(epochMs: Long, zone: TimeZone = TimeZone.currentSystemDefault()): String {
    val dt = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(zone)
    fun Int.pad() = toString().padStart(2, '0')
    return "${dt.dayOfMonth.pad()}/${dt.monthNumber.pad()}/${dt.year} " +
        "${dt.hour.pad()}:${dt.minute.pad()}"
}

/** Format compact « 38 Ko » / « 1,2 Mo ». */
fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes o"
    bytes < 1024L * 1024 -> "${bytes / 1024} Ko"
    bytes < 1024L * 1024 * 1024 -> {
        val mb = bytes / (1024.0 * 1024.0)
        // Une décimale, séparateur virgule (FR).
        val truncated = (mb * 10).toLong() / 10.0
        truncated.toString().replace('.', ',') + " Mo"
    }
    else -> {
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        val truncated = (gb * 10).toLong() / 10.0
        truncated.toString().replace('.', ',') + " Go"
    }
}

/** Libellé par défaut « Ticket du jj/MM/yyyy HH:mm ». */
fun defaultReceiptName(epochMs: Long): String = "Ticket du ${formatDate(epochMs)}"

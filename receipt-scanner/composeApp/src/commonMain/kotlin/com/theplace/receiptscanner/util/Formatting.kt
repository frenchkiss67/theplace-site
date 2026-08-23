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

/**
 * Date verbeuse pour la lecture par TalkBack / VoiceOver, ex.
 * « 14 mai 2026 à 10 h 32 ». Le format slash-deux-points est mal lu
 * par les lecteurs d'écran.
 */
fun formatDateForAccessibility(
    epochMs: Long,
    zone: TimeZone = TimeZone.currentSystemDefault(),
): String {
    val dt = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(zone)
    val monthName = monthLongLabel(dt.monthNumber)
    val hour = dt.hour
    val minute = dt.minute.toString().padStart(2, '0')
    return "${dt.dayOfMonth} $monthName ${dt.year} à $hour h $minute"
}

private val monthsShort = listOf(
    "janv.", "févr.", "mars", "avr.", "mai", "juin",
    "juil.", "août", "sept.", "oct.", "nov.", "déc.",
)
private val monthsLong = listOf(
    "janvier", "février", "mars", "avril", "mai", "juin",
    "juillet", "août", "septembre", "octobre", "novembre", "décembre",
)

/** Libellé court FR d'un mois (« janv. », « févr. »…), 1-indexé. */
fun monthShortLabel(month: Int): String = monthsShort.getOrNull(month - 1) ?: "?"

/** Libellé long FR d'un mois (« janvier », « février »…), 1-indexé. */
fun monthLongLabel(month: Int): String = monthsLong.getOrNull(month - 1) ?: "?"

/**
 * Format monétaire « 12,30 € ». Centimes → euros, séparateur décimal FR,
 * deux décimales obligatoires. `null` → chaîne vide.
 */
fun formatAmount(cents: Long?): String {
    if (cents == null) return ""
    val sign = if (cents < 0) "-" else ""
    val abs = kotlin.math.abs(cents)
    val euros = abs / 100
    val remainder = (abs % 100).toString().padStart(2, '0')
    return "$sign$euros,$remainder €"
}

/**
 * Parse une saisie utilisateur en centimes. Accepte « 12,30 », « 12.30 »,
 * « 12 » ou vide. Retourne `null` pour vide, `null` en cas d'erreur.
 */
fun parseAmountCents(raw: String): Long? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    val normalized = trimmed.replace(',', '.').replace(" ", "").replace(" ", "").removeSuffix("€").trim()
    val parts = normalized.split('.')
    return when (parts.size) {
        1 -> parts[0].toLongOrNull()?.let { it * 100 }
        2 -> {
            val euros = parts[0].toLongOrNull() ?: return null
            val fractional = parts[1].padEnd(2, '0').take(2).toLongOrNull() ?: return null
            euros * 100 + fractional
        }
        else -> null
    }
}

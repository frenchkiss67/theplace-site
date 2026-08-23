package com.theplace.receiptscanner.util

import kotlin.math.max
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * Date de fin de garantie (epoch ms) à partir de la date d'achat et de la
 * durée en mois. Retourne `null` si l'une des entrées est nulle.
 *
 * On ajoute les mois sur `LocalDate` pour gérer correctement les fins
 * de mois (un achat le 31 janvier + 1 mois → 28/29 février).
 */
fun warrantyEndMs(
    purchasedAt: Long?,
    warrantyMonths: Int?,
    zone: TimeZone = TimeZone.currentSystemDefault(),
): Long? {
    if (purchasedAt == null || warrantyMonths == null || warrantyMonths <= 0) return null
    val date: LocalDate = Instant.fromEpochMilliseconds(purchasedAt)
        .toLocalDateTime(zone)
        .date
    val end = date.plus(warrantyMonths, DateTimeUnit.MONTH)
    return LocalDateTime(end, LocalTime(0, 0)).toInstant(zone).toEpochMilliseconds()
}

/**
 * Nombre de jours entre `now` et l'expiration de garantie. Négatif si
 * expirée. `null` si pas de garantie configurée.
 */
fun daysUntilWarrantyEnd(
    now: Long,
    purchasedAt: Long?,
    warrantyMonths: Int?,
    zone: TimeZone = TimeZone.currentSystemDefault(),
): Int? {
    val endMs = warrantyEndMs(purchasedAt, warrantyMonths, zone) ?: return null
    val msPerDay = 24L * 60 * 60 * 1000
    // Arrondi vers le bas : on perd 23h59 plutôt que de gagner un faux jour.
    return ((endMs - now) / msPerDay).toInt()
}

/** Jours restants, clampé à zéro minimum (utile pour les affichages). */
fun daysUntilWarrantyEndClamped(
    now: Long,
    purchasedAt: Long?,
    warrantyMonths: Int?,
    zone: TimeZone = TimeZone.currentSystemDefault(),
): Int? = daysUntilWarrantyEnd(now, purchasedAt, warrantyMonths, zone)?.let { max(0, it) }

/** Format compact « 14/05/2026 » de la date, sans l'heure. */
fun formatDateOnly(epochMs: Long, zone: TimeZone = TimeZone.currentSystemDefault()): String {
    val dt = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(zone)
    fun Int.pad() = toString().padStart(2, '0')
    return "${dt.dayOfMonth.pad()}/${dt.monthNumber.pad()}/${dt.year}"
}

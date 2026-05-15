package com.theplace.receiptscanner.util

import com.theplace.receiptscanner.data.Receipt
import com.theplace.receiptscanner.data.ReceiptCategory
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Total dépensé (centimes) par catégorie pour un mois donné. */
data class CategorySpend(val category: ReceiptCategory?, val totalCents: Long)

/** Total dépensé (centimes) pour un mois calendaire. */
data class MonthSpend(val year: Int, val month: Int, val totalCents: Long)

/**
 * Agrège les tickets du mois calendaire courant par catégorie. Inclut
 * les tickets non classés sous `category = null`. Ignore les tickets
 * sans `totalCents`.
 */
fun List<Receipt>.statsByCategoryForMonth(
    nowMs: Long,
    zone: TimeZone = TimeZone.currentSystemDefault(),
): List<CategorySpend> {
    val now = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(zone)
    val filtered = filter { receipt ->
        if (receipt.totalCents == null) return@filter false
        val dt = Instant.fromEpochMilliseconds(receipt.createdAt).toLocalDateTime(zone)
        dt.year == now.year && dt.monthNumber == now.monthNumber
    }
    return filtered.groupBy { it.category }
        .map { (category, list) -> CategorySpend(category, list.sumOf { it.totalCents ?: 0 }) }
        .sortedByDescending { it.totalCents }
}

/**
 * Total dépensé par mois sur les 12 derniers mois calendaires
 * (incluant le mois courant). Mois sans dépense → 0.
 */
fun List<Receipt>.statsByLast12Months(
    nowMs: Long,
    zone: TimeZone = TimeZone.currentSystemDefault(),
): List<MonthSpend> {
    val now = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(zone)
    val months = buildList {
        var y = now.year
        var m = now.monthNumber
        repeat(12) {
            add(y to m)
            m--
            if (m == 0) { m = 12; y-- }
        }
    }.reversed() // chronologique

    val totals = filter { it.totalCents != null }
        .groupBy { r ->
            val dt = Instant.fromEpochMilliseconds(r.createdAt).toLocalDateTime(zone)
            dt.year to dt.monthNumber
        }
        .mapValues { (_, list) -> list.sumOf { it.totalCents ?: 0 } }

    return months.map { (y, m) -> MonthSpend(y, m, totals[y to m] ?: 0) }
}

/** Libellé court d'un mois (« janv. », « févr. », etc.). FR uniquement pour l'instant. */
fun monthShortLabel(month: Int): String = when (month) {
    1 -> "janv."
    2 -> "févr."
    3 -> "mars"
    4 -> "avr."
    5 -> "mai"
    6 -> "juin"
    7 -> "juil."
    8 -> "août"
    9 -> "sept."
    10 -> "oct."
    11 -> "nov."
    12 -> "déc."
    else -> "?"
}


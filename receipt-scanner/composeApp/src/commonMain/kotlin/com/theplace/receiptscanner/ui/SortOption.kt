package com.theplace.receiptscanner.ui

import com.theplace.receiptscanner.data.Receipt
import com.theplace.receiptscanner.data.ReceiptCategory
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Critères de tri proposés à l'utilisateur pour la liste des tickets. */
enum class SortOption {
    DateDesc,
    DateAsc,
    NameAsc,
    SizeDesc,
}

/** Comparator associé à chaque option de tri. */
fun List<Receipt>.sortedBy(option: SortOption): List<Receipt> = when (option) {
    SortOption.DateDesc -> sortedByDescending { it.createdAt }
    SortOption.DateAsc -> sortedBy { it.createdAt }
    SortOption.NameAsc -> sortedBy { it.name.lowercase() }
    SortOption.SizeDesc -> sortedByDescending { it.sizeBytes }
}

/** Filtre insensible à la casse sur le nom du ticket. Chaîne vide = pas de filtre. */
fun List<Receipt>.filteredByQuery(query: String): List<Receipt> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return this
    return filter { it.name.contains(trimmed, ignoreCase = true) }
}

/** Filtre catégorie modélisant les 3 cas distincts : tout / non classé / une catégorie. */
sealed interface CategoryFilter {
    data object All : CategoryFilter
    data object Uncategorised : CategoryFilter
    data class Of(val category: ReceiptCategory) : CategoryFilter
}

fun List<Receipt>.filteredBy(filter: CategoryFilter): List<Receipt> = when (filter) {
    CategoryFilter.All -> this
    CategoryFilter.Uncategorised -> filter { it.category == null }
    is CategoryFilter.Of -> filter { it.category == filter.category }
}

/** Somme des montants saisis, en centimes. Ignore les tickets sans montant. */
fun List<Receipt>.totalCents(): Long = sumOf { it.totalCents ?: 0L }

/**
 * Somme des montants saisis pour les tickets dont `createdAt` tombe dans le
 * mois courant (zone système).
 */
fun List<Receipt>.monthTotalCents(
    nowMs: Long,
    zone: TimeZone = TimeZone.currentSystemDefault(),
): Long {
    val now = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(zone)
    return filter { receipt ->
        val dt = Instant.fromEpochMilliseconds(receipt.createdAt).toLocalDateTime(zone)
        dt.year == now.year && dt.monthNumber == now.monthNumber
    }.totalCents()
}

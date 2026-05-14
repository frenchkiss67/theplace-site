package com.theplace.receiptscanner.ui

import com.theplace.receiptscanner.data.Receipt

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

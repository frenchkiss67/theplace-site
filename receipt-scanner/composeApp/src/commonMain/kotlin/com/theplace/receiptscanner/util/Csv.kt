package com.theplace.receiptscanner.util

import com.theplace.receiptscanner.data.Receipt
import com.theplace.receiptscanner.data.ReceiptCategory

/**
 * Produit un CSV RFC4180 minimal : virgule comme séparateur, doubles
 * guillemets pour échapper. UTF-8 attendu par le caller à l'écriture.
 *
 * Colonnes : id, dateScan, dateAchat, nom, categorie, montant, fichier.
 */
fun buildReceiptsCsv(receipts: List<Receipt>): String {
    val header = listOf("id", "dateScan", "dateAchat", "nom", "categorie", "montant", "fichier")
    val builder = StringBuilder()
    builder.append(header.joinToString(",")).append('\n')
    for (r in receipts) {
        val cells = listOf(
            r.id.toString(),
            formatDateOnly(r.createdAt),
            r.purchasedAt?.let { formatDateOnly(it) } ?: "",
            csvEscape(r.name),
            categoryLabel(r.category),
            r.totalCents?.let { formatAmount(it).removeSuffix(" €") } ?: "",
            csvEscape(r.fileName),
        )
        builder.append(cells.joinToString(",")).append('\n')
    }
    return builder.toString()
}

/** Labels CSV non localisés — les enums sont stables, les CSV doivent l'être aussi. */
private fun categoryLabel(category: ReceiptCategory?): String = category?.name.orEmpty()

private fun csvEscape(value: String): String {
    val needsQuoting = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
    val escaped = value.replace("\"", "\"\"")
    return if (needsQuoting) "\"$escaped\"" else escaped
}

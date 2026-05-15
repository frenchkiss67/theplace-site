package com.theplace.receiptscanner.data

/**
 * Catégorie d'un ticket. `null` = non catégorisé. Stockée en base sous
 * sa forme `name` (id stable, indépendant des renommages/traductions).
 */
enum class ReceiptCategory {
    Groceries,
    Restaurant,
    Fuel,
    Health,
    Shopping,
    Other,
    ;

    companion object {
        fun fromStorage(value: String?): ReceiptCategory? =
            value?.let { runCatching { valueOf(it) }.getOrNull() }
    }
}

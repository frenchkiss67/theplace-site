package com.theplace.receiptscanner.data

/**
 * Modèle de domaine, indépendant de toute persistance ou plateforme.
 *
 * `totalCents` : montant total saisi par l'utilisateur en centimes
 * (Long pour éviter les flottants). `null` = non saisi.
 * `category` : `null` = non classé.
 */
data class Receipt(
    val id: Long = 0,
    val name: String,
    val fileName: String,
    val pageCount: Int,
    val sizeBytes: Long,
    val createdAt: Long,
    val category: ReceiptCategory? = null,
    val totalCents: Long? = null,
    /** Texte extrait par OCR (peut être `null` tant que la passe n'a pas tourné). */
    val extractedText: String? = null,
)

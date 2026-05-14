package com.theplace.receiptscanner.data

/**
 * Modèle de domaine, indépendant de toute persistance ou plateforme.
 */
data class Receipt(
    val id: Long = 0,
    val name: String,
    val fileName: String,
    val pageCount: Int,
    val sizeBytes: Long,
    val createdAt: Long,
)

package com.theplace.receiptscanner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Représentation Room (Android). Séparée du modèle commun `Receipt` pour
 * laisser commonMain libre de toute annotation plateforme.
 */
@Entity(tableName = "receipts")
internal data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val fileName: String,
    val pageCount: Int,
    val sizeBytes: Long,
    val createdAt: Long,
)

internal fun ReceiptEntity.toDomain(): Receipt = Receipt(
    id = id,
    name = name,
    fileName = fileName,
    pageCount = pageCount,
    sizeBytes = sizeBytes,
    createdAt = createdAt,
)

internal fun Receipt.toEntity(): ReceiptEntity = ReceiptEntity(
    id = id,
    name = name,
    fileName = fileName,
    pageCount = pageCount,
    sizeBytes = sizeBytes,
    createdAt = createdAt,
)

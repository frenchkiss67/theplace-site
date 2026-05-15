package com.theplace.receiptscanner.data

import com.theplace.receiptscanner.platform.PlatformScanResult
import kotlinx.coroutines.flow.Flow

/**
 * Interface commune au-dessus de la persistance.
 * L'implémentation Android branche Room ; iOS pourra brancher SQLDelight,
 * Core Data, ou un store de fichiers + JSON.
 */
interface ReceiptRepository {
    fun observeAll(): Flow<List<Receipt>>
    suspend fun findById(id: Long): Receipt?

    /**
     * Copie le PDF produit par le scanner dans le stockage privé de l'app
     * et insère les métadonnées correspondantes.
     */
    suspend fun addFromScan(scan: PlatformScanResult): Receipt

    suspend fun rename(receipt: Receipt, newName: String)

    /** Replace en base toutes les colonnes mutables du ticket (catégorie, montant…). */
    suspend fun update(receipt: Receipt)

    suspend fun delete(receipt: Receipt)
}

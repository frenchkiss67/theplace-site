package com.theplace.receiptscanner.data

import com.theplace.receiptscanner.platform.PlatformExportTarget
import com.theplace.receiptscanner.platform.PlatformScanResult
import kotlinx.coroutines.flow.Flow

/** Issue d'une opération `restoreFromFolder`. */
sealed interface RestoreOutcome {
    data class Success(val imported: Int, val skipped: Int) : RestoreOutcome
    data class Failure(val message: String, val imported: Int = 0) : RestoreOutcome
}

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

    /**
     * Réimporte les PDFs d'un dossier SAF dans la base. Les `fileName`
     * déjà présents sont ignorés (idempotent). Utile après un changement
     * de téléphone ou un wipe : on pointe vers le dossier de backup
     * choisi via `BackupSettings` et l'app récupère ses tickets.
     */
    suspend fun restoreFromFolder(target: PlatformExportTarget): RestoreOutcome
}

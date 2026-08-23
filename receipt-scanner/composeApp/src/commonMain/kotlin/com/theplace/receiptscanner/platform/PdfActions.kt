package com.theplace.receiptscanner.platform

import com.theplace.receiptscanner.data.Receipt

/**
 * Actions sur le PDF d'un ticket qui dépendent de la plateforme.
 * Android : Intent VIEW + chooser SEND via FileProvider, export via SAF.
 * iOS    : UIDocumentInteractionController / UIActivityViewController.
 */
interface PdfActions {
    fun open(receipt: Receipt)
    fun share(receipt: Receipt, displayName: String)

    /** Partage groupé via `ACTION_SEND_MULTIPLE` côté Android. */
    fun shareMultiple(receipts: List<Receipt>, displayLabel: String)

    /**
     * Copie les PDFs sélectionnés vers la cible choisie par l'utilisateur
     * (dossier SAF côté Android). Suspendable car la copie passe par
     * `Dispatchers.IO` côté plateforme.
     */
    suspend fun exportTo(receipts: List<Receipt>, target: PlatformExportTarget): ExportOutcome
}

/** Résultat d'un export utilisateur. */
sealed interface ExportOutcome {
    data class Success(val exportedCount: Int) : ExportOutcome
    data class Failure(val message: String, val partialCount: Int = 0) : ExportOutcome
}

package com.theplace.receiptscanner.data

/** Issue d'un archivage : succès avec le ticket inséré, ou échec
 *  (disque plein, fichier corrompu…). Pendant de [RestoreOutcome]. */
sealed interface SaveScanOutcome {
    data class Success(val receipt: Receipt) : SaveScanOutcome
    data class Failure(val message: String) : SaveScanOutcome
}

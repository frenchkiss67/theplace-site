package com.theplace.receiptscanner.platform

import androidx.compose.runtime.Composable

/**
 * Référence opaque vers le résultat du scan, dont la structure interne dépend
 * de la plateforme (Uri Android, NSURL iOS…). Seul le repo plateforme la
 * consomme pour copier le PDF.
 */
expect class PlatformScanResult

/** Issue d'un scan utilisateur. */
sealed interface ScanOutcome {
    data class Success(val result: PlatformScanResult) : ScanOutcome
    data object Cancelled : ScanOutcome
    data class Failure(val message: String) : ScanOutcome
}

/** Handle pour déclencher l'ouverture du scanner depuis l'UI. */
interface DocumentScannerLauncher {
    fun launch()
}

/**
 * Lifecycle Compose : retourne un launcher mémoïsé pour lancer le scan.
 * Android : ML Kit Document Scanner. iOS (à venir) : VNDocumentCameraViewController.
 */
@Composable
expect fun rememberDocumentScannerLauncher(
    onResult: (ScanOutcome) -> Unit,
): DocumentScannerLauncher

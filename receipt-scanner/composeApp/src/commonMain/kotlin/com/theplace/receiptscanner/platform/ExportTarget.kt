package com.theplace.receiptscanner.platform

import androidx.compose.runtime.Composable

/**
 * Référence opaque vers la destination d'export (dossier SAF Android,
 * NSURL iOS…). Seul `PdfActions.exportTo` côté plateforme la consomme.
 */
expect class PlatformExportTarget

/** Handle pour ouvrir le sélecteur de dossier depuis l'UI. */
interface ExportFolderLauncher {
    fun launch()
}

/**
 * Lifecycle Compose : retourne un launcher mémoïsé pour choisir un dossier
 * de destination. Android : `ActivityResultContracts.OpenDocumentTree`.
 * `target` vaut `null` si l'utilisateur a annulé.
 */
@Composable
expect fun rememberExportFolderLauncher(
    onResult: (PlatformExportTarget?) -> Unit,
): ExportFolderLauncher

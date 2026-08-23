package com.theplace.receiptscanner.platform

import androidx.compose.runtime.Composable

/**
 * Référence opaque vers un document unique créé par l'utilisateur via
 * SAF (Android : `Uri` issu de `CreateDocument`).
 */
expect class PlatformDocumentTarget

interface CreateDocumentLauncher {
    fun launch()
}

/**
 * Picker SAF pour créer un nouveau document (`text/csv`, `application/zip`…).
 * Android : `ActivityResultContracts.CreateDocument(mimeType)` avec un
 * `defaultName` proposé.
 */
@Composable
expect fun rememberCreateDocumentLauncher(
    mimeType: String,
    defaultName: String,
    onResult: (PlatformDocumentTarget?) -> Unit,
): CreateDocumentLauncher

/**
 * Écriture d'un contenu texte dans la cible SAF. Suspend car la copie
 * passe sur `Dispatchers.IO` côté Android.
 */
interface DocumentWriter {
    suspend fun writeText(target: PlatformDocumentTarget, content: String): Boolean
}

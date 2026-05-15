package com.theplace.receiptscanner.platform

import kotlinx.coroutines.flow.StateFlow

/**
 * Réglages de la sauvegarde périodique vers un dossier SAF utilisateur.
 *
 * `folderLabel` : libellé court affiché à l'utilisateur (dernier segment
 *   du dossier SAF côté Android, ou chaîne vide si rien de choisi).
 * `enabled` : interrupteur principal — `true` exige un dossier configuré.
 */
interface BackupSettings {
    val enabled: StateFlow<Boolean>
    val folderLabel: StateFlow<String>

    /** `true` si un dossier est configuré et l'auto-backup peut tourner. */
    val isReady: Boolean

    fun setFolder(target: PlatformExportTarget?)
    fun setEnabled(enabled: Boolean)
}

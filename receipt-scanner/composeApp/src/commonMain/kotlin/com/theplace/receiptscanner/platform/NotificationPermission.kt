package com.theplace.receiptscanner.platform

import androidx.compose.runtime.Composable

/**
 * Demande la permission « envoyer des notifications » uniquement
 * quand elle est requise et pas déjà accordée. Android 13+ ; no-op
 * sur API < 33 ou si déjà acceptée. iOS : à brancher sur
 * `UNUserNotificationCenter.requestAuthorization` quand la cible
 * sera activée.
 */
interface NotificationPermissionRequester {
    fun requestIfNeeded()
}

@Composable
expect fun rememberNotificationPermissionRequester(): NotificationPermissionRequester

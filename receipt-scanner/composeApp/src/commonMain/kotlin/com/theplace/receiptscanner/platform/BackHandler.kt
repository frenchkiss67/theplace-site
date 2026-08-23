package com.theplace.receiptscanner.platform

import androidx.compose.runtime.Composable

/**
 * Intercepte le bouton retour système (Android) ou le geste retour (iOS,
 * une fois la cible activée) pour exécuter une action contextuelle —
 * typiquement quitter le mode sélection avant de pop la nav stack.
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)

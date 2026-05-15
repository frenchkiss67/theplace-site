package com.theplace.receiptscanner.platform

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow

/**
 * État persistant et capacités du verrouillage biométrique de l'app.
 * `enabled` est sauvegardé par la plateforme (SharedPreferences Android).
 */
interface AppLockSettings {
    val enabled: StateFlow<Boolean>
    val isBiometricAvailable: Boolean
    fun setEnabled(value: Boolean)
}

/**
 * Garde-frontière biométrique : tant que l'app n'est pas déverrouillée
 * lors de la session courante, affiche un écran de verrouillage à la
 * place de `content`. Une fois déverrouillé (auth ou `enabled=false`),
 * l'utilisateur garde l'accès jusqu'à la fin du process.
 */
@Composable
expect fun BiometricGate(
    settings: AppLockSettings,
    content: @Composable () -> Unit,
)

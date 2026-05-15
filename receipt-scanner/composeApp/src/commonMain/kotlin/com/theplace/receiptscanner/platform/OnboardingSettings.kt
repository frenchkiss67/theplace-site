package com.theplace.receiptscanner.platform

import kotlinx.coroutines.flow.StateFlow

/**
 * Suit l'état « tutoriel premier lancement vu ». Marqué une fois, jamais
 * réaffiché. Implémentation : SharedPreferences côté Android.
 */
interface OnboardingSettings {
    val completed: StateFlow<Boolean>
    fun markCompleted()
}

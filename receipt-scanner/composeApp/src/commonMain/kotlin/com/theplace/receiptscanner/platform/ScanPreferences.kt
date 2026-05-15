package com.theplace.receiptscanner.platform

import kotlinx.coroutines.flow.StateFlow

/**
 * Préférences liées au scanner : mode rafale (relance auto après
 * chaque scan réussi) et tout futur réglage (qualité, multi-page…).
 */
interface ScanPreferences {
    val continuousScan: StateFlow<Boolean>
    fun setContinuousScan(value: Boolean)
}

package com.theplace.receiptscanner.platform

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val PREFS = "scan_prefs"
private const val KEY_CONTINUOUS = "continuous_scan"

internal class AndroidScanPreferences(context: Context) : ScanPreferences {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _continuous = MutableStateFlow(prefs.getBoolean(KEY_CONTINUOUS, false))
    override val continuousScan: StateFlow<Boolean> = _continuous.asStateFlow()

    override fun setContinuousScan(value: Boolean) {
        prefs.edit().putBoolean(KEY_CONTINUOUS, value).apply()
        _continuous.value = value
    }
}

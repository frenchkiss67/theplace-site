package com.theplace.receiptscanner.platform

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val PREFS = "onboarding"
private const val KEY_COMPLETED = "completed"

internal class AndroidOnboardingSettings(context: Context) : OnboardingSettings {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _completed = MutableStateFlow(prefs.getBoolean(KEY_COMPLETED, false))
    override val completed: StateFlow<Boolean> = _completed.asStateFlow()

    override fun markCompleted() {
        prefs.edit().putBoolean(KEY_COMPLETED, true).apply()
        _completed.value = true
    }
}

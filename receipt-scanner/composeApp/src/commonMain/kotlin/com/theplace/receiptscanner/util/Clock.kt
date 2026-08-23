package com.theplace.receiptscanner.util

import kotlinx.datetime.Clock

/** Indirection pour faciliter les tests. */
fun nowMs(): Long = Clock.System.now().toEpochMilliseconds()

package com.theplace.receiptscanner.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

private const val WORK_NAME = "receipt_scanner_warranty"

/**
 * Planifie un balayage quotidien des garanties via WorkManager.
 * Idempotent : sûr de l'appeler à chaque démarrage du process.
 */
internal object WarrantyScheduler {
    fun ensureScheduled(context: Context) {
        val request = PeriodicWorkRequestBuilder<WarrantyWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS,
            flexTimeInterval = 6,
            flexTimeIntervalUnit = TimeUnit.HOURS,
        ).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}

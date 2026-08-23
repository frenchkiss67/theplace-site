package com.theplace.receiptscanner.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

private const val WORK_NAME = "receipt_scanner_backup"

/**
 * Aligne l'état WorkManager sur l'état du backup (capturé par
 * `snapshot()` pour éviter une dépendance cyclique sur `BackupSettings`).
 *
 * Si `enabled && isReady` : enqueue/keep une PeriodicWorkRequest 1×/jour
 * (flex 6h), sans contrainte réseau. Sinon : `cancelUniqueWork`.
 */
internal class BackupScheduler(
    private val context: Context,
    private val snapshot: () -> Snapshot,
) {
    data class Snapshot(val enabled: Boolean, val isReady: Boolean)

    fun refresh() {
        val state = snapshot()
        val manager = WorkManager.getInstance(context)
        if (state.enabled && state.isReady) {
            val request = PeriodicWorkRequestBuilder<BackupWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.DAYS,
                flexTimeInterval = 6,
                flexTimeIntervalUnit = TimeUnit.HOURS,
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build(),
                )
                .build()
            manager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        } else {
            manager.cancelUniqueWork(WORK_NAME)
        }
    }
}

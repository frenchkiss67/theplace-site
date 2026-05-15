package com.theplace.receiptscanner.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.theplace.receiptscanner.ReceiptScannerApp
import com.theplace.receiptscanner.util.daysUntilWarrantyEnd
import kotlinx.coroutines.flow.first

private const val PREFS = "warranty_notif"
private const val KEY_NOTIFIED_IDS = "notified_ids"
private const val REMIND_DAYS = 30

/**
 * Tâche périodique 1×/jour : pour chaque ticket dont la garantie expire
 * dans ≤ 30 jours, poste une notif si on ne l'a pas encore notifié.
 * L'état « déjà notifié » est gardé en SharedPreferences pour éviter le
 * spam quotidien (un ticket = une notif J-30, pas une par jour pendant 30).
 */
internal class WarrantyWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? ReceiptScannerApp ?: return Result.failure()
        val receipts = app.services.repository.observeAll().first()
        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val notified = prefs.getStringSet(KEY_NOTIFIED_IDS, emptySet())?.toMutableSet()
            ?: mutableSetOf()
        val notifier = WarrantyNotifier(applicationContext).also { it.ensureChannel() }

        val now = System.currentTimeMillis()
        for (receipt in receipts) {
            val days = daysUntilWarrantyEnd(now, receipt.purchasedAt, receipt.warrantyMonths)
                ?: continue
            if (days !in 0..REMIND_DAYS) continue
            val key = receipt.id.toString()
            if (key in notified) continue
            val posted = notifier.notifyExpiringSoon(receipt.id, receipt.name, days)
            if (posted) notified += key
        }
        prefs.edit().putStringSet(KEY_NOTIFIED_IDS, notified).apply()
        return Result.success()
    }
}

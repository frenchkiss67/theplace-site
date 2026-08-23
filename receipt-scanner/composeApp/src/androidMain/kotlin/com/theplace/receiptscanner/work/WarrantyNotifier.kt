package com.theplace.receiptscanner.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.theplace.receiptscanner.MainActivity
import com.theplace.receiptscanner.R as AppR

internal const val WARRANTY_CHANNEL_ID = "warranties"

internal class WarrantyNotifier(private val context: Context) {

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(WARRANTY_CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            WARRANTY_CHANNEL_ID,
            // Libellés en dur côté Android (impossible d'appeler getString depuis
            // un contexte non-composable pour les Compose Resources).
            "Garanties qui expirent",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Rappels J-30 avant la fin de garantie de vos tickets."
        }
        manager.createNotificationChannel(channel)
    }

    /**
     * Renvoie `true` si la notification a été postée, `false` si la
     * permission POST_NOTIFICATIONS manque (Android 13+).
     */
    fun notifyExpiringSoon(receiptId: Long, receiptName: String, daysLeft: Int): Boolean {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = androidx.core.app.PendingIntentCompat.getActivity(
            context, receiptId.toInt(), intent, 0, false,
        )
        val notif = NotificationCompat.Builder(context, WARRANTY_CHANNEL_ID)
            .setSmallIcon(AppR.mipmap.ic_launcher)
            .setContentTitle("Garantie bientôt expirée")
            .setContentText("$receiptName — il reste $daysLeft jour(s).")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        return runCatching {
            manager.notify(receiptId.toInt(), notif)
            true
        }.getOrDefault(false)
    }
}

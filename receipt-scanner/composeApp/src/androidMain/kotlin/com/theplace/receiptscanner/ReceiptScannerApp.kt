package com.theplace.receiptscanner

import android.app.Application
import com.theplace.receiptscanner.work.WarrantyNotifier
import com.theplace.receiptscanner.work.WarrantyScheduler

class ReceiptScannerApp : Application() {
    lateinit var services: ServiceLocator
        private set

    override fun onCreate() {
        super.onCreate()
        services = ServiceLocator(this)

        // Canal de notif + tâche périodique pour les garanties. `runCatching`
        // pour tolérer les environnements sans WorkManager initialisé
        // (tests Robolectric qui n'utilisent pas de WM stub).
        runCatching {
            WarrantyNotifier(this).ensureChannel()
            WarrantyScheduler.ensureScheduled(this)
        }
    }
}

package com.theplace.receiptscanner

import android.app.Application
import com.theplace.receiptscanner.work.WarrantyNotifier
import com.theplace.receiptscanner.work.WarrantyScheduler

class ReceiptScannerApp : Application() {
    lateinit var services: ServiceLocator
        private set

    override fun onCreate() {
        super.onCreate()
        // `AndroidBackupSettings` aligne lui-même WorkManager dans son init,
        // pas besoin d'appel explicite ici.
        services = ServiceLocator(this)

        // Canal de notif + tâche périodique pour les garanties.
        WarrantyNotifier(this).ensureChannel()
        WarrantyScheduler.ensureScheduled(this)
    }
}

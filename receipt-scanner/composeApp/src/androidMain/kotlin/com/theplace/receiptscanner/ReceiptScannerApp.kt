package com.theplace.receiptscanner

import android.app.Application

class ReceiptScannerApp : Application() {
    lateinit var services: ServiceLocator
        private set

    override fun onCreate() {
        super.onCreate()
        // `AndroidBackupSettings` aligne lui-même WorkManager dans son init,
        // pas besoin d'appel explicite ici.
        services = ServiceLocator(this)
    }
}

package com.theplace.receiptscanner

import android.app.Application

class ReceiptScannerApp : Application() {
    lateinit var services: ServiceLocator
        private set

    override fun onCreate() {
        super.onCreate()
        services = ServiceLocator(this)
    }
}

package com.theplace.receiptscanner

import android.content.Context
import com.theplace.receiptscanner.data.AndroidReceiptRepository
import com.theplace.receiptscanner.data.ReceiptRepository
import com.theplace.receiptscanner.platform.AndroidAppLockSettings
import com.theplace.receiptscanner.platform.AndroidBackupSettings
import com.theplace.receiptscanner.platform.AndroidDocumentWriter
import com.theplace.receiptscanner.platform.AndroidOnboardingSettings
import com.theplace.receiptscanner.platform.AndroidPdfActions
import com.theplace.receiptscanner.platform.AndroidTextRecognizer
import com.theplace.receiptscanner.platform.AppLockSettings
import com.theplace.receiptscanner.platform.BackupSettings
import com.theplace.receiptscanner.platform.DocumentWriter
import com.theplace.receiptscanner.platform.OnboardingSettings
import com.theplace.receiptscanner.platform.PdfActions
import com.theplace.receiptscanner.platform.PdfStorage
import com.theplace.receiptscanner.platform.TextRecognizer

/**
 * DI minimale côté Android : un seul Repository, une seule instance
 * PdfActions, partagés via l'Application.
 */
class ServiceLocator(context: Context) {
    private val appContext = context.applicationContext
    private val storage = PdfStorage(appContext)

    val repository: ReceiptRepository =
        AndroidReceiptRepository.from(appContext)

    val pdfActions: PdfActions =
        AndroidPdfActions(appContext, storage)

    val appLock: AppLockSettings =
        AndroidAppLockSettings(appContext)

    val backupSettings: BackupSettings =
        AndroidBackupSettings(appContext)

    val textRecognizer: TextRecognizer =
        AndroidTextRecognizer(appContext)

    val onboarding: OnboardingSettings =
        AndroidOnboardingSettings(appContext)

    val documentWriter: DocumentWriter =
        AndroidDocumentWriter(appContext)
}

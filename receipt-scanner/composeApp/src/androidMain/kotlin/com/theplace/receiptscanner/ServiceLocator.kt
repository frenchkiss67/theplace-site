package com.theplace.receiptscanner

import android.content.Context
import com.theplace.receiptscanner.data.AndroidReceiptRepository
import com.theplace.receiptscanner.data.ReceiptRepository
import com.theplace.receiptscanner.platform.AndroidPdfActions
import com.theplace.receiptscanner.platform.PdfActions
import com.theplace.receiptscanner.platform.PdfStorage

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
}

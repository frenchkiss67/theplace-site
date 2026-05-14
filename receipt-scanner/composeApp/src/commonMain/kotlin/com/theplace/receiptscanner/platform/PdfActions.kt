package com.theplace.receiptscanner.platform

import com.theplace.receiptscanner.data.Receipt

/**
 * Actions sur le PDF d'un ticket qui dépendent de la plateforme.
 * Android : Intent VIEW + chooser SEND via FileProvider.
 * iOS    : UIDocumentInteractionController / UIActivityViewController.
 */
interface PdfActions {
    fun open(receipt: Receipt)
    fun share(receipt: Receipt, displayName: String)
}

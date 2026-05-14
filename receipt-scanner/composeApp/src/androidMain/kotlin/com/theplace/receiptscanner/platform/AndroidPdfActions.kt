package com.theplace.receiptscanner.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.theplace.receiptscanner.data.Receipt

internal class AndroidPdfActions(
    private val context: Context,
    private val storage: PdfStorage,
) : PdfActions {

    override fun open(receipt: Receipt) {
        val uri = storage.shareUri(receipt.fileName)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            )
        }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "Aucune application pour ouvrir les PDF", Toast.LENGTH_LONG).show()
        }
    }

    override fun share(receipt: Receipt, displayName: String) {
        val uri = storage.shareUri(receipt.fileName)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, displayName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, displayName).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}

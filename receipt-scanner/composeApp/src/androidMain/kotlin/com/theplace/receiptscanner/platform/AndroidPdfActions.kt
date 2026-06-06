package com.theplace.receiptscanner.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
import com.theplace.receiptscanner.data.Receipt
import com.theplace.receiptscanner.util.ensurePdfSuffix
import com.theplace.receiptscanner.util.sanitizeFileName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    override fun shareMultiple(receipts: List<Receipt>, displayLabel: String) {
        if (receipts.isEmpty()) return
        val uris = ArrayList(receipts.map { storage.shareUri(it.fileName) })
        val send = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "application/pdf"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            putExtra(Intent.EXTRA_SUBJECT, displayLabel)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, displayLabel).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    override suspend fun exportTo(
        receipts: List<Receipt>,
        target: PlatformExportTarget,
    ): ExportOutcome = withContext(Dispatchers.IO) {
        if (receipts.isEmpty()) return@withContext ExportOutcome.Success(0)

        val resolver = context.contentResolver
        val parent = DocumentsContract.buildDocumentUriUsingTree(
            target.treeUri,
            DocumentsContract.getTreeDocumentId(target.treeUri),
        )

        var exported = 0
        var lastError: String? = null
        for (receipt in receipts) {
            val source = storage.file(receipt.fileName)
            if (!source.exists()) {
                lastError = "Fichier introuvable : ${receipt.fileName}"
                continue
            }
            val docName = ensurePdfSuffix(sanitizeFileName(receipt.name).ifBlank { receipt.fileName })
            val destUri: Uri? = try {
                DocumentsContract.createDocument(
                    resolver,
                    parent,
                    "application/pdf",
                    docName,
                )
            } catch (t: Throwable) {
                lastError = t.message ?: "Création du document refusée"
                null
            } ?: continue

            runCatching {
                resolver.openOutputStream(destUri)?.use { output ->
                    source.inputStream().use { input -> input.copyTo(output) }
                } ?: error("Flux de sortie nul pour $destUri")
                exported++
            }.onFailure { lastError = it.message ?: "Copie échouée" }
        }

        when {
            exported == receipts.size -> ExportOutcome.Success(exported)
            exported == 0 -> ExportOutcome.Failure(
                message = lastError ?: "Aucun fichier exporté",
                partialCount = 0,
            )
            else -> ExportOutcome.Failure(
                message = lastError ?: "Export partiel",
                partialCount = exported,
            )
        }
    }
}


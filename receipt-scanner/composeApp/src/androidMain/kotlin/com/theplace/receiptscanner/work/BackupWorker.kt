package com.theplace.receiptscanner.work

import android.content.Context
import android.provider.DocumentsContract
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.theplace.receiptscanner.ReceiptScannerApp
import com.theplace.receiptscanner.platform.AndroidBackupSettings
import com.theplace.receiptscanner.platform.PdfStorage
import kotlinx.coroutines.flow.first

/**
 * Tâche de fond périodique : copie les PDFs des tickets archivés vers
 * le dossier SAF choisi par l'utilisateur, en évitant les doublons via
 * un set de `fileName` déjà exportés (persistant en SharedPreferences).
 */
internal class BackupWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? ReceiptScannerApp ?: return Result.failure()
        val services = app.services
        val backup = services.backupSettings as? AndroidBackupSettings ?: return Result.failure()

        if (!backup.enabled.first() || !backup.isReady) return Result.success()

        val treeUri = backup.folderUri() ?: return Result.success()
        val parent = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri),
        )

        val storage = PdfStorage(applicationContext)
        val resolver = applicationContext.contentResolver
        val receipts = services.repository.observeAll().first()
        val already = backup.exportedFileNames()

        var copied = 0
        var errors = 0
        for (receipt in receipts) {
            if (receipt.fileName in already) continue
            val source = storage.file(receipt.fileName)
            if (!source.exists()) continue

            val docName = if (receipt.fileName.endsWith(".pdf", ignoreCase = true)) {
                receipt.fileName
            } else {
                "${receipt.fileName}.pdf"
            }
            val target = runCatching {
                DocumentsContract.createDocument(resolver, parent, "application/pdf", docName)
            }.getOrNull()
            if (target == null) {
                errors++
                continue
            }
            val ok = runCatching {
                resolver.openOutputStream(target)?.use { os ->
                    source.inputStream().use { it.copyTo(os) }
                } ?: error("no output stream")
            }.isSuccess
            if (ok) {
                backup.markExported(receipt.fileName)
                copied++
            } else {
                errors++
            }
        }
        // En cas d'erreur (permissions perdues, dossier supprimé), on demande
        // un retry exponentiel — succès même si rien à copier.
        return if (errors > 0 && copied == 0) Result.retry() else Result.success()
    }
}

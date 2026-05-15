package com.theplace.receiptscanner.data

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import com.theplace.receiptscanner.platform.PdfStorage
import com.theplace.receiptscanner.platform.PlatformExportTarget
import com.theplace.receiptscanner.platform.PlatformScanResult
import com.theplace.receiptscanner.platform.ThumbnailCache
import com.theplace.receiptscanner.util.defaultReceiptName
import com.theplace.receiptscanner.util.nowMs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

internal class AndroidReceiptRepository(
    private val context: Context,
    private val dao: ReceiptDao,
    private val storage: PdfStorage,
    private val thumbnails: ThumbnailCache,
) : ReceiptRepository {

    override fun observeAll(): Flow<List<Receipt>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun findById(id: Long): Receipt? = dao.findById(id)?.toDomain()

    override suspend fun addFromScan(scan: PlatformScanResult): Receipt {
        val fileName = storage.newFileName()
        val sizeBytes = storage.importFrom(scan.uri, fileName)
        val now = nowMs()
        val receipt = Receipt(
            name = defaultReceiptName(now),
            fileName = fileName,
            pageCount = scan.pageCount,
            sizeBytes = sizeBytes,
            createdAt = now,
        )
        val id = dao.insert(receipt.toEntity())
        return receipt.copy(id = id)
    }

    override suspend fun rename(receipt: Receipt, newName: String) {
        dao.update(receipt.copy(name = newName).toEntity())
    }

    override suspend fun update(receipt: Receipt) {
        dao.update(receipt.toEntity())
    }

    override suspend fun delete(receipt: Receipt) {
        storage.delete(receipt.fileName)
        thumbnails.delete(receipt.fileName)
        dao.delete(receipt.toEntity())
    }

    override suspend fun restoreFromFolder(target: PlatformExportTarget): RestoreOutcome =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val treeUri = target.treeUri
            val childrenUri = runCatching {
                DocumentsContract.buildChildDocumentsUriUsingTree(
                    treeUri,
                    DocumentsContract.getTreeDocumentId(treeUri),
                )
            }.getOrNull() ?: return@withContext RestoreOutcome.Failure("URI invalide")

            val existing = dao.observeAll().first().map { it.fileName }.toSet()
            var imported = 0
            var skipped = 0
            var lastError: String? = null

            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            )
            val cursor = resolver.query(childrenUri, projection, null, null, null)
                ?: return@withContext RestoreOutcome.Failure("Dossier illisible")

            cursor.use { c ->
                while (c.moveToNext()) {
                    val documentId = c.getString(0)
                    val displayName = c.getString(1) ?: continue
                    val mime = c.getString(2)
                    val lastModified = if (!c.isNull(3)) c.getLong(3) else nowMs()

                    if (mime != "application/pdf" && !displayName.endsWith(".pdf", true)) continue
                    val targetFileName = sanitizeFileName(displayName)
                    if (targetFileName in existing) {
                        skipped++
                        continue
                    }
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
                    runCatching { importOne(docUri, targetFileName, displayName, lastModified) }
                        .onSuccess { imported++ }
                        .onFailure { lastError = it.message ?: "Échec d'import" }
                }
            }
            if (lastError != null && imported == 0) {
                RestoreOutcome.Failure(lastError!!, imported)
            } else {
                RestoreOutcome.Success(imported, skipped)
            }
        }

    /** Copie un PDF SAF dans le stockage privé et insère le ticket. */
    private suspend fun importOne(
        sourceUri: Uri,
        targetFileName: String,
        displayName: String,
        lastModifiedMs: Long,
    ) {
        val sizeBytes = storage.importFrom(sourceUri, targetFileName)
        val pageCount = countPages(storage.file(targetFileName).absolutePath)
        val name = displayName.removeSuffix(".pdf").ifBlank { defaultReceiptName(lastModifiedMs) }
        val receipt = Receipt(
            name = name,
            fileName = targetFileName,
            pageCount = pageCount,
            sizeBytes = sizeBytes,
            createdAt = lastModifiedMs,
        )
        dao.insert(receipt.toEntity())
    }

    private fun countPages(absolutePath: String): Int = runCatching {
        ParcelFileDescriptor.open(java.io.File(absolutePath), ParcelFileDescriptor.MODE_READ_ONLY)
            .use { pfd -> PdfRenderer(pfd).use { it.pageCount } }
    }.getOrDefault(1)

    companion object {
        fun from(context: Context): AndroidReceiptRepository {
            val db = ReceiptDatabase.get(context)
            return AndroidReceiptRepository(
                context = context.applicationContext,
                dao = db.receiptDao(),
                storage = PdfStorage(context),
                thumbnails = ThumbnailCache(context),
            )
        }
    }
}

/** Sanitise un nom de fichier SAF restoré pour qu'il rentre dans `filesDir/receipts`. */
private fun sanitizeFileName(raw: String): String {
    val forbidden = charArrayOf('/', '\\', '?', '*', ':', '|', '"', '<', '>')
    val cleaned = raw.trim().map { if (it in forbidden) '_' else it }.joinToString("")
    return if (cleaned.endsWith(".pdf", ignoreCase = true)) cleaned else "$cleaned.pdf"
}

package com.theplace.receiptscanner.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Gère les fichiers PDF des tickets dans le dossier privé de l'app
 * (filesDir/receipts), accessible aux autres apps uniquement via FileProvider.
 */
class PdfStorage(private val context: Context) {

    private val dir: File =
        File(context.filesDir, "receipts").apply { if (!exists()) mkdirs() }

    private val authority: String
        get() = "${context.packageName}.fileprovider"

    fun newFileName(): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "ticket_$stamp.pdf"
    }

    fun file(fileName: String): File = File(dir, fileName)

    fun shareUri(fileName: String): Uri =
        FileProvider.getUriForFile(context, authority, file(fileName))

    /** Importe le PDF retourné par le scanner en le copiant dans notre stockage. */
    fun importFrom(sourceUri: Uri, targetFileName: String): Long {
        val target = file(targetFileName)
        context.contentResolver.openInputStream(sourceUri).use { input ->
            requireNotNull(input) { "Flux d'entrée nul pour $sourceUri" }
            target.outputStream().use { output -> input.copyTo(output) }
        }
        return target.length()
    }

    fun delete(fileName: String): Boolean = file(fileName).delete()
}

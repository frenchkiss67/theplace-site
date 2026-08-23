package com.theplace.receiptscanner.platform

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.theplace.receiptscanner.data.Receipt
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class AndroidTextRecognizer(private val context: Context) : TextRecognizer {

    // Modèle Latin embarqué — utilisable hors-ligne, pas de téléchargement.
    // `by lazy` : évite d'initialiser Play Services au démarrage de l'app
    // (crash Robolectric si non installé et création différée jusqu'au 1er OCR).
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    override suspend fun extractText(receipt: Receipt): String? = withContext(Dispatchers.IO) {
        val pdf = File(File(context.filesDir, "receipts"), receipt.fileName)
        if (!pdf.exists()) return@withContext null

        val builder = StringBuilder()
        runCatching {
            ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    for (i in 0 until renderer.pageCount) {
                        appendPageText(renderer, i, builder)
                    }
                }
            }
        }
        builder.toString().trim().takeIf { it.isNotBlank() }
    }

    private fun appendPageText(renderer: PdfRenderer, index: Int, out: StringBuilder) {
        // ~1200 px : compromis qualité OCR / mémoire (preview 1600, vignette 400).
        val bitmap = renderer.renderPageBitmap(index, targetWidth = 1200) ?: return
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            // Tasks.await blocant — OK ici, on est sur Dispatchers.IO.
            val result = runCatching { Tasks.await(recognizer.process(image)) }.getOrNull()
            if (result != null) {
                if (out.isNotEmpty()) out.append("\n\n")
                out.append(result.text)
            }
        } finally {
            bitmap.recycle()
        }
    }
}

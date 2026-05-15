package com.theplace.receiptscanner.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
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
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

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
        val page = renderer.openPage(index)
        try {
            // ~1200 px de large : compromis qualité OCR / mémoire.
            val targetWidth = 1200
            val scale = targetWidth.toFloat() / page.width
            val w = targetWidth
            val h = (page.height * scale).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.WHITE)
            }
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            val image = InputImage.fromBitmap(bitmap, 0)
            // Tasks.await blocant — OK ici, on est sur Dispatchers.IO.
            val result = runCatching { Tasks.await(recognizer.process(image)) }.getOrNull()
            if (result != null) {
                if (out.isNotEmpty()) out.append("\n\n")
                out.append(result.text)
            }
            bitmap.recycle()
        } finally {
            page.close()
        }
    }
}

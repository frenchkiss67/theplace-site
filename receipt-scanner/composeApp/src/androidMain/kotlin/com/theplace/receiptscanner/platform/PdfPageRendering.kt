package com.theplace.receiptscanner.platform

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer

/**
 * Rendu d'une page PDF en bitmap, calibré sur `targetWidth` (px). Fond
 * blanc pour éviter les zones transparentes affichées en noir. Renvoie
 * `null` si la page est illisible.
 *
 * Mutualisé entre vignettes (`ThumbnailCache`), preview (`PdfPreview`)
 * et OCR (`AndroidTextRecognizer`).
 */
internal fun PdfRenderer.renderPageBitmap(index: Int, targetWidth: Int): Bitmap? {
    val page = runCatching { openPage(index) }.getOrNull() ?: return null
    return try {
        val scale = targetWidth.toFloat() / page.width
        val height = (page.height * scale).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(targetWidth, height, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.WHITE)
        }
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        bitmap
    } finally {
        page.close()
    }
}

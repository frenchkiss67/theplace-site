package com.theplace.receiptscanner.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.theplace.receiptscanner.data.Receipt
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
actual fun PdfThumbnail(receipt: Receipt, modifier: Modifier) {
    val context = LocalContext.current
    var image by remember(receipt.fileName) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(receipt.fileName) {
        image = withContext(Dispatchers.IO) {
            ThumbnailCache(context).loadOrRender(receipt.fileName)?.asImageBitmap()
        }
    }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val bmp = image
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * Cache disque des vignettes dans `cacheDir/thumbs/`. Une vignette par
 * `fileName`. Pas de stratégie d'éviction explicite — Android purge le
 * cacheDir si besoin de stockage.
 */
internal class ThumbnailCache(private val context: Context) {
    private val dir: File =
        File(context.cacheDir, "thumbs").apply { if (!exists()) mkdirs() }

    fun loadOrRender(fileName: String): Bitmap? {
        val cached = File(dir, "$fileName.png")
        if (cached.exists()) {
            runCatching { BitmapFactory.decodeFile(cached.absolutePath) }
                .getOrNull()
                ?.let { return it }
        }
        val pdf = File(File(context.filesDir, "receipts"), fileName)
        if (!pdf.exists()) return null
        return render(pdf)?.also { bmp -> persist(bmp, cached) }
    }

    fun delete(fileName: String) {
        File(dir, "$fileName.png").delete()
    }

    private fun render(pdf: File): Bitmap? = runCatching {
        ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                renderer.renderPageBitmap(index = 0, targetWidth = 400)
            }
        }
    }.getOrNull()

    private fun persist(bitmap: Bitmap, file: File) {
        runCatching {
            file.outputStream().use { os ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, os)
            }
        }
    }
}

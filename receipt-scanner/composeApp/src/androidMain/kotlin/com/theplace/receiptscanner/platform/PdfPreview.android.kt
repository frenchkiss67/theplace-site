package com.theplace.receiptscanner.platform

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.theplace.receiptscanner.data.Receipt
import com.theplace.receiptscanner.resources.Res
import com.theplace.receiptscanner.resources.detail_loading
import com.theplace.receiptscanner.resources.detail_preview_unavailable
import com.theplace.receiptscanner.resources.page_index
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun PdfPreview(receipt: Receipt, modifier: Modifier) {
    val context = LocalContext.current
    val storage = remember(context) { PdfStorage(context) }
    val file = remember(receipt.fileName) { storage.file(receipt.fileName) }

    // Renderer ouvert pour la durée de vie du composable, fermé proprement à la sortie.
    val renderer = remember(file.path) { openRenderer(file) }
    DisposableEffect(renderer) {
        onDispose { runCatching { renderer?.close() } }
    }

    if (renderer == null) {
        Box(
            modifier = modifier.fillMaxWidth().padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(Res.string.detail_preview_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(count = renderer.pageCount) { index ->
            PdfPageItem(renderer, index, totalPages = renderer.pageCount)
        }
    }
}

@Composable
private fun PdfPageItem(renderer: PdfRenderer, index: Int, totalPages: Int) {
    var image by remember(renderer, index) { mutableStateOf<ImageBitmap?>(null) }
    var aspect by remember(renderer, index) { mutableStateOf(0.7f) }

    LaunchedEffect(renderer, index) {
        val (bitmap, ratio) = withContext(Dispatchers.IO) { renderPage(renderer, index) }
        aspect = ratio
        image = bitmap?.asImageBitmap()
    }

    Column {
        Text(
            text = stringResource(Res.string.page_index, index + 1, totalPages),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth().aspectRatio(aspect),
        ) {
            val pageImage = image
            if (pageImage == null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text(
                            text = stringResource(Res.string.detail_loading),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            } else {
                ZoomableImage(image = pageImage)
            }
        }
    }
}

/**
 * Image avec pinch-to-zoom (1×→4×) et pan en mode zoomé. Double-tap
 * pour réinitialiser. Le pan est annulé dès qu'on revient à scale 1.
 */
@Composable
private fun ZoomableImage(image: ImageBitmap) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4f)
        offset = if (scale > 1f) offset + panChange else Offset.Zero
    }
    Image(
        bitmap = image,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offset.x,
                translationY = offset.y,
            )
            .transformable(transformableState)
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    scale = 1f
                    offset = Offset.Zero
                })
            },
    )
}

private fun openRenderer(file: File): PdfRenderer? = runCatching {
    val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    PdfRenderer(pfd)
}.getOrNull()

/** Rend une page en bitmap calibré sur ~2x la largeur logique pour la netteté. */
private fun renderPage(renderer: PdfRenderer, index: Int): Pair<Bitmap?, Float> {
    val bitmap = renderer.renderPageBitmap(index, targetWidth = 1600) ?: return null to 0.7f
    return bitmap to (bitmap.width.toFloat() / bitmap.height.toFloat())
}

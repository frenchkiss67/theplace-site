package com.theplace.receiptscanner.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.theplace.receiptscanner.data.Receipt

/**
 * Affiche page par page le PDF d'un ticket, sous forme d'images empilées
 * verticalement, avec chargement à la demande.
 * Android : `android.graphics.pdf.PdfRenderer` → `ImageBitmap`.
 * iOS : `CGPDFDocument` + Core Graphics (à fournir avec la cible).
 */
@Composable
expect fun PdfPreview(
    receipt: Receipt,
    modifier: Modifier = Modifier,
)

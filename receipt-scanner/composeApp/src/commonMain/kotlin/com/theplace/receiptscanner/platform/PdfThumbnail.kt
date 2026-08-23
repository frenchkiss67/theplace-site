package com.theplace.receiptscanner.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.theplace.receiptscanner.data.Receipt

/**
 * Vignette d'un ticket (1ère page rendue petite, ~280×400 px), cachée en
 * mémoire/disque côté plateforme. Affiche un placeholder le temps du rendu
 * ou si le PDF est introuvable.
 */
@Composable
expect fun PdfThumbnail(
    receipt: Receipt,
    modifier: Modifier = Modifier,
)

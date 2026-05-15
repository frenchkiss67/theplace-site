package com.theplace.receiptscanner.platform

import com.theplace.receiptscanner.data.Receipt

/**
 * Extraction OCR du texte d'un PDF. On-device : aucune télémétrie, aucun
 * réseau. Android : ML Kit Text Recognition v2 sur chaque page rendue.
 */
interface TextRecognizer {
    suspend fun extractText(receipt: Receipt): String?
}

package com.theplace.receiptscanner.util

private val forbidden = charArrayOf('/', '\\', '?', '*', ':', '|', '"', '<', '>')

/**
 * Retire les caractères interdits dans les noms de fichiers (FAT/exFAT,
 * Android SAF). Trim + remplacement par `_`. Utilisé par l'export, le
 * backup et la restauration SAF.
 */
fun sanitizeFileName(raw: String): String =
    raw.trim().map { c -> if (c in forbidden) '_' else c }.joinToString("")

/** Garantit le suffixe `.pdf` (insensible à la casse). */
fun ensurePdfSuffix(name: String): String =
    if (name.endsWith(".pdf", ignoreCase = true)) name else "$name.pdf"

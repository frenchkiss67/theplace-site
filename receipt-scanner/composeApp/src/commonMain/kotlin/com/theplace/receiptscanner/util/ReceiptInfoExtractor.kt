package com.theplace.receiptscanner.util

/**
 * Heuristiques d'extraction sur le texte OCR brut d'un ticket.
 * Volontairement simples — l'utilisateur peut toujours corriger
 * manuellement dans l'écran détail.
 */
object ReceiptInfoExtractor {

    private val totalKeyword = Regex("(?i)\\b(total|montant)\\b")
    private val amountPattern = Regex("(\\d+)[.,](\\d{2})\\s*€?")

    /**
     * Cherche un total dans le texte. Stratégie :
     *  - repère les lignes contenant « total » ou « montant » (insensible
     *    à la casse) ;
     *  - extrait tous les nombres décimaux de la ligne et de la suivante ;
     *  - retient la valeur la plus élevée (le total est rarement le plus
     *    petit montant du ticket).
     *
     *  Retourne `null` si aucun candidat n'est trouvé.
     */
    fun extractTotalCents(text: String?): Long? {
        if (text.isNullOrBlank()) return null
        val lines = text.lines()
        var best: Long? = null
        lines.forEachIndexed { idx, line ->
            if (!totalKeyword.containsMatchIn(line)) return@forEachIndexed
            val window = buildString {
                append(line)
                lines.getOrNull(idx + 1)?.let { append(' ').append(it) }
            }
            amountPattern.findAll(window).forEach { match ->
                val euros = match.groupValues[1].toLongOrNull() ?: return@forEach
                val cents = match.groupValues[2].toLongOrNull() ?: return@forEach
                val total = euros * 100 + cents
                if (best == null || total > best!!) best = total
            }
        }
        return best
    }
}

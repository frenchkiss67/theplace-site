package com.theplace.receiptscanner.util

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * Heuristiques d'extraction sur le texte OCR brut d'un ticket.
 * Volontairement simples — l'utilisateur peut toujours corriger
 * manuellement dans l'écran détail.
 */
object ReceiptInfoExtractor {

    private val totalKeyword = Regex("(?i)\\b(total|montant)\\b")
    private val amountPattern = Regex("(\\d+)[.,](\\d{2})\\s*€?")
    private val datePattern = Regex("(\\d{1,2})[/.-](\\d{1,2})[/.-](\\d{2,4})")

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

    /**
     * Devine le nom du marchand : première ligne non vide « significative »
     * (≥ 3 caractères, contient au moins une lettre, n'est pas un montant
     * ni une date isolée). On filtre aussi les SIRET et numéros de TVA.
     */
    fun extractMerchantName(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val rejected = Regex("^\\s*(\\d+[\\d\\s.,/-]*|SIRET.*|TVA.*|N°.*)\\s*$", RegexOption.IGNORE_CASE)
        return text.lineSequence()
            .map { it.trim() }
            .filter { it.length >= 3 }
            .filter { it.any { c -> c.isLetter() } }
            .filterNot { rejected.matches(it) }
            .firstOrNull()
            ?.take(60)
    }

    /**
     * Cherche une date dans le texte (`dd/MM/yy(yy)`, `dd-MM-yy(yy)`,
     * `dd.MM.yy(yy)`). Retourne l'instant epoch ms (minuit, zone donnée)
     * ou `null` si rien de plausible.
     */
    fun extractPurchasedAtMs(
        text: String?,
        zone: TimeZone = TimeZone.currentSystemDefault(),
    ): Long? {
        if (text.isNullOrBlank()) return null
        for (match in datePattern.findAll(text)) {
            val day = match.groupValues[1].toIntOrNull() ?: continue
            val month = match.groupValues[2].toIntOrNull() ?: continue
            var year = match.groupValues[3].toIntOrNull() ?: continue
            if (year < 100) year += 2000
            if (month !in 1..12 || day !in 1..31) continue
            // Plafond raisonnable pour éviter de gober un « 12/12/9999 » de PDF.
            if (year !in 2000..2099) continue
            return runCatching {
                LocalDateTime(year, month, day, 0, 0)
                    .toInstant(zone)
                    .toEpochMilliseconds()
            }.getOrNull() ?: continue
        }
        return null
    }
}

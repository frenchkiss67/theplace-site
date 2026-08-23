package com.theplace.receiptscanner.util

import com.theplace.receiptscanner.data.Receipt
import com.theplace.receiptscanner.data.ReceiptCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class CsvTest {

    @Test
    fun buildReceiptsCsv_has_header_and_one_line_per_receipt() {
        val csv = buildReceiptsCsv(
            listOf(
                receipt(id = 1, name = "Carrefour", category = ReceiptCategory.Groceries, totalCents = 1230),
                receipt(id = 2, name = "Resto", category = null, totalCents = null),
            )
        )
        val lines = csv.trim().lines()
        assertEquals(3, lines.size)
        assertEquals("id,dateScan,dateAchat,nom,categorie,montant,fichier", lines[0])
        // L'ID 1 doit apparaître avec catégorie "Groceries" et montant "12,30".
        assertTrue(lines[1].startsWith("1,"), "got: ${lines[1]}")
        assertTrue(lines[1].contains("Groceries"))
        assertTrue(lines[1].contains("12,30"))
        // L'ID 2 doit avoir des cellules vides pour catégorie + montant.
        assertTrue(lines[2].contains(",,") || lines[2].endsWith(","), "got: ${lines[2]}")
    }

    @Test
    fun buildReceiptsCsv_escapes_commas_and_quotes() {
        val csv = buildReceiptsCsv(
            listOf(receipt(id = 1, name = "Carrefour, Paris \"centre\"")),
        )
        val dataLine = csv.lines()[1]
        // Le nom doit être entre guillemets, et les guillemets internes doublés.
        assertTrue(dataLine.contains("\"Carrefour, Paris \"\"centre\"\"\""), "got: $dataLine")
    }

    @Test
    fun buildReceiptsCsv_handles_empty_list() {
        val csv = buildReceiptsCsv(emptyList())
        // Juste l'en-tête.
        assertEquals("id,dateScan,dateAchat,nom,categorie,montant,fichier", csv.trim())
    }

    private fun receipt(
        id: Long,
        name: String,
        category: ReceiptCategory? = null,
        totalCents: Long? = null,
        purchasedAt: Long? = null,
    ) = Receipt(
        id = id,
        name = name,
        fileName = "$id.pdf",
        pageCount = 1,
        sizeBytes = 100,
        createdAt = LocalDateTime(2026, 5, 14, 10, 32)
            .toInstant(TimeZone.UTC).toEpochMilliseconds(),
        category = category,
        totalCents = totalCents,
        purchasedAt = purchasedAt,
    )
}

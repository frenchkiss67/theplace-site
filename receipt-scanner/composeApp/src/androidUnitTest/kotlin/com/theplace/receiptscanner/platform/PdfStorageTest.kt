package com.theplace.receiptscanner.platform

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests Robolectric autour de PdfStorage : nommage timestamp, copie depuis
 * une source `file://` vers le filesDir, suppression.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PdfStorageTest {

    private lateinit var context: Context
    private lateinit var storage: PdfStorage
    private lateinit var tmpSource: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        storage = PdfStorage(context)
        tmpSource = File.createTempFile("source", ".pdf").apply { writeBytes(SAMPLE_BYTES) }
    }

    @After
    fun tearDown() {
        tmpSource.delete()
        File(context.filesDir, "receipts").deleteRecursively()
    }

    @Test
    fun newFileName_matches_expected_pattern() {
        val name = storage.newFileName()
        assertTrue(
            Regex("^ticket_\\d{8}_\\d{6}\\.pdf$").matches(name),
            "got: $name",
        )
    }

    @Test
    fun importFrom_copies_bytes_and_reports_size() {
        val target = "imported.pdf"
        val sourceUri: Uri = tmpSource.toUri()

        val sizeBytes = storage.importFrom(sourceUri, target)
        val copied = storage.file(target)

        assertTrue(copied.exists())
        assertEquals(SAMPLE_BYTES.size.toLong(), sizeBytes)
        assertEquals(SAMPLE_BYTES.toList(), copied.readBytes().toList())
    }

    @Test
    fun delete_removes_existing_file() {
        val name = "to_remove.pdf"
        storage.importFrom(tmpSource.toUri(), name)
        assertTrue(storage.file(name).exists())

        val deleted = storage.delete(name)
        assertTrue(deleted)
        assertFalse(storage.file(name).exists())
    }

    @Test
    fun delete_returns_false_for_missing_file() {
        assertFalse(storage.delete("never_existed.pdf"))
    }

    companion object {
        private val SAMPLE_BYTES = "%PDF-1.4 sample".toByteArray()
    }
}

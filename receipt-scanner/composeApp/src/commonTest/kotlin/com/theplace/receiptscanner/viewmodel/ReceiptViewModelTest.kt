package com.theplace.receiptscanner.viewmodel

import com.theplace.receiptscanner.data.Receipt
import com.theplace.receiptscanner.data.ReceiptRepository
import com.theplace.receiptscanner.platform.PdfActions
import com.theplace.receiptscanner.platform.PlatformScanResult
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class ReceiptViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun receipts_flow_reflects_repository_emissions() = runTest(dispatcher) {
        val repo = FakeRepository()
        val vm = ReceiptViewModel(repo, FakePdfActions())

        repo.emit(listOf(receipt(id = 1, name = "A")))
        advanceUntilIdle()

        assertEquals(listOf("A"), vm.receipts.first().map { it.name })
    }

    @Test
    fun rename_trims_and_delegates_to_repository() = runTest(dispatcher) {
        val repo = FakeRepository()
        val vm = ReceiptViewModel(repo, FakePdfActions())
        val r = receipt(name = "Old")

        vm.rename(r, "   Carrefour 02 mai  ")
        advanceUntilIdle()

        assertEquals(listOf(r to "Carrefour 02 mai"), repo.renamed)
    }

    @Test
    fun rename_is_noop_when_name_unchanged() = runTest(dispatcher) {
        val repo = FakeRepository()
        val vm = ReceiptViewModel(repo, FakePdfActions())
        val r = receipt(name = "Same")

        vm.rename(r, "Same")
        vm.rename(r, "  Same  ")
        advanceUntilIdle()

        assertTrue(repo.renamed.isEmpty())
    }

    @Test
    fun rename_is_noop_when_blank() = runTest(dispatcher) {
        val repo = FakeRepository()
        val vm = ReceiptViewModel(repo, FakePdfActions())

        vm.rename(receipt(), "   ")
        advanceUntilIdle()

        assertTrue(repo.renamed.isEmpty())
    }

    @Test
    fun delete_delegates_to_repository() = runTest(dispatcher) {
        val repo = FakeRepository()
        val vm = ReceiptViewModel(repo, FakePdfActions())
        val r = receipt(id = 7)

        vm.delete(r)
        advanceUntilIdle()

        assertEquals(listOf(r), repo.deleted)
    }

    @Test
    fun openPdf_and_sharePdf_route_to_PdfActions() {
        val actions = FakePdfActions()
        val vm = ReceiptViewModel(FakeRepository(), actions)
        val r = receipt(name = "Ticket carrefour")

        vm.openPdf(r)
        vm.sharePdf(r)

        assertEquals(listOf(r), actions.opened)
        assertEquals(listOf(r to "Ticket carrefour"), actions.shared)
    }

    private fun receipt(
        id: Long = 1,
        name: String = "Ticket",
        fileName: String = "ticket_1.pdf",
    ): Receipt = Receipt(
        id = id,
        name = name,
        fileName = fileName,
        pageCount = 1,
        sizeBytes = 1024,
        createdAt = 0L,
    )
}

private class FakeRepository : ReceiptRepository {
    private val source = MutableStateFlow<List<Receipt>>(emptyList())
    val renamed = mutableListOf<Pair<Receipt, String>>()
    val deleted = mutableListOf<Receipt>()

    fun emit(list: List<Receipt>) {
        source.update { list }
    }

    override fun observeAll(): Flow<List<Receipt>> = source

    override suspend fun findById(id: Long): Receipt? =
        source.value.firstOrNull { it.id == id }

    override suspend fun addFromScan(scan: PlatformScanResult): Receipt {
        error("Pas couvert par ce test (PlatformScanResult dépend de la plateforme).")
    }

    override suspend fun rename(receipt: Receipt, newName: String) {
        renamed += receipt to newName
    }

    override suspend fun delete(receipt: Receipt) {
        deleted += receipt
    }
}

private class FakePdfActions : PdfActions {
    val opened = mutableListOf<Receipt>()
    val shared = mutableListOf<Pair<Receipt, String>>()

    override fun open(receipt: Receipt) {
        opened += receipt
    }

    override fun share(receipt: Receipt, displayName: String) {
        shared += receipt to displayName
    }
}

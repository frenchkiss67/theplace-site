package com.theplace.receiptscanner.viewmodel

import com.theplace.receiptscanner.data.Receipt
import com.theplace.receiptscanner.data.ReceiptCategory
import com.theplace.receiptscanner.data.ReceiptRepository
import com.theplace.receiptscanner.data.RestoreOutcome
import com.theplace.receiptscanner.platform.ExportOutcome
import com.theplace.receiptscanner.platform.PdfActions
import com.theplace.receiptscanner.platform.PlatformExportTarget
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

    // -- Multi-sélection -----------------------------------------------------

    @Test
    fun toggleSelection_adds_then_removes_id() = runTest(dispatcher) {
        val vm = ReceiptViewModel(FakeRepository(), FakePdfActions())

        vm.toggleSelection(1)
        vm.toggleSelection(2)
        assertEquals(setOf(1L, 2L), vm.selection.first())

        vm.toggleSelection(1)
        assertEquals(setOf(2L), vm.selection.first())
    }

    @Test
    fun selectAll_replaces_selection_and_clearSelection_empties_it() = runTest(dispatcher) {
        val vm = ReceiptViewModel(FakeRepository(), FakePdfActions())

        vm.selectAll(listOf(1L, 2L, 3L))
        assertEquals(setOf(1L, 2L, 3L), vm.selection.first())

        vm.clearSelection()
        assertTrue(vm.selection.first().isEmpty())
    }

    @Test
    fun shareSelected_passes_only_selected_receipts_to_PdfActions() = runTest(dispatcher) {
        val repo = FakeRepository()
        val actions = FakePdfActions()
        val vm = ReceiptViewModel(repo, actions)
        val all = listOf(receipt(id = 1, name = "A"), receipt(id = 2, name = "B"), receipt(id = 3, name = "C"))
        repo.emit(all)
        advanceUntilIdle()

        vm.toggleSelection(1)
        vm.toggleSelection(3)
        vm.shareSelected("Tickets")

        assertEquals(1, actions.sharedMultiple.size)
        val (sharedReceipts, label) = actions.sharedMultiple.single()
        assertEquals(listOf(1L, 3L), sharedReceipts.map { it.id })
        assertEquals("Tickets", label)
    }

    @Test
    fun shareSelected_is_noop_when_selection_empty() = runTest(dispatcher) {
        val actions = FakePdfActions()
        val vm = ReceiptViewModel(FakeRepository(), actions)

        vm.shareSelected("Tickets")

        assertTrue(actions.sharedMultiple.isEmpty())
    }

    @Test
    fun deleteSelected_removes_each_and_clears_selection() = runTest(dispatcher) {
        val repo = FakeRepository()
        val vm = ReceiptViewModel(repo, FakePdfActions())
        val all = listOf(receipt(id = 1), receipt(id = 2), receipt(id = 3))
        repo.emit(all)
        advanceUntilIdle()

        vm.toggleSelection(1)
        vm.toggleSelection(2)

        var reported = -1
        vm.deleteSelected { reported = it }
        advanceUntilIdle()

        assertEquals(2, reported)
        assertEquals(listOf(1L, 2L), repo.deleted.map { it.id })
        assertTrue(vm.selection.first().isEmpty())
    }

    @Test
    fun setCategory_and_setAmount_update_via_repository() = runTest(dispatcher) {
        val repo = FakeRepository()
        val vm = ReceiptViewModel(repo, FakePdfActions())
        val r = receipt(id = 1, name = "Carrefour")

        vm.setCategory(r, ReceiptCategory.Groceries)
        vm.setAmount(r, 1230)
        advanceUntilIdle()

        assertEquals(2, repo.updated.size)
        assertEquals(ReceiptCategory.Groceries, repo.updated[0].category)
        assertEquals(1230L, repo.updated[1].totalCents)
    }

    @Test
    fun deleteSelected_calls_onDone_with_zero_when_empty() = runTest(dispatcher) {
        val vm = ReceiptViewModel(FakeRepository(), FakePdfActions())

        var reported = -1
        vm.deleteSelected { reported = it }

        assertEquals(0, reported)
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

    val updated = mutableListOf<Receipt>()
    override suspend fun update(receipt: Receipt) {
        updated += receipt
        source.update { current -> current.map { if (it.id == receipt.id) receipt else it } }
    }

    override suspend fun delete(receipt: Receipt) {
        deleted += receipt
        source.update { current -> current.filterNot { it.id == receipt.id } }
    }

    var restoreResult: RestoreOutcome = RestoreOutcome.Success(imported = 0, skipped = 0)
    val restoreCalls = mutableListOf<PlatformExportTarget>()
    override suspend fun restoreFromFolder(target: PlatformExportTarget): RestoreOutcome {
        restoreCalls += target
        return restoreResult
    }
}

private class FakePdfActions : PdfActions {
    val opened = mutableListOf<Receipt>()
    val shared = mutableListOf<Pair<Receipt, String>>()
    val sharedMultiple = mutableListOf<Pair<List<Receipt>, String>>()
    val exported = mutableListOf<Pair<List<Receipt>, PlatformExportTarget>>()
    var exportResult: ExportOutcome = ExportOutcome.Success(0)

    override fun open(receipt: Receipt) {
        opened += receipt
    }

    override fun share(receipt: Receipt, displayName: String) {
        shared += receipt to displayName
    }

    override fun shareMultiple(receipts: List<Receipt>, displayLabel: String) {
        sharedMultiple += receipts to displayLabel
    }

    override suspend fun exportTo(
        receipts: List<Receipt>,
        target: PlatformExportTarget,
    ): ExportOutcome {
        exported += receipts to target
        return exportResult
    }
}

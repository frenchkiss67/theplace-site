package com.theplace.receiptscanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theplace.receiptscanner.data.Receipt
import com.theplace.receiptscanner.data.ReceiptRepository
import com.theplace.receiptscanner.platform.ExportOutcome
import com.theplace.receiptscanner.platform.PdfActions
import com.theplace.receiptscanner.platform.PlatformExportTarget
import com.theplace.receiptscanner.platform.PlatformScanResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReceiptViewModel(
    private val repository: ReceiptRepository,
    private val pdfActions: PdfActions,
) : ViewModel() {

    // Eagerly : la VM est liée à l'Activity, on garde toujours `value` à jour
    // pour servir les actions groupées (deleteSelected, exportSelected…)
    // sans dépendre d'un abonnement UI actif.
    val receipts: StateFlow<List<Receipt>> = repository.observeAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList(),
    )

    private val _selection = MutableStateFlow<Set<Long>>(emptySet())
    /** Identifiants des tickets sélectionnés en mode multi-sélection. */
    val selection: StateFlow<Set<Long>> = _selection.asStateFlow()

    fun saveScan(result: PlatformScanResult, onSaved: (Receipt) -> Unit) {
        viewModelScope.launch {
            val saved = repository.addFromScan(result)
            onSaved(saved)
        }
    }

    fun rename(receipt: Receipt, newName: String) {
        viewModelScope.launch {
            val trimmed = newName.trim()
            if (trimmed.isNotEmpty() && trimmed != receipt.name) {
                repository.rename(receipt, trimmed)
            }
        }
    }

    fun delete(receipt: Receipt) {
        viewModelScope.launch { repository.delete(receipt) }
    }

    fun openPdf(receipt: Receipt) = pdfActions.open(receipt)

    fun sharePdf(receipt: Receipt) = pdfActions.share(receipt, receipt.name)

    // -- Multi-sélection ------------------------------------------------

    fun toggleSelection(id: Long) {
        _selection.update { current ->
            if (id in current) current - id else current + id
        }
    }

    fun selectAll(ids: Collection<Long>) {
        _selection.value = ids.toSet()
    }

    fun clearSelection() {
        _selection.value = emptySet()
    }

    /** Lookup interne — ne dépend pas du repo car les receipts sont déjà en mémoire. */
    private fun selectedReceipts(): List<Receipt> {
        val ids = _selection.value
        return receipts.value.filter { it.id in ids }
    }

    fun shareSelected(label: String) {
        val items = selectedReceipts()
        if (items.isEmpty()) return
        pdfActions.shareMultiple(items, label)
    }

    /** Supprime les tickets sélectionnés ; appelle `onDone` avec le nombre traité. */
    fun deleteSelected(onDone: (Int) -> Unit) {
        val items = selectedReceipts()
        if (items.isEmpty()) {
            onDone(0)
            return
        }
        viewModelScope.launch {
            items.forEach { repository.delete(it) }
            _selection.value = emptySet()
            onDone(items.size)
        }
    }

    /** Exporte les tickets sélectionnés vers `target` ; remonte le résultat via `onDone`. */
    fun exportSelected(target: PlatformExportTarget, onDone: (ExportOutcome) -> Unit) {
        val items = selectedReceipts()
        if (items.isEmpty()) {
            onDone(ExportOutcome.Success(0))
            return
        }
        viewModelScope.launch {
            val result = pdfActions.exportTo(items, target)
            if (result is ExportOutcome.Success) {
                _selection.value = emptySet()
            }
            onDone(result)
        }
    }
}

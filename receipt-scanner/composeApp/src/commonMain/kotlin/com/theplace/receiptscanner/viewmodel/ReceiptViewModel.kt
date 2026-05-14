package com.theplace.receiptscanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theplace.receiptscanner.data.Receipt
import com.theplace.receiptscanner.data.ReceiptRepository
import com.theplace.receiptscanner.platform.PdfActions
import com.theplace.receiptscanner.platform.PlatformScanResult
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReceiptViewModel(
    private val repository: ReceiptRepository,
    private val pdfActions: PdfActions,
) : ViewModel() {

    val receipts: StateFlow<List<Receipt>> = repository.observeAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

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
}

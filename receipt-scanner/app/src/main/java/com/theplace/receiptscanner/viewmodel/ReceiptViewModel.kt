package com.theplace.receiptscanner.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.theplace.receiptscanner.data.Receipt
import com.theplace.receiptscanner.data.ReceiptRepository
import com.theplace.receiptscanner.util.PdfStorage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReceiptViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ReceiptRepository.from(app)
    private val storage = PdfStorage(app)

    val receipts: StateFlow<List<Receipt>> = repository.observeAll().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun saveScan(result: GmsDocumentScanningResult.Pdf, onSaved: (Receipt) -> Unit) {
        viewModelScope.launch {
            val fileName = storage.newFileName()
            val sizeBytes = storage.importFrom(result.uri, fileName)
            val now = System.currentTimeMillis()
            val receipt = Receipt(
                name = defaultNameForNow(now),
                fileName = fileName,
                pageCount = result.pageCount,
                sizeBytes = sizeBytes,
                createdAt = now,
            )
            val id = repository.add(receipt)
            onSaved(receipt.copy(id = id))
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

    private fun defaultNameForNow(timestamp: Long): String {
        val date = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(timestamp))
        return "Ticket du $date"
    }
}

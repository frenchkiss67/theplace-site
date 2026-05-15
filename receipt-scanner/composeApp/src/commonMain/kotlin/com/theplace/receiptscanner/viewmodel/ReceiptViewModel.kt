package com.theplace.receiptscanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.theplace.receiptscanner.data.Receipt
import com.theplace.receiptscanner.data.ReceiptCategory
import com.theplace.receiptscanner.data.ReceiptRepository
import com.theplace.receiptscanner.data.RestoreOutcome
import com.theplace.receiptscanner.platform.ExportOutcome
import com.theplace.receiptscanner.platform.PdfActions
import com.theplace.receiptscanner.platform.PlatformExportTarget
import com.theplace.receiptscanner.platform.PlatformScanResult
import com.theplace.receiptscanner.platform.TextRecognizer
import com.theplace.receiptscanner.util.ReceiptInfoExtractor
import com.theplace.receiptscanner.util.defaultReceiptName
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
    private val textRecognizer: TextRecognizer? = null,
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

    /** Issue d'un archivage : succès avec le ticket inséré, ou échec (disque plein, fichier corrompu…). */
    sealed interface SaveScanOutcome {
        data class Success(val receipt: Receipt) : SaveScanOutcome
        data class Failure(val message: String) : SaveScanOutcome
    }

    fun saveScan(result: PlatformScanResult, onSaved: (SaveScanOutcome) -> Unit) {
        viewModelScope.launch {
            val outcome: SaveScanOutcome = try {
                SaveScanOutcome.Success(repository.addFromScan(result))
            } catch (t: Throwable) {
                SaveScanOutcome.Failure(t.message ?: "Erreur inconnue")
            }
            onSaved(outcome)
            if (outcome is SaveScanOutcome.Success) runOcrInBackground(outcome.receipt)
        }
    }

    /**
     * Lance l'OCR de manière non bloquante après l'archivage. Met à jour
     * le ticket avec le texte extrait et pré-remplit, quand l'utilisateur
     * n'a pas déjà saisi :
     *   - le nom (si encore au libellé par défaut « Ticket du … »),
     *     remplacé par le marchand détecté ;
     *   - le total (centimes) ;
     *   - la date d'achat.
     */
    private fun runOcrInBackground(receipt: Receipt) {
        val recognizer = textRecognizer ?: return
        viewModelScope.launch {
            val text = runCatching { recognizer.extractText(receipt) }.getOrNull() ?: return@launch
            val current = repository.findById(receipt.id) ?: return@launch

            val merchant = ReceiptInfoExtractor.extractMerchantName(text)
            val proposedName = if (current.name == defaultReceiptName(current.createdAt)) {
                merchant ?: current.name
            } else current.name
            val proposedTotal = current.totalCents ?: ReceiptInfoExtractor.extractTotalCents(text)
            val proposedPurchasedAt =
                current.purchasedAt ?: ReceiptInfoExtractor.extractPurchasedAtMs(text)
            val proposedCategory = current.category
                ?: ReceiptInfoExtractor.categoryForMerchant(merchant ?: current.name)

            repository.update(
                current.copy(
                    name = proposedName,
                    extractedText = text,
                    totalCents = proposedTotal,
                    purchasedAt = proposedPurchasedAt,
                    category = proposedCategory,
                )
            )
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

    fun setCategory(receipt: Receipt, category: ReceiptCategory?) {
        viewModelScope.launch {
            repository.update(receipt.copy(category = category))
        }
    }

    fun setAmount(receipt: Receipt, totalCents: Long?) {
        viewModelScope.launch {
            repository.update(receipt.copy(totalCents = totalCents))
        }
    }

    fun setPurchasedAt(receipt: Receipt, purchasedAt: Long?) {
        viewModelScope.launch {
            repository.update(receipt.copy(purchasedAt = purchasedAt))
        }
    }

    fun setWarrantyMonths(receipt: Receipt, months: Int?) {
        viewModelScope.launch {
            repository.update(receipt.copy(warrantyMonths = months))
        }
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

    /** Réimporte les PDFs d'un dossier SAF. Idempotent (skip si déjà présent). */
    fun restoreFromFolder(target: PlatformExportTarget, onDone: (RestoreOutcome) -> Unit) {
        viewModelScope.launch {
            onDone(repository.restoreFromFolder(target))
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

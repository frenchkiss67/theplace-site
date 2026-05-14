package com.theplace.receiptscanner

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.theplace.receiptscanner.platform.ScanOutcome
import com.theplace.receiptscanner.platform.rememberDocumentScannerLauncher
import com.theplace.receiptscanner.ui.ReceiptListScreen
import com.theplace.receiptscanner.ui.theme.ReceiptScannerTheme
import com.theplace.receiptscanner.viewmodel.ReceiptViewModel

/**
 * Point d'entrée Compose partagé. Reçoit le ViewModel câblé côté plateforme
 * (sa construction nécessite un Repository et des PdfActions concrets), et
 * un éventuel ColorScheme dynamique (Android 12+) injecté par la plateforme.
 */
@Composable
fun App(
    viewModel: ReceiptViewModel,
    dynamicColorScheme: ColorScheme? = null,
    onScanFailure: (String) -> Unit = {},
    onScanCancelled: () -> Unit = {},
) {
    ReceiptScannerTheme(dynamicColors = dynamicColorScheme) {
        val receipts by viewModel.receipts.collectAsState()

        val scanner = rememberDocumentScannerLauncher { outcome ->
            when (outcome) {
                is ScanOutcome.Success -> viewModel.saveScan(outcome.result) { /* feedback géré par hôte */ }
                is ScanOutcome.Cancelled -> onScanCancelled()
                is ScanOutcome.Failure -> onScanFailure(outcome.message)
            }
        }

        ReceiptListScreen(
            receipts = receipts,
            onScanClicked = { scanner.launch() },
            onRename = viewModel::rename,
            onDelete = viewModel::delete,
            onOpen = viewModel::openPdf,
            onShare = viewModel::sharePdf,
        )
    }
}

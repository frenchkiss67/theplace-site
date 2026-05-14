package com.theplace.receiptscanner

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.theplace.receiptscanner.scanner.ScanOutcome
import com.theplace.receiptscanner.scanner.rememberDocumentScannerLauncher
import com.theplace.receiptscanner.ui.ReceiptListScreen
import com.theplace.receiptscanner.ui.theme.ReceiptScannerTheme
import com.theplace.receiptscanner.viewmodel.ReceiptViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ReceiptViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ReceiptScannerTheme {
                val receipts by viewModel.receipts.collectAsState()

                val scanner = rememberDocumentScannerLauncher { outcome ->
                    when (outcome) {
                        is ScanOutcome.Success -> {
                            viewModel.saveScan(outcome.pdfResult) { saved ->
                                Toast.makeText(this, "« ${saved.name} » archivé", Toast.LENGTH_SHORT).show()
                            }
                        }
                        is ScanOutcome.Cancelled -> {
                            Toast.makeText(this, R.string.scan_cancelled, Toast.LENGTH_SHORT).show()
                        }
                        is ScanOutcome.Failure -> {
                            Toast.makeText(
                                this,
                                getString(R.string.scan_error, outcome.message),
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    }
                }

                ReceiptListScreen(
                    receipts = receipts,
                    onScanClicked = { scanner.launch() },
                    onRename = viewModel::rename,
                    onDelete = viewModel::delete,
                )
            }
        }
    }
}

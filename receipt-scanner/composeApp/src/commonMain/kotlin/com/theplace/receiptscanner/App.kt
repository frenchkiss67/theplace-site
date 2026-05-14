package com.theplace.receiptscanner

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.theplace.receiptscanner.platform.ScanOutcome
import com.theplace.receiptscanner.platform.rememberDocumentScannerLauncher
import com.theplace.receiptscanner.resources.Res
import com.theplace.receiptscanner.resources.receipt_deleted
import com.theplace.receiptscanner.resources.receipt_saved
import com.theplace.receiptscanner.resources.scan_cancelled
import com.theplace.receiptscanner.resources.scan_error
import com.theplace.receiptscanner.ui.ReceiptDetailScreen
import com.theplace.receiptscanner.ui.ReceiptListScreen
import com.theplace.receiptscanner.ui.theme.ReceiptScannerTheme
import com.theplace.receiptscanner.viewmodel.ReceiptViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

private const val ROUTE_LIST = "list"
private const val ROUTE_DETAIL = "detail/{id}"
private const val ARG_ID = "id"

@Composable
fun App(
    viewModel: ReceiptViewModel,
    dynamicColorScheme: ColorScheme? = null,
) {
    ReceiptScannerTheme(dynamicColors = dynamicColorScheme) {
        val navController = rememberNavController()
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        val receipts by viewModel.receipts.collectAsState()

        val scanner = rememberDocumentScannerLauncher { outcome ->
            when (outcome) {
                is ScanOutcome.Success -> viewModel.saveScan(outcome.result) { saved ->
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            getString(Res.string.receipt_saved, saved.name)
                        )
                    }
                }
                is ScanOutcome.Cancelled -> scope.launch {
                    snackbarHostState.showSnackbar(getString(Res.string.scan_cancelled))
                }
                is ScanOutcome.Failure -> scope.launch {
                    snackbarHostState.showSnackbar(
                        getString(Res.string.scan_error, outcome.message)
                    )
                }
            }
        }

        NavHost(
            navController = navController,
            startDestination = ROUTE_LIST,
        ) {
            composable(ROUTE_LIST) {
                ReceiptListScreen(
                    receipts = receipts,
                    snackbarHostState = snackbarHostState,
                    onScanClicked = { scanner.launch() },
                    onItemClick = { receipt ->
                        navController.navigate("detail/${receipt.id}")
                    },
                    onRename = viewModel::rename,
                    onDelete = { receipt ->
                        viewModel.delete(receipt)
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                getString(Res.string.receipt_deleted, receipt.name)
                            )
                        }
                    },
                    onOpen = viewModel::openPdf,
                    onShare = viewModel::sharePdf,
                )
            }
            composable(
                route = ROUTE_DETAIL,
                arguments = listOf(navArgument(ARG_ID) { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong(ARG_ID) ?: -1L
                val receipt = receipts.firstOrNull { it.id == id }
                if (receipt == null) {
                    // Élément supprimé entre-temps ; on revient à la liste.
                    navController.popBackStack(ROUTE_LIST, inclusive = false)
                } else {
                    ReceiptDetailScreen(
                        receipt = receipt,
                        snackbarHostState = snackbarHostState,
                        onBack = { navController.popBackStack() },
                        onRename = viewModel::rename,
                        onDelete = { r ->
                            viewModel.delete(r)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    getString(Res.string.receipt_deleted, r.name)
                                )
                            }
                            navController.popBackStack()
                        },
                        onOpen = viewModel::openPdf,
                        onShare = viewModel::sharePdf,
                    )
                }
            }
        }
    }
}

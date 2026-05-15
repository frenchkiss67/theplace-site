package com.theplace.receiptscanner

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.theplace.receiptscanner.platform.AppLockSettings
import com.theplace.receiptscanner.platform.BiometricGate
import com.theplace.receiptscanner.platform.ExportOutcome
import com.theplace.receiptscanner.platform.ScanOutcome
import com.theplace.receiptscanner.platform.rememberDocumentScannerLauncher
import com.theplace.receiptscanner.platform.rememberExportFolderLauncher
import com.theplace.receiptscanner.resources.Res
import com.theplace.receiptscanner.resources.export_done
import com.theplace.receiptscanner.resources.export_failed
import com.theplace.receiptscanner.resources.export_partial
import com.theplace.receiptscanner.resources.receipt_deleted
import com.theplace.receiptscanner.resources.receipt_saved
import com.theplace.receiptscanner.resources.receipts_deleted_many
import com.theplace.receiptscanner.resources.scan_cancelled
import com.theplace.receiptscanner.resources.scan_error
import com.theplace.receiptscanner.resources.selection_share_label
import com.theplace.receiptscanner.ui.ReceiptDetailScreen
import com.theplace.receiptscanner.ui.ReceiptListScreen
import com.theplace.receiptscanner.ui.theme.ReceiptScannerTheme
import com.theplace.receiptscanner.viewmodel.ReceiptViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

private const val ROUTE_LIST = "list"
private const val ROUTE_DETAIL = "detail/{id}"
private const val ARG_ID = "id"

// Durées de transition Material Expressive : ~250 ms slide, ~150 ms fade.
private const val NAV_SLIDE_MS = 250
private const val NAV_FADE_MS = 150

@Composable
fun App(
    viewModel: ReceiptViewModel,
    appLock: AppLockSettings,
    dynamicColorScheme: ColorScheme? = null,
) {
    ReceiptScannerTheme(dynamicColors = dynamicColorScheme) {
        BiometricGate(settings = appLock) {
            AppContent(viewModel = viewModel, appLock = appLock)
        }
    }
}

@Composable
private fun AppContent(
    viewModel: ReceiptViewModel,
    appLock: AppLockSettings,
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val receipts by viewModel.receipts.collectAsState()
    val selection by viewModel.selection.collectAsState()
    val shareLabel = stringResource(Res.string.selection_share_label)
    val lockEnabled by appLock.enabled.collectAsState()

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

        // SAF folder picker pour l'export multi-tickets.
        val exportLauncher = rememberExportFolderLauncher { target ->
            if (target == null) return@rememberExportFolderLauncher
            viewModel.exportSelected(target) { result ->
                scope.launch {
                    val message = when (result) {
                        is ExportOutcome.Success -> getString(Res.string.export_done, result.exportedCount)
                        is ExportOutcome.Failure -> if (result.partialCount > 0) {
                            getString(Res.string.export_partial, result.partialCount, result.message)
                        } else {
                            getString(Res.string.export_failed, result.message)
                        }
                    }
                    snackbarHostState.showSnackbar(message)
                }
            }
        }

        NavHost(
            navController = navController,
            startDestination = ROUTE_LIST,
        ) {
            // Liste : reste en place quand on pousse le détail (fade simple),
            // glisse vers la droite quand on revient depuis le détail.
            composable(
                route = ROUTE_LIST,
                enterTransition = { fadeIn(animationSpec = tween(NAV_FADE_MS)) },
                exitTransition = { fadeOut(animationSpec = tween(NAV_FADE_MS)) },
                popEnterTransition = {
                    slideIntoContainer(SlideDirection.End, tween(NAV_SLIDE_MS)) +
                        fadeIn(animationSpec = tween(NAV_FADE_MS))
                },
                popExitTransition = {
                    slideOutOfContainer(SlideDirection.End, tween(NAV_SLIDE_MS)) +
                        fadeOut(animationSpec = tween(NAV_FADE_MS))
                },
            ) {
                ReceiptListScreen(
                    receipts = receipts,
                    selection = selection,
                    lockEnabled = lockEnabled,
                    lockAvailable = appLock.isBiometricAvailable,
                    onToggleLock = appLock::setEnabled,
                    snackbarHostState = snackbarHostState,
                    onScanClicked = { scanner.launch() },
                    onItemClick = { receipt ->
                        navController.navigate("detail/${receipt.id}")
                    },
                    onToggleSelection = { viewModel.toggleSelection(it.id) },
                    onClearSelection = viewModel::clearSelection,
                    onSelectAll = { filtered -> viewModel.selectAll(filtered.map { it.id }) },
                    onShareSelected = { viewModel.shareSelected(shareLabel) },
                    onExportSelected = { exportLauncher.launch() },
                    onDeleteSelected = {
                        viewModel.deleteSelected { count ->
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    getString(Res.string.receipts_deleted_many, count)
                                )
                            }
                        }
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
            // Détail : entre par la droite, sort vers la droite au retour.
            composable(
                route = ROUTE_DETAIL,
                arguments = listOf(navArgument(ARG_ID) { type = NavType.LongType }),
                enterTransition = {
                    slideIntoContainer(SlideDirection.Start, tween(NAV_SLIDE_MS)) +
                        fadeIn(animationSpec = tween(NAV_FADE_MS))
                },
                exitTransition = {
                    slideOutOfContainer(SlideDirection.Start, tween(NAV_SLIDE_MS)) +
                        fadeOut(animationSpec = tween(NAV_FADE_MS))
                },
                popEnterTransition = {
                    slideIntoContainer(SlideDirection.Start, tween(NAV_SLIDE_MS)) +
                        fadeIn(animationSpec = tween(NAV_FADE_MS))
                },
                popExitTransition = {
                    slideOutOfContainer(SlideDirection.End, tween(NAV_SLIDE_MS)) +
                        fadeOut(animationSpec = tween(NAV_FADE_MS))
                },
            ) { entry ->
                val id = entry.arguments?.getLong(ARG_ID) ?: -1L
                val receipt = receipts.firstOrNull { it.id == id }
                if (receipt == null) {
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

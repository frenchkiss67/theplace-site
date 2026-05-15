package com.theplace.receiptscanner.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.theplace.receiptscanner.data.Receipt
import com.theplace.receiptscanner.platform.PdfThumbnail
import com.theplace.receiptscanner.platform.PlatformBackHandler
import com.theplace.receiptscanner.resources.Res
import com.theplace.receiptscanner.resources.action_delete
import com.theplace.receiptscanner.resources.action_export
import com.theplace.receiptscanner.resources.action_open
import com.theplace.receiptscanner.resources.action_rename
import com.theplace.receiptscanner.resources.action_scan
import com.theplace.receiptscanner.resources.action_share
import com.theplace.receiptscanner.resources.app_name
import com.theplace.receiptscanner.resources.dialog_cancel
import com.theplace.receiptscanner.resources.dialog_confirm
import com.theplace.receiptscanner.resources.dialog_delete_many_message
import com.theplace.receiptscanner.resources.dialog_delete_many_title
import com.theplace.receiptscanner.resources.dialog_delete_message
import com.theplace.receiptscanner.resources.dialog_delete_title
import com.theplace.receiptscanner.resources.dialog_rename_hint
import com.theplace.receiptscanner.resources.dialog_rename_title
import com.theplace.receiptscanner.resources.empty_cta
import com.theplace.receiptscanner.resources.empty_subtitle
import com.theplace.receiptscanner.resources.empty_title
import com.theplace.receiptscanner.resources.pages_label
import com.theplace.receiptscanner.resources.receipts_count
import com.theplace.receiptscanner.resources.search_clear
import com.theplace.receiptscanner.resources.search_no_results
import com.theplace.receiptscanner.resources.search_placeholder
import com.theplace.receiptscanner.resources.selection_all
import com.theplace.receiptscanner.resources.settings_lock_description
import com.theplace.receiptscanner.resources.settings_lock_label
import com.theplace.receiptscanner.resources.settings_lock_unavailable
import com.theplace.receiptscanner.resources.settings_title
import com.theplace.receiptscanner.resources.selection_clear
import com.theplace.receiptscanner.resources.selection_count
import com.theplace.receiptscanner.resources.sort_date_asc
import com.theplace.receiptscanner.resources.sort_date_desc
import com.theplace.receiptscanner.resources.sort_label
import com.theplace.receiptscanner.resources.sort_name_asc
import com.theplace.receiptscanner.resources.sort_size_desc
import com.theplace.receiptscanner.util.formatDate
import com.theplace.receiptscanner.util.formatSize
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptListScreen(
    receipts: List<Receipt>,
    selection: Set<Long>,
    lockEnabled: Boolean,
    lockAvailable: Boolean,
    onToggleLock: (Boolean) -> Unit,
    snackbarHostState: SnackbarHostState,
    onScanClicked: () -> Unit,
    onItemClick: (Receipt) -> Unit,
    onToggleSelection: (Receipt) -> Unit,
    onClearSelection: () -> Unit,
    onSelectAll: (List<Receipt>) -> Unit,
    onShareSelected: () -> Unit,
    onExportSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    onRename: (Receipt, String) -> Unit,
    onDelete: (Receipt) -> Unit,
    onOpen: (Receipt) -> Unit,
    onShare: (Receipt) -> Unit,
) {
    var renameTarget by remember { mutableStateOf<Receipt?>(null) }
    var deleteTarget by remember { mutableStateOf<Receipt?>(null) }
    var deleteManyOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }

    var query by rememberSaveable { mutableStateOf("") }
    var sortOption by rememberSaveable { mutableStateOf(SortOption.DateDesc) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    var overflowOpen by remember { mutableStateOf(false) }

    val filteredReceipts = remember(receipts, query, sortOption) {
        receipts.filteredByQuery(query).sortedBy(sortOption)
    }
    val totalSize = remember(filteredReceipts) { filteredReceipts.sumOf { it.sizeBytes } }
    val inSelectionMode = selection.isNotEmpty()

    PlatformBackHandler(enabled = inSelectionMode, onBack = onClearSelection)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (inSelectionMode) {
                SelectionTopBar(
                    count = selection.size,
                    onClear = onClearSelection,
                    onSelectAll = { onSelectAll(filteredReceipts) },
                    onShare = onShareSelected,
                    onExport = onExportSelected,
                    onDelete = { deleteManyOpen = true },
                )
            } else {
                TopAppBar(
                    title = { Text(stringResource(Res.string.app_name)) },
                    actions = {
                        IconButton(
                            onClick = { sortMenuOpen = true },
                            enabled = receipts.isNotEmpty(),
                        ) {
                            Icon(Icons.Default.Sort, contentDescription = stringResource(Res.string.sort_label))
                        }
                        SortMenu(
                            expanded = sortMenuOpen,
                            current = sortOption,
                            onDismiss = { sortMenuOpen = false },
                            onSelect = {
                                sortOption = it
                                sortMenuOpen = false
                            },
                        )
                        IconButton(onClick = { overflowOpen = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = stringResource(Res.string.settings_title),
                            )
                        }
                        DropdownMenu(
                            expanded = overflowOpen,
                            onDismissRequest = { overflowOpen = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.settings_title)) },
                                onClick = {
                                    overflowOpen = false
                                    settingsOpen = true
                                },
                            )
                        }
                    },
                )
            }
        },
        floatingActionButton = {
            if (!inSelectionMode) {
                ExtendedFloatingActionButton(
                    onClick = onScanClicked,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource(Res.string.action_scan)) },
                )
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (receipts.isEmpty()) {
                EmptyState(onScanClicked = onScanClicked)
                return@Column
            }

            SearchField(
                query = query,
                onQueryChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            if (filteredReceipts.isEmpty()) {
                NoResultsState(query = query)
            } else {
                Text(
                    text = stringResource(
                        Res.string.receipts_count,
                        filteredReceipts.size,
                        formatSize(totalSize),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(filteredReceipts, key = { it.id }) { receipt ->
                        ReceiptCard(
                            receipt = receipt,
                            selectionMode = inSelectionMode,
                            selected = receipt.id in selection,
                            onTap = {
                                if (inSelectionMode) onToggleSelection(receipt)
                                else onItemClick(receipt)
                            },
                            onLongTap = { onToggleSelection(receipt) },
                            onOpen = { onOpen(receipt) },
                            onShare = { onShare(receipt) },
                            onRename = { renameTarget = receipt },
                            onDelete = { deleteTarget = receipt },
                        )
                    }
                }
            }
        }
    }

    renameTarget?.let { target ->
        RenameDialog(
            initial = target.name,
            onDismiss = { renameTarget = null },
            onConfirm = { newName ->
                onRename(target, newName)
                renameTarget = null
            },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(Res.string.dialog_delete_title)) },
            text = { Text(stringResource(Res.string.dialog_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(target)
                    deleteTarget = null
                }) { Text(stringResource(Res.string.dialog_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(Res.string.dialog_cancel))
                }
            },
        )
    }

    if (settingsOpen) {
        SettingsDialog(
            lockEnabled = lockEnabled,
            lockAvailable = lockAvailable,
            onToggleLock = onToggleLock,
            onDismiss = { settingsOpen = false },
        )
    }

    if (deleteManyOpen) {
        AlertDialog(
            onDismissRequest = { deleteManyOpen = false },
            title = { Text(stringResource(Res.string.dialog_delete_many_title, selection.size)) },
            text = { Text(stringResource(Res.string.dialog_delete_many_message)) },
            confirmButton = {
                TextButton(onClick = {
                    deleteManyOpen = false
                    onDeleteSelected()
                }) { Text(stringResource(Res.string.dialog_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteManyOpen = false }) {
                    Text(stringResource(Res.string.dialog_cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(
    count: Int,
    onClear: () -> Unit,
    onSelectAll: () -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        title = { Text(stringResource(Res.string.selection_count, count)) },
        navigationIcon = {
            IconButton(onClick = onClear) {
                Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.selection_clear))
            }
        },
        actions = {
            IconButton(onClick = onSelectAll) {
                Icon(Icons.Default.DoneAll, contentDescription = stringResource(Res.string.selection_all))
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, contentDescription = stringResource(Res.string.action_share))
            }
            IconButton(onClick = onExport) {
                Icon(Icons.Default.FolderOpen, contentDescription = stringResource(Res.string.action_export))
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(Res.string.action_delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        },
    )
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = { Text(stringResource(Res.string.search_placeholder)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = stringResource(Res.string.search_clear),
                    )
                }
            }
        } else null,
        modifier = modifier,
    )
}

@Composable
private fun SortMenu(
    expanded: Boolean,
    current: SortOption,
    onDismiss: () -> Unit,
    onSelect: (SortOption) -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        sortMenuEntries().forEach { (option, labelRes) ->
            DropdownMenuItem(
                text = { Text(stringResource(labelRes)) },
                onClick = { onSelect(option) },
                trailingIcon = if (option == current) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else null,
            )
        }
    }
}

private fun sortMenuEntries() = listOf(
    SortOption.DateDesc to Res.string.sort_date_desc,
    SortOption.DateAsc to Res.string.sort_date_asc,
    SortOption.NameAsc to Res.string.sort_name_asc,
    SortOption.SizeDesc to Res.string.sort_size_desc,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReceiptCard(
    receipt: Receipt,
    selectionMode: Boolean,
    selected: Boolean,
    onTap: () -> Unit,
    onLongTap: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val cardSemantics = "${receipt.name}. ${formatDate(receipt.createdAt)}"
    Card(
        colors = if (selected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        } else CardDefaults.cardColors(),
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = cardSemantics
            }
            .combinedClickable(onClick = onTap, onLongClick = onLongTap),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectionMode) {
                    Icon(
                        imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                PdfThumbnail(
                    receipt = receipt,
                    modifier = Modifier
                        .size(width = 56.dp, height = 72.dp)
                        .clip(RoundedCornerShape(6.dp)),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = receipt.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = formatDate(receipt.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${stringResource(Res.string.pages_label, receipt.pageCount)}" +
                            "  ·  ${formatSize(receipt.sizeBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // Les actions individuelles sont masquées en mode sélection — l'utilisateur agit en lot.
            if (!selectionMode) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = onOpen) {
                        Icon(Icons.Default.OpenInNew, contentDescription = stringResource(Res.string.action_open))
                    }
                    IconButton(onClick = onShare) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(Res.string.action_share))
                    }
                    IconButton(onClick = onRename) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.action_rename))
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(Res.string.action_delete),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(onScanClicked: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(Res.string.empty_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onScanClicked) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(Res.string.empty_cta))
            }
        }
    }
}

@Composable
private fun NoResultsState(query: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.search_no_results, query),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsDialog(
    lockEnabled: Boolean,
    lockAvailable: Boolean,
    onToggleLock: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.settings_title)) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.settings_lock_label),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = stringResource(Res.string.settings_lock_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = lockEnabled && lockAvailable,
                        enabled = lockAvailable,
                        onCheckedChange = onToggleLock,
                    )
                }
                if (!lockAvailable) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.settings_lock_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.dialog_confirm))
            }
        },
    )
}

@Composable
private fun RenameDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.dialog_rename_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text(stringResource(Res.string.dialog_rename_hint)) },
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text(stringResource(Res.string.dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.dialog_cancel))
            }
        },
    )
}

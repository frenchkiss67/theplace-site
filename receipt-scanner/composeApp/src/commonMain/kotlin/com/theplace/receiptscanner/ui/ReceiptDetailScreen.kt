package com.theplace.receiptscanner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.theplace.receiptscanner.data.Receipt
import com.theplace.receiptscanner.data.ReceiptCategory
import com.theplace.receiptscanner.platform.PdfPreview
import com.theplace.receiptscanner.resources.Res
import com.theplace.receiptscanner.resources.action_delete
import com.theplace.receiptscanner.resources.action_open
import com.theplace.receiptscanner.resources.action_rename
import com.theplace.receiptscanner.resources.action_share
import com.theplace.receiptscanner.resources.amount_hint
import com.theplace.receiptscanner.resources.amount_invalid
import com.theplace.receiptscanner.resources.amount_label
import com.theplace.receiptscanner.resources.amount_unit
import com.theplace.receiptscanner.resources.category_label
import com.theplace.receiptscanner.resources.category_none
import com.theplace.receiptscanner.resources.detail_back
import com.theplace.receiptscanner.resources.detail_open_external
import com.theplace.receiptscanner.resources.detail_title
import com.theplace.receiptscanner.resources.dialog_cancel
import com.theplace.receiptscanner.resources.dialog_confirm
import com.theplace.receiptscanner.resources.dialog_delete_message
import com.theplace.receiptscanner.resources.dialog_delete_title
import com.theplace.receiptscanner.resources.dialog_rename_hint
import com.theplace.receiptscanner.resources.dialog_rename_title
import com.theplace.receiptscanner.resources.pages_label
import com.theplace.receiptscanner.util.formatAmount
import com.theplace.receiptscanner.util.formatDate
import com.theplace.receiptscanner.util.formatSize
import com.theplace.receiptscanner.util.parseAmountCents
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptDetailScreen(
    receipt: Receipt,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onRename: (Receipt, String) -> Unit,
    onDelete: (Receipt) -> Unit,
    onOpen: (Receipt) -> Unit,
    onShare: (Receipt) -> Unit,
    onCategoryChange: (Receipt, ReceiptCategory?) -> Unit,
    onAmountChange: (Receipt, Long?) -> Unit,
) {
    var renameOpen by remember { mutableStateOf(false) }
    var deleteOpen by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.detail_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onShare(receipt) }) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(Res.string.action_share))
                    }
                    IconButton(onClick = { renameOpen = true }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(Res.string.action_rename))
                    }
                    IconButton(onClick = { deleteOpen = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(Res.string.action_delete),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            Header(receipt)
            MetadataRow(
                receipt = receipt,
                onCategoryChange = { onCategoryChange(receipt, it) },
                onAmountChange = { onAmountChange(receipt, it) },
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.weight(1f, fill = true).fillMaxWidth()) {
                PdfPreview(receipt = receipt, modifier = Modifier.fillMaxSize())
            }
            ExternalOpenButton(onClick = { onOpen(receipt) })
        }
    }

    if (renameOpen) {
        RenameDialog(
            initial = receipt.name,
            onDismiss = { renameOpen = false },
            onConfirm = { newName ->
                onRename(receipt, newName)
                renameOpen = false
            },
        )
    }

    if (deleteOpen) {
        AlertDialog(
            onDismissRequest = { deleteOpen = false },
            title = { Text(stringResource(Res.string.dialog_delete_title)) },
            text = { Text(stringResource(Res.string.dialog_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    deleteOpen = false
                    onDelete(receipt)
                }) { Text(stringResource(Res.string.dialog_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteOpen = false }) {
                    Text(stringResource(Res.string.dialog_cancel))
                }
            },
        )
    }
}

@Composable
private fun Header(receipt: Receipt) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = receipt.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatDate(receipt.createdAt),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(Res.string.pages_label, receipt.pageCount) +
                "  ·  ${formatSize(receipt.sizeBytes)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ExternalOpenButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        OutlinedButton(onClick = onClick) {
            Icon(Icons.Default.OpenInNew, contentDescription = null)
            Spacer(modifier = Modifier.height(0.dp))
            Text(
                text = stringResource(Res.string.detail_open_external),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetadataRow(
    receipt: Receipt,
    onCategoryChange: (ReceiptCategory?) -> Unit,
    onAmountChange: (Long?) -> Unit,
) {
    var categoryOpen by remember { mutableStateOf(false) }
    val categoryLabel = receipt.category?.let { stringResource(it.labelRes()) }
        ?: stringResource(Res.string.category_none)

    // L'amount est édité localement et propagé au repo seulement quand le format
    // est valide ; on garde la saisie brute dans le state pour ne pas bouger le
    // curseur pendant la frappe.
    var amountText by remember(receipt.id, receipt.totalCents) {
        mutableStateOf(receipt.totalCents?.let { formatAmount(it).removeSuffix(" €") } ?: "")
    }
    var amountError by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.weight(1f)) {
            OutlinedButton(
                onClick = { categoryOpen = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(Res.string.category_label) + " : " + categoryLabel,
                    maxLines = 1,
                )
            }
            DropdownMenu(
                expanded = categoryOpen,
                onDismissRequest = { categoryOpen = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.category_none)) },
                    onClick = {
                        onCategoryChange(null)
                        categoryOpen = false
                    },
                )
                ReceiptCategory.entries.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(stringResource(cat.labelRes())) },
                        onClick = {
                            onCategoryChange(cat)
                            categoryOpen = false
                        },
                    )
                }
            }
        }
        OutlinedTextField(
            value = amountText,
            onValueChange = { raw ->
                amountText = raw
                if (raw.isBlank()) {
                    amountError = false
                    onAmountChange(null)
                    return@OutlinedTextField
                }
                val parsed = parseAmountCents(raw)
                amountError = parsed == null
                if (parsed != null) onAmountChange(parsed)
            },
            singleLine = true,
            isError = amountError,
            label = { Text(stringResource(Res.string.amount_label)) },
            placeholder = { Text(stringResource(Res.string.amount_hint)) },
            trailingIcon = { Text(stringResource(Res.string.amount_unit)) },
            supportingText = if (amountError) {
                { Text(stringResource(Res.string.amount_invalid)) }
            } else null,
            modifier = Modifier.weight(1f),
        )
    }
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

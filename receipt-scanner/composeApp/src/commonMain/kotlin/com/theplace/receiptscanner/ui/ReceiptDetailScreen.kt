package com.theplace.receiptscanner.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.material3.rememberDatePickerState
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
import com.theplace.receiptscanner.resources.detail_rerun_ocr
import com.theplace.receiptscanner.resources.detail_title
import com.theplace.receiptscanner.resources.warranty_active_until
import com.theplace.receiptscanner.resources.warranty_clear
import com.theplace.receiptscanner.resources.warranty_duration
import com.theplace.receiptscanner.resources.warranty_expired
import com.theplace.receiptscanner.resources.warranty_expires_in
import com.theplace.receiptscanner.resources.warranty_months_short
import com.theplace.receiptscanner.resources.warranty_none
import com.theplace.receiptscanner.resources.warranty_pick_date_first
import com.theplace.receiptscanner.resources.warranty_purchased_at
import com.theplace.receiptscanner.resources.warranty_section
import com.theplace.receiptscanner.resources.warranty_set_date
import com.theplace.receiptscanner.resources.warranty_year
import com.theplace.receiptscanner.resources.warranty_years
import com.theplace.receiptscanner.resources.dialog_cancel
import com.theplace.receiptscanner.resources.dialog_confirm
import com.theplace.receiptscanner.resources.dialog_delete_message
import com.theplace.receiptscanner.resources.dialog_delete_title
import com.theplace.receiptscanner.resources.dialog_rename_hint
import com.theplace.receiptscanner.resources.dialog_rename_title
import com.theplace.receiptscanner.resources.pages_label
import com.theplace.receiptscanner.util.daysUntilWarrantyEnd
import com.theplace.receiptscanner.util.formatAmount
import com.theplace.receiptscanner.util.formatDate
import com.theplace.receiptscanner.util.formatDateOnly
import com.theplace.receiptscanner.util.formatSize
import com.theplace.receiptscanner.util.nowMs
import com.theplace.receiptscanner.util.parseAmountCents
import com.theplace.receiptscanner.util.warrantyEndMs
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
    onPurchasedAtChange: (Receipt, Long?) -> Unit,
    onWarrantyMonthsChange: (Receipt, Int?) -> Unit,
    onRerunOcr: (Receipt) -> Unit,
) {
    var renameOpen by remember { mutableStateOf(false) }
    var deleteOpen by remember { mutableStateOf(false) }
    var overflowOpen by remember { mutableStateOf(false) }

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
                    IconButton(onClick = { overflowOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = overflowOpen,
                        onDismissRequest = { overflowOpen = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.detail_rerun_ocr)) },
                            onClick = {
                                overflowOpen = false
                                onRerunOcr(receipt)
                            },
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
            WarrantySection(
                receipt = receipt,
                onPurchasedAtChange = { onPurchasedAtChange(receipt, it) },
                onWarrantyMonthsChange = { onWarrantyMonthsChange(receipt, it) },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WarrantySection(
    receipt: Receipt,
    onPurchasedAtChange: (Long?) -> Unit,
    onWarrantyMonthsChange: (Int?) -> Unit,
) {
    var pickerOpen by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = stringResource(Res.string.warranty_section),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            val dateLabel = receipt.purchasedAt?.let { formatDateOnly(it) }
            Text(
                text = stringResource(Res.string.warranty_purchased_at) + " : " + (dateLabel ?: "—"),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { pickerOpen = true }) {
                Text(stringResource(Res.string.warranty_set_date))
            }
            if (receipt.purchasedAt != null) {
                TextButton(onClick = {
                    onPurchasedAtChange(null)
                    onWarrantyMonthsChange(null)
                }) {
                    Text(stringResource(Res.string.warranty_clear))
                }
            }
        }

        // Durée : 4 chips classiques. Activable seulement si une date est posée.
        val durationEnabled = receipt.purchasedAt != null
        Text(
            text = stringResource(Res.string.warranty_duration),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DurationChip(label = stringResource(Res.string.warranty_none),
                selected = receipt.warrantyMonths == null,
                enabled = durationEnabled,
                onClick = { onWarrantyMonthsChange(null) })
            DurationChip(label = stringResource(Res.string.warranty_months_short, 6),
                selected = receipt.warrantyMonths == 6,
                enabled = durationEnabled,
                onClick = { onWarrantyMonthsChange(6) })
            DurationChip(label = stringResource(Res.string.warranty_year),
                selected = receipt.warrantyMonths == 12,
                enabled = durationEnabled,
                onClick = { onWarrantyMonthsChange(12) })
            DurationChip(label = stringResource(Res.string.warranty_years, 2),
                selected = receipt.warrantyMonths == 24,
                enabled = durationEnabled,
                onClick = { onWarrantyMonthsChange(24) })
            DurationChip(label = stringResource(Res.string.warranty_years, 3),
                selected = receipt.warrantyMonths == 36,
                enabled = durationEnabled,
                onClick = { onWarrantyMonthsChange(36) })
        }

        // Récap couverture / expiration.
        val now = remember { nowMs() }
        val endMs = warrantyEndMs(receipt.purchasedAt, receipt.warrantyMonths)
        val daysLeft = daysUntilWarrantyEnd(now, receipt.purchasedAt, receipt.warrantyMonths)
        if (endMs != null && daysLeft != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (daysLeft < 0) {
                    stringResource(Res.string.warranty_expired)
                } else {
                    stringResource(Res.string.warranty_active_until, formatDateOnly(endMs)) +
                        " · " + stringResource(Res.string.warranty_expires_in, daysLeft)
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (daysLeft < 0) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else if (!durationEnabled) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.warranty_pick_date_first),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (pickerOpen) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = receipt.purchasedAt ?: receipt.createdAt,
        )
        DatePickerDialog(
            onDismissRequest = { pickerOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    onPurchasedAtChange(datePickerState.selectedDateMillis)
                    pickerOpen = false
                }) { Text(stringResource(Res.string.dialog_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pickerOpen = false }) {
                    Text(stringResource(Res.string.dialog_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun DurationChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        label = { Text(label) },
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

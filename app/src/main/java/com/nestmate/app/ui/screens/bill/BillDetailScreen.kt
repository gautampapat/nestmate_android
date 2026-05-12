package com.nestmate.app.ui.screens.bill

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nestmate.app.data.model.BillGroup
import com.nestmate.app.data.model.Participant
import com.nestmate.app.data.model.SettlementTransfer
import com.nestmate.app.ui.components.NestMateCard
import com.nestmate.app.utils.CurrencyFormatter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.nestmate.app.ui.components.NestMateTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillDetailScreen(
    billId: String,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: BillSplitterViewModel = hiltViewModel()

    var bill by remember { mutableStateOf<BillGroup?>(null) }
    LaunchedEffect(billId) {
        viewModel.getBill(billId).collectLatest { bill = it }
    }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val error by viewModel.error.collectAsStateWithLifecycle()
    LaunchedEffect(error) {
        error?.let { snackbar.showSnackbar(it); viewModel.clearError() }
    }

    var menuOpen by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }
    var payingParticipant by remember { mutableStateOf<Participant?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            NestMateTopBar(
                title = { Text(bill?.title.orEmpty().ifBlank { "Bill" }) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Share summary") },
                            onClick = {
                                menuOpen = false
                                bill?.let { shareSummary(context, it) }
                            },
                            leadingIcon = { Icon(Icons.Filled.Share, null) },
                        )
                        DropdownMenuItem(
                            text = { Text("Export CSV") },
                            onClick = {
                                menuOpen = false
                                bill?.let { b ->
                                    viewModel.exportCsv(b, context)?.let { uri ->
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/csv"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        val chooser = Intent.createChooser(intent, "Share CSV")
                                        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(chooser)
                                    }
                                }
                            },
                            leadingIcon = { Icon(Icons.Filled.Download, null) },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = { menuOpen = false; deleteDialog = true },
                            leadingIcon = { Icon(Icons.Filled.Delete, null) },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        val current = bill
        if (current == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Loading…", color = MaterialTheme.colorScheme.onBackground)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val base = current.totalAmount + current.taxAmount +
                current.serviceChargeAmount + current.tipAmount
            NestMateCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = CurrencyFormatter.formatPaise(base),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "${current.splitMethod.name}  •  ${current.participants.size} participants",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (current.isSettled) {
                        Text(
                            "Settled",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }

            Text(
                text = "Participants",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            current.participants.forEach { p ->
                NestMateCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    p.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    "Share: ${CurrencyFormatter.formatPaise(p.shareAmount)}  •  Paid: ${CurrencyFormatter.formatPaise(p.paidAmount)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    "Status: ${p.paymentStatus.name}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = when (p.paymentStatus.name) {
                                        "PAID" -> MaterialTheme.colorScheme.secondary
                                        "PARTIALLY_PAID" -> MaterialTheme.colorScheme.tertiary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                            TextButton(onClick = { payingParticipant = p }) {
                                Text("Update")
                            }
                        }
                    }
                }
            }

            val transfers = viewModel.simplify(current.participants)
            if (transfers.isNotEmpty()) {
                Text(
                    "Settlement",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                transfers.forEach { t ->
                    SettlementRow(t)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    payingParticipant?.let { p ->
        UpdatePaidDialog(
            initialPaid = p.paidAmount,
            onDismiss = { payingParticipant = null },
            onConfirm = { newPaise ->
                viewModel.markPaid(billId, p.id, newPaise)
                payingParticipant = null
            },
        )
    }

    if (deleteDialog) {
        AlertDialog(
            onDismissRequest = { deleteDialog = false },
            title = { Text("Delete bill?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    deleteDialog = false
                    viewModel.deleteBill(billId) { ok -> if (ok) onNavigateBack() }
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SettlementRow(t: SettlementTransfer) {
    NestMateCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${t.fromName} → ${t.toName}",
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                CurrencyFormatter.formatPaise(t.amount),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun UpdatePaidDialog(
    initialPaid: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    var amountRs by remember { mutableStateOf((initialPaid / 100L).toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update payment") },
        text = {
            OutlinedTextField(
                value = amountRs,
                onValueChange = { amountRs = it.filter { ch -> ch.isDigit() } },
                label = { Text("Paid so far (Rs)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                ),
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val rupees = amountRs.toLongOrNull() ?: 0L
                onConfirm(rupees * 100L)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun shareSummary(context: android.content.Context, bill: BillGroup) {
    val sb = StringBuilder()
    sb.appendLine("Bill: ${bill.title}")
    sb.appendLine("Total: ${CurrencyFormatter.formatPaise(bill.totalAmount + bill.taxAmount + bill.serviceChargeAmount + bill.tipAmount)}")
    sb.appendLine()
    sb.appendLine("Participants:")
    bill.participants.forEach { p ->
        sb.appendLine("• ${p.name}: share ${CurrencyFormatter.formatPaise(p.shareAmount)}, paid ${CurrencyFormatter.formatPaise(p.paidAmount)}, ${p.paymentStatus.name}")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, sb.toString())
    }
    val chooser = Intent.createChooser(intent, "Share summary")
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
}


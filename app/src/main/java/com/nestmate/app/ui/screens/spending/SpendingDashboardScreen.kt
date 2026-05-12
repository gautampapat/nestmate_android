package com.nestmate.app.ui.screens.spending

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nestmate.app.data.model.SpendingTransaction
import com.nestmate.app.ui.navigation.Screen
import java.text.SimpleDateFormat
import java.util.*
import com.nestmate.app.ui.components.NestMateTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpendingDashboardScreen(
    onNavigateToBudget: () -> Unit,
    onNavigateToInsights: () -> Unit,
    viewModel: SpendingViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val totalIncome by viewModel.totalIncome.collectAsStateWithLifecycle()
    val totalExpenses by viewModel.totalExpenses.collectAsStateWithLifecycle()
    val totalBalance by viewModel.totalBalance.collectAsStateWithLifecycle()
    val budgetProgress by viewModel.budgetProgress.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val selectedYearMonth by viewModel.selectedYearMonth.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    var showAddSheet by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<SpendingTransaction?>(null) }
    var deleteDialogTx by remember { mutableStateOf<SpendingTransaction?>(null) }

    val warningBudgets = budgetProgress.filter { it.isWarning }

    Scaffold(
        topBar = {
            NestMateTopBar(
                title = { Text("Spending Tracker", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToInsights) {
                        Icon(Icons.Default.PieChart, "Insights")
                    }
                    IconButton(onClick = {
                        viewModel.exportCsv { uri ->
                            uri?.let {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_STREAM, it)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Export Spending CSV"))
                            }
                        }
                    }) {
                        Icon(Icons.Default.FileDownload, "Export CSV")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddSheet = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Add Transaction") }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Summary strip
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryCard("Balance", totalBalance, Color(0xFF7B1FA2), modifier = Modifier.weight(1f))
                    SummaryCard("Income", totalIncome, Color(0xFF2E7D32), modifier = Modifier.weight(1f))
                    SummaryCard("Expenses", totalExpenses, Color(0xFFC62828), modifier = Modifier.weight(1f))
                }
            }

            // Period toggle
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("DAILY", "WEEKLY", "MONTHLY").forEach { period ->
                            FilterChip(
                                selected = selectedPeriod == period,
                                onClick = { viewModel.setSelectedPeriod(period) },
                                label = { Text(period.lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                    if (selectedPeriod == "MONTHLY") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.navigateMonth(-1) }) {
                                Icon(Icons.Default.ChevronLeft, "Previous")
                            }
                            val months = listOf("", "Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
                            Text("${months[selectedYearMonth.second]} ${selectedYearMonth.first}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            IconButton(onClick = { viewModel.navigateMonth(1) }) {
                                Icon(Icons.Default.ChevronRight, "Next")
                            }
                        }
                    }
                }
            }

            // Budget warnings
            if (warningBudgets.isNotEmpty()) {
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        items(warningBudgets) { bp ->
                            val pct = if (bp.limit > 0) (bp.spent.toFloat() / bp.limit * 100).toInt() else 0
                            AssistChip(
                                onClick = onNavigateToBudget,
                                label = { Text("${bp.category} ${pct}% used ⚠️", style = MaterialTheme.typography.bodySmall) },
                                colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                            )
                        }
                    }
                }
            }

            // Transaction list header
            item {
                Text("Transactions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }

            if (isLoading && transactions.isEmpty()) {
                item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            } else if (transactions.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("💰", style = MaterialTheme.typography.displayMedium)
                            Spacer(Modifier.height(8.dp))
                            Text("No transactions yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(transactions, key = { it.id }) { tx ->
                    TransactionRow(
                        tx = tx,
                        onClick = { editingTransaction = tx },
                        onLongClick = { deleteDialogTx = tx }
                    )
                }
            }
        }
    }

    if (showAddSheet || editingTransaction != null) {
        AddTransactionSheet(
            existingTransaction = editingTransaction,
            categories = categories,
            onSave = { tx ->
                if (editingTransaction != null) viewModel.updateTransaction(tx) else viewModel.addTransaction(tx)
                showAddSheet = false; editingTransaction = null
            },
            onAddCategory = { name, type -> viewModel.addCategory(name, type) },
            onDismiss = { showAddSheet = false; editingTransaction = null }
        )
    }

    deleteDialogTx?.let { tx ->
        AlertDialog(
            onDismissRequest = { deleteDialogTx = null },
            title = { Text("Delete Transaction?") },
            text = { Text("This will permanently remove this transaction.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteTransaction(tx.id); deleteDialogTx = null }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Delete")
                }
            },
            dismissButton = { TextButton(onClick = { deleteDialogTx = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SummaryCard(label: String, amountPaise: Long, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
            Spacer(Modifier.height(4.dp))
            Text(formatRupees(amountPaise), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransactionRow(tx: SpendingTransaction, onClick: () -> Unit, onLongClick: () -> Unit) {
    val isIncome = tx.type == "INCOME"
    val sdf = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(if (isIncome) Color(0xFF2E7D32).copy(0.15f) else Color(0xFFC62828).copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(tx.category.firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.labelLarge, color = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828), fontWeight = FontWeight.Bold)
                }
                Column {
                    Text(tx.category, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text(tx.notes ?: sdf.format(Date(tx.date)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                "${if (isIncome) "+" else "−"} ${formatRupees(tx.amount)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)
            )
        }
    }
}

fun formatRupees(paise: Long): String {
    val rs = paise / 100.0
    return "₹${"%.0f".format(rs)}"
}

@Suppress("DEPRECATION")
private annotation class ExperimentalFoundationApi

@Suppress("DEPRECATION")
private fun Modifier.combinedClickable(onClick: () -> Unit, onLongClick: () -> Unit): Modifier =
    this.then(Modifier.clickable { onClick() })

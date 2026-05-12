package com.nestmate.app.ui.screens.spending

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nestmate.app.ui.components.NestMateTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    onNavigateBack: () -> Unit,
    viewModel: SpendingViewModel = hiltViewModel(),
) {
    val budgetProgress by viewModel.budgetProgress.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedYearMonth by viewModel.selectedYearMonth.collectAsStateWithLifecycle()
    val expenseCategories = categories.filter { it.transactionType == "EXPENSE" }

    var editingCategory by remember { mutableStateOf<String?>(null) }
    var limitInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            NestMateTopBar(
                title = { Text("Budgets", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { viewModel.navigateMonth(-1) }) { Icon(Icons.Default.ChevronLeft, "Previous") }
                    val months = listOf("","Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
                    Text("${months[selectedYearMonth.second]} ${selectedYearMonth.first}", style = MaterialTheme.typography.titleSmall)
                    IconButton(onClick = { viewModel.navigateMonth(1) }) { Icon(Icons.Default.ChevronRight, "Next") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(expenseCategories) { cat ->
                val bp = budgetProgress.firstOrNull { it.category == cat.name }
                val spent = bp?.spent ?: 0L
                val limit = bp?.limit ?: 0L
                val pct = if (limit > 0) (spent.toFloat() / limit).coerceAtMost(1f) else 0f
                val isWarning = bp?.isWarning == true

                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clickable {
                        editingCategory = cat.name
                        limitInput = if (limit > 0) (limit / 100).toString() else ""
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(cat.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            if (limit > 0) {
                                Text("${formatRupees(spent)} / ${formatRupees(limit)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                AssistChip(onClick = { editingCategory = cat.name; limitInput = "" }, label = { Text("Set Budget") })
                            }
                        }
                        if (limit > 0) {
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { pct },
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                color = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    editingCategory?.let { catName ->
        AlertDialog(
            onDismissRequest = { editingCategory = null },
            title = { Text("Set Budget — $catName") },
            text = {
                OutlinedTextField(
                    value = limitInput,
                    onValueChange = { limitInput = it },
                    label = { Text("Monthly Limit (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    val limitRs = limitInput.toLongOrNull() ?: 0L
                    viewModel.setBudget(catName, limitRs * 100)
                    editingCategory = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editingCategory = null }) { Text("Cancel") } }
        )
    }
}

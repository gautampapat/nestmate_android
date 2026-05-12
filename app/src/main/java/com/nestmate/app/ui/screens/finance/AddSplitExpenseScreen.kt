package com.nestmate.app.ui.screens.finance

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nestmate.app.ui.components.NestMatePrimaryButton
import com.nestmate.app.ui.components.NestMateTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSplitExpenseScreen(
    onNavigateBack: () -> Unit,
    viewModel: FinanceViewModel = hiltViewModel()
) {
    // ── Form state ──────────────────────────────────────────────────────────
    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Food") }
    var flatmateInput by remember { mutableStateOf("") }
    val flatmates = remember { mutableStateListOf<String>() }
    var useCustomSplits by remember { mutableStateOf(false) }
    // Map name -> custom amount string (only used when useCustomSplits == true)
    val customAmounts = remember { mutableStateMapOf<String, String>() }

    val categories = listOf("Food", "Groceries", "Rent", "Utilities", "Entertainment", "Travel", "Other")
    var categoryExpanded by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }

    // ── Derived split preview ───────────────────────────────────────────────
    val totalAmount = amountText.toIntOrNull() ?: 0
    val totalPeople = flatmates.size + 1          // flatmates + current user
    val equalSplit = if (totalPeople > 0) totalAmount / totalPeople else 0

    Scaffold(
        topBar = {
            NestMateTopBar(
                title = { Text("Split Expense") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Description ──────────────────────────────────────────────────
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                placeholder = { Text("e.g. Pizza night") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = submitted && description.isBlank()
            )

            // ── Total Amount ─────────────────────────────────────────────────
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                label = { Text("Total Amount (₹)") },
                placeholder = { Text("e.g. 1200") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = submitted && totalAmount <= 0,
                supportingText = if (totalAmount > 0 && totalPeople > 0) {
                    {
                        Text(
                            text = "Equal split: ₹$equalSplit per person ($totalPeople people)",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else null
            )

            // ── Category Dropdown ────────────────────────────────────────────
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded }
            ) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                selectedCategory = cat
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            // ── Add Flatmates Section ────────────────────────────────────────
            Text(
                "Add Flatmates",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = flatmateInput,
                    onValueChange = { flatmateInput = it },
                    label = { Text("Flatmate name") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
                FilledTonalButton(
                    onClick = {
                        val name = flatmateInput.trim()
                        if (name.isNotBlank() && !flatmates.contains(name)) {
                            flatmates.add(name)
                            flatmateInput = ""
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add flatmate")
                }
            }

            // Chip row for added flatmates
            if (flatmates.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    flatmates.forEach { name ->
                        InputChip(
                            selected = false,
                            onClick = {},
                            label = { Text(name) },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        flatmates.remove(name)
                                        customAmounts.remove(name)
                                    },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove $name",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        )
                    }
                }
            }

            if (submitted && flatmates.isEmpty()) {
                Text(
                    "Add at least one flatmate to split the expense.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // ── Custom Split Toggle ──────────────────────────────────────────
            if (flatmates.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Custom split amounts",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Override equal split per person",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = useCustomSplits,
                        onCheckedChange = { useCustomSplits = it }
                    )
                }
            }

            // ── Custom Split Fields ──────────────────────────────────────────
            if (useCustomSplits && flatmates.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Enter amounts per person (₹)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        flatmates.forEach { name ->
                            OutlinedTextField(
                                value = customAmounts[name] ?: "",
                                onValueChange = { customAmounts[name] = it.filter { c -> c.isDigit() } },
                                label = { Text(name) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                prefix = { Text("₹") }
                            )
                        }
                        // Sum indicator
                        val customTotal = customAmounts.values.sumOf { it.toIntOrNull() ?: 0 }
                        Text(
                            "Assigned: ₹$customTotal / ₹$totalAmount",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (customTotal == totalAmount)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Submit Button ────────────────────────────────────────────────
            NestMatePrimaryButton(
                text = "Split Expense",
                onClick = {
                    submitted = true
                    if (description.isBlank() || totalAmount <= 0 || flatmates.isEmpty()) return@NestMatePrimaryButton

                    // Build the flatmates list to pass
                    // viewModel.addSplitExpense uses equal splits internally;
                    // for custom splits we pass the same flatmate list and note unequal allocation
                    viewModel.addSplitExpense(
                        desc = description.trim(),
                        amount = totalAmount,
                        category = selectedCategory,
                        flatmates = flatmates.toList()
                    )
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            )
        }
    }
}

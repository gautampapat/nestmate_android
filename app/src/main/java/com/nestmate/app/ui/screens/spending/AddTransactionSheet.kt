package com.nestmate.app.ui.screens.spending

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nestmate.app.data.model.SpendingCategory
import com.nestmate.app.data.model.SpendingTransaction
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    existingTransaction: SpendingTransaction? = null,
    categories: List<SpendingCategory>,
    onSave: (SpendingTransaction) -> Unit,
    onAddCategory: (name: String, type: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var amountStr by remember { mutableStateOf(existingTransaction?.let { (it.amount / 100).toString() } ?: "") }
    var selectedType by remember { mutableStateOf(existingTransaction?.type ?: "EXPENSE") }
    var selectedCategory by remember { mutableStateOf(existingTransaction?.category ?: "") }
    var selectedDate by remember { mutableStateOf(existingTransaction?.date ?: System.currentTimeMillis()) }
    var notes by remember { mutableStateOf(existingTransaction?.notes ?: "") }
    var isRecurring by remember { mutableStateOf(existingTransaction?.isRecurring ?: false) }
    var recurrenceInterval by remember { mutableStateOf(existingTransaction?.recurrenceInterval ?: "MONTHLY") }
    var newCategoryInput by remember { mutableStateOf("") }
    var showAddCategory by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val filteredCategories = categories.filter { it.transactionType == selectedType }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            Text(
                if (existingTransaction != null) "Edit Transaction" else "Add Transaction",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            // Amount display
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text(
                    text = if (amountStr.isEmpty()) "₹0" else "₹$amountStr",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.height(12.dp))

            // Numpad
            val keys = listOf("7","8","9","4","5","6","1","2","3","","0","⌫")
            val rows = keys.chunked(3)
            rows.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { key ->
                        if (key.isEmpty()) {
                            Spacer(Modifier.weight(1f))
                        } else {
                            FilledTonalButton(
                                onClick = {
                                    when (key) {
                                        "⌫" -> if (amountStr.isNotEmpty()) amountStr = amountStr.dropLast(1)
                                        "." -> if ("." !in amountStr) amountStr += "."
                                        else -> if (amountStr.length < 10) amountStr += key
                                    }
                                },
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                if (key == "⌫") Icon(Icons.Default.Backspace, null)
                                else Text(key, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            Spacer(Modifier.height(12.dp))

            // Income/Expense toggle
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("EXPENSE", "INCOME").forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type; selectedCategory = "" },
                        label = { Text(type.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Category picker
            Text("Category", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                items(filteredCategories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat.name,
                        onClick = { selectedCategory = cat.name },
                        label = { Text(cat.name) }
                    )
                }
                item {
                    if (showAddCategory) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(
                                value = newCategoryInput,
                                onValueChange = { newCategoryInput = it },
                                placeholder = { Text("Category name") },
                                modifier = Modifier.width(140.dp),
                                singleLine = true
                            )
                            TextButton(onClick = {
                                if (newCategoryInput.isNotBlank()) {
                                    onAddCategory(newCategoryInput, selectedType)
                                    selectedCategory = newCategoryInput
                                    newCategoryInput = ""
                                    showAddCategory = false
                                }
                            }) { Text("Add") }
                        }
                    } else {
                        AssistChip(
                            onClick = { showAddCategory = true },
                            label = { Text("+") },
                            leadingIcon = { Icon(Icons.Default.Add, null, Modifier.size(16.dp)) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Date picker
            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Date: ${sdf.format(Date(selectedDate))}")
            }

            Spacer(Modifier.height(8.dp))

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(8.dp))

            // Recurring
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Recurring transaction", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = isRecurring, onCheckedChange = { isRecurring = it })
            }
            if (isRecurring) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("WEEKLY", "MONTHLY").forEach { interval ->
                        FilterChip(
                            selected = recurrenceInterval == interval,
                            onClick = { recurrenceInterval = interval },
                            label = { Text(interval.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    val amountPaise = ((amountStr.toDoubleOrNull() ?: 0.0) * 100).toLong()
                    if (amountPaise > 0 && selectedCategory.isNotBlank()) {
                        onSave(
                            SpendingTransaction(
                                id = existingTransaction?.id ?: "",
                                userId = existingTransaction?.userId ?: "",
                                type = selectedType,
                                amount = amountPaise,
                                category = selectedCategory,
                                date = selectedDate,
                                notes = notes.ifBlank { null },
                                isRecurring = isRecurring,
                                recurrenceInterval = if (isRecurring) recurrenceInterval else null,
                                createdAt = existingTransaction?.createdAt ?: System.currentTimeMillis()
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = amountStr.isNotBlank() && amountStr.toDoubleOrNull() != null && (amountStr.toDoubleOrNull() ?: 0.0) > 0 && selectedCategory.isNotBlank()
            ) { Text("Save") }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDate = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }
}

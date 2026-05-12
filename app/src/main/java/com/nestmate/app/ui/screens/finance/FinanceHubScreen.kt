package com.nestmate.app.ui.screens.finance

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.auth.FirebaseAuth
import com.nestmate.app.data.model.Expense
import com.nestmate.app.ui.components.EmptyState
import java.text.SimpleDateFormat
import java.util.*
import com.nestmate.app.ui.components.NestMateTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceHubScreen(
    onNavigateToAddExpense: () -> Unit,
    onNavigateToAddSplitExpense: () -> Unit,
    onNavigateToBillSplitter: () -> Unit = {},
    viewModel: FinanceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Personal", "Splitter", "Rent")

    Scaffold(
        topBar = {
            NestMateTopBar(
                title = { Text("Finance Hub", fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            if (selectedTabIndex == 0) {
                FloatingActionButton(onClick = onNavigateToAddExpense, containerColor = MaterialTheme.colorScheme.primary) {
                    Icon(Icons.Default.Add, contentDescription = "Add Expense")
                }
            } else if (selectedTabIndex == 1) {
                FloatingActionButton(onClick = onNavigateToBillSplitter, containerColor = MaterialTheme.colorScheme.secondary) {
                    Icon(Icons.Default.AccountCircle, contentDescription = "Open Bill Splitter")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            when (val state = uiState) {
                is FinanceState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is FinanceState.Error -> {
                    EmptyState(title = "Oops!", message = state.message)
                }
                is FinanceState.Success -> {
                    when (selectedTabIndex) {
                        0 -> PersonalTrackerTab(state.personalExpenses)
                        1 -> Column(modifier = Modifier.fillMaxSize()) {
                            TextButton(
                                onClick = onNavigateToBillSplitter,
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                            ) { Text("Open Bill Splitter →") }
                            ExpenseSplitterTab(state.owedToMe, state.iOwe, viewModel)
                        }
                        2 -> RentTrackerTab(state.rentPayments, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun PersonalTrackerTab(expenses: List<Expense>) {
    val totalSpent = expenses.sumOf { it.amount }
    val budget = 5000 // In a real app, fetch from User model
    val progress = (totalSpent.toFloat() / budget).coerceIn(0f, 1f)
    
    val progressColor = when {
        progress > 0.9f -> Color(0xFFD32F2F) // Red
        progress > 0.7f -> Color(0xFFFF8F00) // Amber
        else -> Color(0xFF2E7D32) // Forest Green
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Budget Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Progress
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
                    val trackColor = MaterialTheme.colorScheme.outlineVariant
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(
                            color = trackColor,
                            startAngle = 135f,
                            sweepAngle = 270f,
                            useCenter = false,
                            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = progressColor,
                            startAngle = 135f,
                            sweepAngle = 270f * progress,
                            useCenter = false,
                            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("used", style = MaterialTheme.typography.bodySmall)
                    }
                }
                
                Spacer(modifier = Modifier.width(24.dp))
                
                Column {
                    Text("Total Spent", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${"%,d".format(totalSpent)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = progressColor)
                    Text("of ₹${"%,d".format(budget)} budget", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Text("Recent Expenses", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))

        if (expenses.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No personal expenses recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn {
                items(expenses.sortedByDescending { it.timestamp }) { exp ->
                    ExpenseItemRow(exp)
                }
            }
        }
    }
}

@Composable
fun ExpenseItemRow(expense: Expense) {
    val formatter = SimpleDateFormat("MMM dd", Locale.getDefault())
    val date = formatter.format(Date(expense.timestamp))
    
    val icon = when (expense.category) {
        "Food/Mess" -> Icons.Default.Restaurant
        "Travel" -> Icons.Default.DirectionsBus
        "Rent" -> Icons.Default.Home
        else -> Icons.Default.Receipt
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = "Icon", tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(expense.description, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("$date • ${expense.category}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("₹${"%,d".format(expense.amount)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ExpenseSplitterTab(owedToMe: List<Expense>, iOwe: List<Expense>, viewModel: FinanceViewModel) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val totalOwedToMe = owedToMe.sumOf { it.splits.values.sum() }
    val totalIOwe = iOwe.sumOf { it.splits[currentUserId] ?: 0 }
    val netBalance = totalOwedToMe - totalIOwe

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Balance Cards
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("You are owed", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF2E7D32))
                    Text("₹${"%,d".format(totalOwedToMe)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                }
            }
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("You owe", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFD32F2F))
                    Text("₹${"%,d".format(totalIOwe)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F))
                }
            }
        }
        
        Text("Active Splits", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))

        if (owedToMe.isEmpty() && iOwe.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("You're all settled up! No active splits.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn {
                if (owedToMe.isNotEmpty()) {
                    item { Text("Owed to you", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(vertical = 8.dp)) }
                    items(owedToMe) { exp ->
                        SplitItemRow(exp, true, viewModel)
                    }
                }
                if (iOwe.isNotEmpty()) {
                    item { Text("You owe", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(vertical = 8.dp)) }
                    items(iOwe) { exp ->
                        SplitItemRow(exp, false, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun SplitItemRow(expense: Expense, isOwedToMe: Boolean, viewModel: FinanceViewModel) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val amount = if (isOwedToMe) expense.splits.values.sum() else (expense.splits[currentUserId] ?: 0)
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(expense.description, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    val text = if (isOwedToMe) "Waiting on ${expense.splits.size} people" else "Owe to ${expense.payerName}"
                    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    "₹${"%,d".format(amount)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isOwedToMe) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    if (isOwedToMe) {
                        // Mark all as settled for this expense
                        expense.splits.keys.forEach { userId ->
                            viewModel.markSettled(expense.expenseId, userId)
                        }
                    } else {
                        viewModel.markSettled(expense.expenseId, currentUserId)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(36.dp)
            ) {
                Text(if (isOwedToMe) "Mark Received" else "Mark Paid")
            }
        }
    }
}

@Composable
fun RentTrackerTab(rentPayments: List<Map<String, Any>>, viewModel: FinanceViewModel) {
    // In a real app, this would be fetched from User model config
    val rentAmount = 8500
    val dueDay = 5
    
    val calendar = Calendar.getInstance()
    val today = calendar.get(Calendar.DAY_OF_MONTH)
    val daysUntilDue = if (today <= dueDay) dueDay - today else (30 - today) + dueDay

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Upcoming Rent", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("₹${"%,d".format(rentAmount)}", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(" / month", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(bottom = 8.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                val dueText = if (daysUntilDue == 0) "Due Today!" else "Due in $daysUntilDue days"
                Text("Landlord: Mr. Sharma • $dueText", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.markRentPaid(rentAmount) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Mark This Month Paid")
                }
            }
        }

        Text("Payment History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))

        if (rentPayments.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No past rent payments recorded.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn {
                items(rentPayments) { payment ->
                    val amount = (payment["amount"] as? Long ?: 0L).toInt()
                    val timestamp = payment["timestamp"] as? Long ?: 0L
                    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    val date = formatter.format(Date(timestamp))

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFE8F5E9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Icon", tint = Color(0xFF2E7D32))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Rent Paid", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(date, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("₹${"%,d".format(amount)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

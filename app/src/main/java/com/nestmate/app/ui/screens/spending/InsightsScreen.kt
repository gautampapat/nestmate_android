package com.nestmate.app.ui.screens.spending

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin
import com.nestmate.app.ui.components.NestMateTopBar

private val ChartPalette = listOf(
    Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFFFC107), Color(0xFFE91E63),
    Color(0xFF9C27B0), Color(0xFFFF5722), Color(0xFF00BCD4), Color(0xFF8BC34A),
    Color(0xFF673AB7), Color(0xFFFF9800)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SpendingViewModel = hiltViewModel(),
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val categoryBreakdown by viewModel.categoryBreakdown.collectAsStateWithLifecycle()
    val timeSeriesData by viewModel.timeSeriesData.collectAsStateWithLifecycle()
    val totalIncome by viewModel.totalIncome.collectAsStateWithLifecycle()
    val totalExpenses by viewModel.totalExpenses.collectAsStateWithLifecycle()
    val selectedYearMonth by viewModel.selectedYearMonth.collectAsStateWithLifecycle()

    val savings = totalIncome - totalExpenses
    val mostSpent = categoryBreakdown.maxByOrNull { it.total }
    val biggestTx = transactions.filter { it.type == "EXPENSE" }.maxByOrNull { it.amount }
    val sdf = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }

    Scaffold(
        topBar = {
            NestMateTopBar(
                title = { Text("Insights", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { viewModel.navigateMonth(-1) }) { Icon(Icons.Default.ChevronLeft, "Prev") }
                    val months = listOf("","Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
                    Text("${months[selectedYearMonth.second]}", style = MaterialTheme.typography.titleSmall)
                    IconButton(onClick = { viewModel.navigateMonth(1) }) { Icon(Icons.Default.ChevronRight, "Next") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Savings card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (savings >= 0) Color(0xFF2E7D32).copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(if (savings >= 0) "You saved this month 🎉" else "Over budget this month", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(formatRupees(kotlin.math.abs(savings)), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    }
                }
            }

            // Insight cards
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (mostSpent != null) {
                        InsightCard("Most Spent", mostSpent.category, formatRupees(mostSpent.total), modifier = Modifier.weight(1f))
                    }
                    if (biggestTx != null) {
                        InsightCard("Biggest Expense", biggestTx.category, formatRupees(biggestTx.amount), modifier = Modifier.weight(1f))
                    }
                }
            }

            // Pie chart
            if (categoryBreakdown.isNotEmpty()) {
                item {
                    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Expense Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(16.dp))
                            DonutChart(categoryBreakdown)
                            Spacer(Modifier.height(16.dp))
                            // Legend
                            categoryBreakdown.forEachIndexed { idx, cat ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Canvas(Modifier.size(12.dp)) {
                                            drawCircle(color = ChartPalette[idx % ChartPalette.size])
                                        }
                                        Text(cat.category, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text("${"%.1f".format(cat.percentage)}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            // Bar chart
            if (timeSeriesData.isNotEmpty()) {
                item {
                    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Daily Spending", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(16.dp))
                            SpendingBarChart(timeSeriesData)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightCard(label: String, title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(value, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DonutChart(breakdown: List<CategorySpend>) {
    val total = breakdown.sumOf { it.total }.toFloat().coerceAtLeast(1f)
    Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
        val radius = size.minDimension / 2.2f
        val strokeWidth = radius * 0.35f
        val center = Offset(size.width / 2, size.height / 2)
        var startAngle = -90f
        breakdown.forEachIndexed { idx, cat ->
            val sweep = (cat.total / total) * 360f
            drawArc(
                color = ChartPalette[idx % ChartPalette.size],
                startAngle = startAngle,
                sweepAngle = sweep - 2f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth)
            )
            startAngle += sweep
        }
    }
}

@Composable
private fun SpendingBarChart(data: List<DailySpend>) {
    val maxAmount = data.maxOfOrNull { it.total }?.toFloat() ?: 1f
    val avg = data.map { it.total }.average().toFloat()
    val sdf = remember { SimpleDateFormat("dd", Locale.getDefault()) }
    Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
        val barWidth = (size.width / data.size.coerceAtLeast(1)) * 0.7f
        val gap = (size.width / data.size.coerceAtLeast(1)) * 0.3f
        val avgY = size.height - (avg / maxAmount) * size.height * 0.85f

        // Draw average line
        drawLine(
            color = Color(0xFFFFA000).copy(alpha = 0.7f),
            start = Offset(0f, avgY),
            end = Offset(size.width, avgY),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        )

        data.forEachIndexed { idx, day ->
            val barHeight = (day.total.toFloat() / maxAmount) * size.height * 0.85f
            val x = idx.toFloat() * (barWidth + gap) + gap / 2f
            drawRect(
                color = Color(0xFF4CAF50).copy(alpha = 0.8f),
                topLeft = Offset(x, size.height - barHeight),
                size = Size(barWidth, barHeight)
            )
        }
    }
}

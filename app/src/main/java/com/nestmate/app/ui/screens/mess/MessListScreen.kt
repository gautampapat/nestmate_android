package com.nestmate.app.ui.screens.mess

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.nestmate.app.data.model.Mess
import androidx.compose.foundation.lazy.LazyRow
import com.nestmate.app.ui.components.EmptyState
import com.nestmate.app.ui.components.NestMateCard
import com.nestmate.app.ui.components.ShimmerCard
import com.nestmate.app.ui.components.NestMateTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessListScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: MessViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val foodFilter by viewModel.foodFilter.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            NestMateTopBar(
                title = { Text("Discover Food", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val options = listOf("All", "Veg", "Non-Veg", "Both")
                items(options) { option ->
                    FilterChip(
                        selected = foodFilter == option,
                        onClick = { viewModel.setFoodFilter(option) },
                        label = { Text(option) }
                    )
                }
            }

            when (uiState) {
                is MessState.Loading -> {
                    Column(modifier = Modifier.padding(16.dp)) {
                        repeat(5) { ShimmerCard() }
                    }
                }
                is MessState.Error -> {
                    EmptyState(
                        title = "Network Error",
                        message = (uiState as MessState.Error).message,
                        buttonText = "Retry",
                        onButtonClick = { /* Real-time listener auto-retries */ }
                    )
                }
                is MessState.Success -> {
                    val state = uiState as MessState.Success
                    if (state.messes.isEmpty()) {
                        EmptyState(
                            title = "No Messes Found",
                            message = "We couldn't locate any messes nearby."
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(state.messes) { mess ->
                                MessCard(
                                    mess = mess,
                                    onVote = { level -> viewModel.submitCrowdVote(mess.messId, level) },
                                    onClick = { onNavigateToDetail(mess.messId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessCard(mess: Mess, onVote: (String) -> Unit, onClick: () -> Unit) {
    NestMateCard(onClick = onClick) {
        Column {
            // PHOTO SECTION — only if photos exist
            if (mess.photoUrls.isNotEmpty()) {
                AsyncImage(
                    model = mess.photoUrls.first(),
                    contentDescription = "Mess photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
            // EXISTING text content below
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = mess.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    val crowdColor = when (mess.derivedCrowdLevel) {
                        "high" -> MaterialTheme.colorScheme.error
                        "medium" -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.primary
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = crowdColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = mess.derivedCrowdLevel.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = crowdColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = mess.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Text(text = "★ ${mess.rating}", style = MaterialTheme.typography.labelMedium, color = Color(0xFFFF8F00), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = mess.vegNonVeg, style = MaterialTheme.typography.labelMedium, color = if (mess.vegNonVeg == "Veg") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = "₹${mess.pricing["PerMeal"] ?: 0}/meal", style = MaterialTheme.typography.labelMedium)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = { onVote("low") }) {
                        Text("Not Crowded")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(onClick = { onVote("high") }) {
                        Text("Crowded")
                    }
                }
            }
        }
    }
}


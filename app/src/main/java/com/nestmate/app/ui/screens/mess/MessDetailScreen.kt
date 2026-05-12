package com.nestmate.app.ui.screens.mess

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Restaurant
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import coil.compose.AsyncImage
import com.nestmate.app.data.model.Mess
import kotlinx.coroutines.launch
import com.nestmate.app.ui.components.NestMateTopBar

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MessDetailScreen(
    messId: String,
    onNavigateBack: () -> Unit,
    viewModel: MessViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val selectedCrowdLevel by viewModel.selectedCrowdLevel.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(messId) {
        viewModel.loadUserVote(messId)
    }

    // Show snackbar on vote result
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    // Find the mess from the real-time flow by messId
    val mess: Mess? = if (uiState is MessState.Success) {
        (uiState as MessState.Success).messes.find { it.messId == messId }
    } else null

    Scaffold(
        topBar = {
            NestMateTopBar(
                title = { Text(mess?.name ?: "Mess Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            mess == null && uiState is MessState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            mess == null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("Mess not found", style = MaterialTheme.typography.bodyLarge)
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    // ── Photo Gallery ─────────────────────────────────────────────
                    val photoCount = mess.photoUrls.size.coerceAtLeast(1)
                    val pagerState = rememberPagerState(pageCount = { photoCount })

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                    ) { page ->
                        if (mess.photoUrls.isNotEmpty()) {
                            AsyncImage(
                                model = mess.photoUrls[page],
                                contentDescription = "Mess photo ${page + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            // Placeholder when no photos
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Restaurant,
                                    contentDescription = "No photo",
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Dot indicators (only if more than 1 photo)
                    if (mess.photoUrls.size > 1) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                        ) {
                            items(mess.photoUrls.size) { index ->
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (pagerState.currentPage == index)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                        )
                                )
                            }
                        }
                    }

                    // ── Existing detail content ───────────────────────────────────
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Header card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mess.name,
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = mess.address,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    // Crowd level indicator
                                    val crowdColor = when (mess.crowdLevel) {
                                        "high"   -> Color(0xFFD32F2F)
                                        "medium" -> Color(0xFFFF8F00)
                                        else     -> Color(0xFF2E7D32)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(crowdColor)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AssistChip(
                                        onClick = {},
                                        label = { Text(mess.vegNonVeg) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = if (mess.vegNonVeg == "Veg")
                                                Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                        )
                                    )
                                    if (mess.rating > 0) {
                                        AssistChip(
                                            onClick = {},
                                            label = { Text("★ ${"%.1f".format(mess.rating)}") }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Timings
                        if (mess.timings.isNotEmpty()) {
                            Text("Timings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            mess.timings.forEach { (meal, time) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(meal, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text(time, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Pricing
                        if (mess.pricing.isNotEmpty()) {
                            Text("Pricing", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            mess.pricing.forEach { (type, cost) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(type, style = MaterialTheme.typography.bodyMedium)
                                    Text("₹$cost", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Today's menu
                        if (mess.menu.isNotEmpty()) {
                            Text("Today's Menu", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            mess.menu.forEach { (meal, items) ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(Icons.Default.Restaurant, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(meal, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                            Text(items, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        // ── Community Vote Section ───────────────────────────────────
                        Text(
                            "How crowded is it now?",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Your vote helps others plan their visit. Tap to report current crowd.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )

                        val totalVotes = mess.crowdVotesLow + mess.crowdVotesMedium + mess.crowdVotesHigh

                        // Vote buttons showing live counts
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VoteCrowdButton(
                                label = "Low",
                                votes = mess.crowdVotesLow,
                                color = Color(0xFF2E7D32),
                                isSelected = selectedCrowdLevel == "low",
                                onClick = { viewModel.submitCrowdVote(messId, "low") },
                                modifier = Modifier.weight(1f)
                            )
                            VoteCrowdButton(
                                label = "Medium",
                                votes = mess.crowdVotesMedium,
                                color = Color(0xFFFF8F00),
                                isSelected = selectedCrowdLevel == "medium",
                                onClick = { viewModel.submitCrowdVote(messId, "medium") },
                                modifier = Modifier.weight(1f)
                            )
                            VoteCrowdButton(
                                label = "High",
                                votes = mess.crowdVotesHigh,
                                color = Color(0xFFD32F2F),
                                isSelected = selectedCrowdLevel == "high",
                                onClick = { viewModel.submitCrowdVote(messId, "high") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (totalVotes > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "$totalVotes total vote${if (totalVotes != 1) "s" else ""} · Current: ${mess.crowdLevel.replaceFirstChar { it.uppercase() }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun VoteCrowdButton(
    label: String,
    votes: Int,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) color.copy(alpha = 0.12f) else Color.Transparent,
            contentColor = if (isSelected) color else MaterialTheme.colorScheme.onSurface
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) color else MaterialTheme.colorScheme.outline)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
            Text("$votes votes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

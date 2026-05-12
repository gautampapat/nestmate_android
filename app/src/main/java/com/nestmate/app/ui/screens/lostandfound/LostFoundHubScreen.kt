package com.nestmate.app.ui.screens.lostandfound

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.nestmate.app.data.model.LostFoundCategory
import com.nestmate.app.data.model.LostFoundItem
import com.nestmate.app.data.model.LostFoundStatus
import com.nestmate.app.data.model.LostFoundType
import com.nestmate.app.ui.components.EmptyState
import com.nestmate.app.ui.components.NestMateTopBar
import com.nestmate.app.utils.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LostFoundHubScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPost: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: LostFoundViewModel = hiltViewModel()
) {
    val items by viewModel.feedItems.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val selectedType by viewModel.selectedType.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()

    val pullRefreshState = rememberPullToRefreshState()
    
    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.refreshFeed()
            pullRefreshState.endRefresh()
        }
    }

    val lostCount = items.count { it.type == LostFoundType.LOST }
    val foundCount = items.count { it.type == LostFoundType.FOUND }
    val resolvedCount = items.count { it.status == LostFoundStatus.RESOLVED }

    Scaffold(
        topBar = {
            NestMateTopBar(
                title = "Lost & Found",
                onBack = onNavigateBack,
                actions = {
                    IconButton(onClick = onNavigateToPost) {
                        Icon(Icons.Default.Add, contentDescription = "Post Item")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nestedScroll(pullRefreshState.nestedScrollConnection)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Filter Row
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedType == null,
                            onClick = { viewModel.setTypeFilter(null) },
                            label = { Text("All") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedType == LostFoundType.LOST,
                            onClick = { viewModel.setTypeFilter(LostFoundType.LOST) },
                            label = { Text("Lost") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedType == LostFoundType.FOUND,
                            onClick = { viewModel.setTypeFilter(LostFoundType.FOUND) },
                            label = { Text("Found") }
                        )
                    }
                    
                    if (selectedType != null) {
                        items(LostFoundCategory.values()) { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = {
                                    if (selectedCategory == category) viewModel.setCategoryFilter(null)
                                    else viewModel.setCategoryFilter(category)
                                },
                                label = { Text(category.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                }

                // Stats Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔴 $lostCount Lost", style = MaterialTheme.typography.labelSmall)
                    Text("🟢 $foundCount Found", style = MaterialTheme.typography.labelSmall)
                    Text("✓ $resolvedCount Resolved", style = MaterialTheme.typography.labelSmall)
                }

                // Feed
                if (items.isEmpty() && !isLoading) {
                    val emptyMsg = if (selectedType == LostFoundType.LOST) "No lost items reported yet" 
                                   else if (selectedType == LostFoundType.FOUND) "No found items posted yet"
                                   else "No items available"
                    EmptyState(title = "It's quiet here", message = emptyMsg)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(items) { item ->
                            LostFoundItemCard(
                                item = item,
                                onClick = { onNavigateToDetail(item.id) }
                            )
                        }
                    }
                }
            }
            
            PullToRefreshContainer(
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
fun LostFoundItemCard(item: LostFoundItem, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (item.photoUrls.isNotEmpty()) {
                    AsyncImage(
                        model = item.photoUrls.first(),
                        contentDescription = "Item Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = "Placeholder",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                if (item.status == LostFoundStatus.RESOLVED) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Resolved ✓",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Green,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val badgeColor = if (item.type == LostFoundType.LOST) Color(0xFFFFA000) else Color(0xFF388E3C)
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = badgeColor.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = item.type.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.category.name.replace("_", " "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val initial = item.reportedByName.take(1).uppercase()
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${item.reportedByName} • ${TimeUtils.formatTimeAgo(item.createdAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

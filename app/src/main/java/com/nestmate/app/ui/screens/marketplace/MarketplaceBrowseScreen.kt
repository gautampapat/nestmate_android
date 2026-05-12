package com.nestmate.app.ui.screens.marketplace

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.nestmate.app.data.model.ItemCategory
import com.nestmate.app.data.model.MarketplaceItem
import com.nestmate.app.data.model.WantedPost
import com.nestmate.app.ui.components.EmptyState
import com.nestmate.app.ui.components.GlassCard
import com.nestmate.app.ui.components.GlassSurface
import com.nestmate.app.ui.components.GlowButton
import com.nestmate.app.ui.components.NestMateTopBar
import com.nestmate.app.utils.CurrencyFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceBrowseScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToAddItem: () -> Unit,
    onNavigateToWantedCreate: () -> Unit,
    onNavigateToChats: () -> Unit,
    onNavigateToWantedChat: (String) -> Unit,
    onNavigateBack: () -> Unit,
) {
    val viewModel: MarketplaceViewModel = hiltViewModel()
    val activeListings by viewModel.activeListings.collectAsStateWithLifecycle()
    val trending by viewModel.trendingItems.collectAsStateWithLifecycle()
    val movingOut by viewModel.movingOutItems.collectAsStateWithLifecycle()
    val wanted by viewModel.wantedPosts.collectAsStateWithLifecycle()
    val savedIds by viewModel.savedItemIds.collectAsStateWithLifecycle()
    val belowAvg by viewModel.belowAverageIds.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(error) {
        error?.let {
            snackbar.showSnackbar(it)
            viewModel.clearError()
        }
    }

    var selectedTab by remember { mutableStateOf(0) }
    var searchText by remember { mutableStateOf(filters.searchQuery.orEmpty()) }

    GlassSurface {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            NestMateTopBar(
                title = "Marketplace",
                actions = {
                    IconButton(onClick = onNavigateToChats) {
                        Icon(Icons.Filled.Inbox, contentDescription = "Chats")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (selectedTab == 0) onNavigateToAddItem() else onNavigateToWantedCreate()
                },
                icon = { Icon(Icons.Filled.Add, contentDescription = "Icon") },
                text = { Text(if (selectedTab == 0) "Post Item" else "Post Wanted") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Browse") },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Wanted") },
                )
            }

            when (selectedTab) {
                0 -> BrowseTab(
                    searchText = searchText,
                    onSearchChange = {
                        searchText = it
                        viewModel.setSearchQuery(it)
                    },
                    selectedCategory = filters.category,
                    onCategoryChange = viewModel::setCategory,
                    onClearFilters = {
                        viewModel.clearFilters()
                        searchText = ""
                    },
                    isLoading = isLoading,
                    trending = trending,
                    movingOut = movingOut,
                    items = activeListings,
                    savedIds = savedIds,
                    belowAvg = belowAvg,
                    onItemClick = { id ->
                        if (currentUser?.isVerified == true) {
                            onNavigateToDetail(id)
                        } else {
                            scope.launch { snackbar.showSnackbar("Verify your college email to use this feature") }
                        }
                    },
                    onToggleSave = viewModel::toggleSave,
                )
                1 -> WantedTab(
                    posts = wanted,
                    currentUserId = viewModel.currentUserId,
                    onRespond = { post ->
                        if (currentUser?.isVerified == true) {
                            scope.launch {
                                viewModel.openWantedChat(post)
                                    .onSuccess { id -> onNavigateToWantedChat(id) }
                                    .onFailure { snackbar.showSnackbar(it.message ?: "Unable to respond") }
                            }
                        } else {
                            scope.launch { snackbar.showSnackbar("Verify your college email to use this feature") }
                        }
                    },
                )
            }
        }
    }
    }
}

@Composable
private fun BrowseTab(
    searchText: String,
    onSearchChange: (String) -> Unit,
    selectedCategory: ItemCategory?,
    onCategoryChange: (ItemCategory?) -> Unit,
    onClearFilters: () -> Unit,
    isLoading: Boolean,
    trending: List<MarketplaceItem>,
    movingOut: List<MarketplaceItem>,
    items: List<MarketplaceItem>,
    savedIds: Set<String>,
    belowAvg: Set<String>,
    onItemClick: (String) -> Unit,
    onToggleSave: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search listings") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Icon") },
            trailingIcon = {
                if (searchText.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onCategoryChange(null) },
                    label = { Text("All") },
                )
            }
            items(ItemCategory.values().toList()) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { onCategoryChange(if (selectedCategory == cat) null else cat) },
                    label = { Text(cat.label) },
                )
            }
            item {
                AssistChip(
                    onClick = onClearFilters,
                    label = { Text("Clear") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                )
            }
        }

        when {
            isLoading && items.isEmpty() && trending.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            items.isEmpty() && trending.isEmpty() && movingOut.isEmpty() -> {
                EmptyState(
                    title = "No listings yet",
                    message = "Be the first to post something for your college.",
                )
            }
            else -> BrowseList(
                trending = trending,
                movingOut = movingOut,
                items = items,
                savedIds = savedIds,
                belowAvg = belowAvg,
                onItemClick = onItemClick,
                onToggleSave = onToggleSave,
            )
        }
    }
}

@Composable
private fun BrowseList(
    trending: List<MarketplaceItem>,
    movingOut: List<MarketplaceItem>,
    items: List<MarketplaceItem>,
    savedIds: Set<String>,
    belowAvg: Set<String>,
    onItemClick: (String) -> Unit,
    onToggleSave: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (trending.isNotEmpty()) {
            item { SectionHeader("Trending") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(trending, key = { "trend-${it.id}" }) { item ->
                        TrendingCard(
                            item = item,
                            saved = item.id in savedIds,
                            onClick = { onItemClick(item.id) },
                            onToggleSave = { onToggleSave(item.id) },
                        )
                    }
                }
            }
        }

        if (movingOut.isNotEmpty()) {
            item { SectionHeader("Moving Out Sales") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(movingOut, key = { "mo-${it.id}" }) { item ->
                        TrendingCard(
                            item = item,
                            saved = item.id in savedIds,
                            onClick = { onItemClick(item.id) },
                            onToggleSave = { onToggleSave(item.id) },
                        )
                    }
                }
            }
        }

        item { SectionHeader("All Listings") }

        items(items.chunked(2), key = { pair -> pair.joinToString("|") { it.id } }) { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) {
                        ListingCard(
                            item = item,
                            saved = item.id in savedIds,
                            isBelowAverage = item.id in belowAvg,
                            onClick = { onItemClick(item.id) },
                            onToggleSave = { onToggleSave(item.id) },
                        )
                    }
                }
                if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun TrendingCard(
    item: MarketplaceItem,
    saved: Boolean,
    onClick: () -> Unit,
    onToggleSave: () -> Unit,
) {
    GlassCard(
        modifier = Modifier.width(180.dp),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            PhotoThumb(item.photoUrls.firstOrNull(), height = 156)
            Spacer(Modifier.height(8.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = CurrencyFormatter.formatPaise(item.price),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.category.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onToggleSave, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (saved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = if (saved) "Unsave" else "Save",
                        tint = if (saved) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ListingCard(
    item: MarketplaceItem,
    saved: Boolean,
    isBelowAverage: Boolean,
    onClick: () -> Unit,
    onToggleSave: () -> Unit,
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            PhotoThumb(item.photoUrls.firstOrNull(), height = 140)
            Spacer(Modifier.height(8.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = CurrencyFormatter.formatPaise(item.price),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onToggleSave, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (saved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = if (saved) "Unsave" else "Save",
                        tint = if (saved) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (item.isBundleListing) {
                Text(
                    text = "Bundle • ${item.bundleItemIds.size} items",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            if (isBelowAverage) {
                Text(
                    text = "Great price",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }
}

@Composable
private fun PhotoThumb(url: String?, height: Int) {
    val shape = RoundedCornerShape(12.dp)
    val placeholderColor = MaterialTheme.colorScheme.surface
    if (url.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height.dp)
                .clip(shape)
                .background(placeholderColor),
        )
    } else {
        AsyncImage(
            model = url,
            contentDescription = "Icon",
            modifier = Modifier
                .fillMaxWidth()
                .height(height.dp)
                .clip(shape),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun WantedTab(
    posts: List<WantedPost>,
    currentUserId: String?,
    onRespond: (WantedPost) -> Unit,
) {
    if (posts.isEmpty()) {
        EmptyState(
            title = "No wanted posts yet",
            message = "Looking for something specific? Post a Wanted request.",
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(posts, key = { it.id }) { post ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = post.itemDescription,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Max budget: ${CurrencyFormatter.formatPaise(post.maxBudget)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    post.category?.let {
                        Text(
                            text = "Category: ${it.label}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = "By ${post.buyerName.ifBlank { "Student" }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    if (currentUserId != null && currentUserId != post.buyerId) {
                        GlowButton(
                            text = "Respond",
                            onClick = { onRespond(post) },
                        )
                    }
                }
            }
        }
    }
}

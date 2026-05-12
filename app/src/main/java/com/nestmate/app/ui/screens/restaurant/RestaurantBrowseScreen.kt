package com.nestmate.app.ui.screens.restaurant

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.nestmate.app.data.model.Restaurant
import com.nestmate.app.ui.components.NestMateTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantBrowseScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: RestaurantViewModel = hiltViewModel(),
) {
    val filteredRestaurants by viewModel.filteredRestaurants.collectAsStateWithLifecycle()
    val trendingRestaurants by viewModel.trendingRestaurants.collectAsStateWithLifecycle()
    val budgetRestaurants by viewModel.budgetRestaurants.collectAsStateWithLifecycle()
    val promotedAndTrusted by viewModel.promotedAndTrusted.collectAsStateWithLifecycle()
    val imHungryPicks by viewModel.imHungryPicks.collectAsStateWithLifecycle()
    val savedIds by viewModel.savedRestaurantIds.collectAsStateWithLifecycle()
    val activeFilters by viewModel.activeFilters.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    var showImHungrySheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    val hasFilters = activeFilters != RestaurantFilters()

    Scaffold(
        topBar = {
            NestMateTopBar(
                title = { Text("Restaurants", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        BadgedBox(
                            badge = { if (hasFilters) Badge() }
                        ) {
                            Icon(Icons.Default.FilterList, "Filters")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isLoading && filteredRestaurants.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            // I'm Hungry button
            item {
                Button(
                    onClick = { showImHungrySheet = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("🍽️  I'm Hungry — Show me something!")
                }
            }

            // Active filter chips
            if (hasFilters) {
                item {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.clearFilters() },
                            label = { Text("Clear All ✕") }
                        )
                        activeFilters.foodType?.let {
                            FilterChip(selected = true, onClick = { viewModel.updateFilter { copy(foodType = null) } }, label = { Text(it) })
                        }
                        activeFilters.priceLevel?.let {
                            FilterChip(selected = true, onClick = { viewModel.updateFilter { copy(priceLevel = null) } }, label = { Text(it) })
                        }
                        activeFilters.category?.let {
                            FilterChip(selected = true, onClick = { viewModel.updateFilter { copy(category = null) } }, label = { Text(it) })
                        }
                    }
                }
            }

            // Quick Comparison strip
            if (filteredRestaurants.isNotEmpty()) {
                item {
                    Text(
                        "Quick Compare",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        items(filteredRestaurants.take(4)) { r ->
                            QuickCompareCard(r) { onNavigateToDetail(r.id) }
                        }
                    }
                }
            }

            // Trending section
            if (trendingRestaurants.isNotEmpty()) {
                item {
                    SectionHeader("Trending Near Campus 🔥")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        items(trendingRestaurants) { r ->
                            HorizontalRestaurantCard(r, r.id in savedIds, { viewModel.toggleSave(r.id) }) {
                                onNavigateToDetail(r.id)
                            }
                        }
                    }
                }
            }

            // Budget section
            if (budgetRestaurants.isNotEmpty()) {
                item {
                    SectionHeader("Easy on the Wallet 💸")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        items(budgetRestaurants) { r ->
                            HorizontalRestaurantCard(r, r.id in savedIds, { viewModel.toggleSave(r.id) }) {
                                onNavigateToDetail(r.id)
                            }
                        }
                    }
                }
            }

            // Promoted & Trusted section
            if (promotedAndTrusted.isNotEmpty()) {
                item {
                    SectionHeader("Trusted by NestMate ✓")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        items(promotedAndTrusted) { r ->
                            HorizontalRestaurantCard(r, r.id in savedIds, { viewModel.toggleSave(r.id) }) {
                                onNavigateToDetail(r.id)
                            }
                        }
                    }
                }
            }

            // Section header for main list
            item {
                SectionHeader("All Restaurants (${filteredRestaurants.size})")
            }

            // Main list
            if (filteredRestaurants.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🍽️", style = MaterialTheme.typography.displayMedium)
                            Spacer(Modifier.height(8.dp))
                            Text("No restaurants found", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = { viewModel.clearFilters() }) { Text("Clear filters") }
                        }
                    }
                }
            } else {
                items(filteredRestaurants) { r ->
                    RestaurantListCard(
                        restaurant = r,
                        isSaved = r.id in savedIds,
                        onSaveClick = { viewModel.toggleSave(r.id) },
                        onClick = { onNavigateToDetail(r.id) }
                    )
                }
            }
        }
    }

    // I'm Hungry Bottom Sheet
    if (showImHungrySheet) {
        ModalBottomSheet(onDismissRequest = { showImHungrySheet = false }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Top Picks for You 🍽️", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                imHungryPicks.forEach { r ->
                    RestaurantListCard(
                        restaurant = r,
                        isSaved = r.id in savedIds,
                        onSaveClick = { viewModel.toggleSave(r.id) },
                        onClick = { showImHungrySheet = false; onNavigateToDetail(r.id) }
                    )
                }
                TextButton(
                    onClick = { showImHungrySheet = false; viewModel.updateFilter { copy(priceLevel = "BUDGET") } },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("See all Budget Options →")
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Filter Bottom Sheet
    if (showFilterSheet) {
        FilterBottomSheet(
            current = activeFilters,
            onApply = { filters -> viewModel.updateFilter { filters } },
            onDismiss = { showFilterSheet = false },
            onFilterChange = { updateFunc -> viewModel.updateFilter(updateFunc) },
            onClearAll = { viewModel.clearFilters() }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun QuickCompareCard(restaurant: Restaurant, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(140.dp).clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(restaurant.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(priceLevelDots(restaurant.priceLevel), style = MaterialTheme.typography.bodySmall)
            Text("${restaurant.distanceKm} km away", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, null, Modifier.size(12.dp), tint = Color(0xFFFFA000))
                Text("${restaurant.overallRating}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun HorizontalRestaurantCard(
    restaurant: Restaurant,
    isSaved: Boolean,
    onSaveClick: () -> Unit,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.width(180.dp).clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box {
            if (restaurant.photoUrls.isNotEmpty()) {
                AsyncImage(
                    model = restaurant.photoUrls.first(),
                    contentDescription = restaurant.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                )
            } else {
                Box(Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)), contentAlignment = Alignment.Center) {
                    Text("🍽️", style = MaterialTheme.typography.headlineLarge)
                }
            }
            IconButton(onClick = onSaveClick, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(
                    if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    null, tint = if (isSaved) MaterialTheme.colorScheme.primary else Color.White
                )
            }
        }
        Column(modifier = Modifier.padding(10.dp)) {
            Text(restaurant.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(restaurant.category.replace("_", " "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, null, Modifier.size(12.dp), tint = Color(0xFFFFA000))
                Text(" ${restaurant.overallRating} · ${restaurant.distanceKm}km", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun RestaurantListCard(
    restaurant: Restaurant,
    isSaved: Boolean,
    onSaveClick: () -> Unit,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.height(100.dp)) {
            if (restaurant.photoUrls.isNotEmpty()) {
                AsyncImage(
                    model = restaurant.photoUrls.first(),
                    contentDescription = "Icon",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.width(100.dp).fillMaxHeight().clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                )
            } else {
                Box(
                    Modifier.width(100.dp).fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                        .then(Modifier.wrapContentSize(Alignment.Center)),
                    contentAlignment = Alignment.Center
                ) { Text("🍽️", style = MaterialTheme.typography.headlineMedium) }
            }
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(restaurant.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    IconButton(onClick = onSaveClick, modifier = Modifier.size(24.dp)) {
                        Icon(
                            if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            null, Modifier.size(18.dp),
                            tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    "${foodTypeEmoji(restaurant.foodType)} ${restaurant.category.replace("_", " ")} · ${priceLevelDots(restaurant.priceLevel)}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Star, null, Modifier.size(12.dp), tint = Color(0xFFFFA000))
                    Text("${restaurant.overallRating} (${restaurant.ratingCount})", style = MaterialTheme.typography.bodySmall)
                    Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${restaurant.distanceKm} km", style = MaterialTheme.typography.bodySmall)
                }
                if (restaurant.studentTags.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        restaurant.studentTags.take(2).forEach { tag ->
                            SuggestionChip(onClick = {}, label = { Text(tag.replace("_", " "), style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    current: RestaurantFilters,
    onApply: (RestaurantFilters) -> Unit,
    onDismiss: () -> Unit,
    onFilterChange: (RestaurantFilters.() -> RestaurantFilters) -> Unit,
    onClearAll: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("Filter Restaurants", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            Text("Food Type", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                listOf("VEG", "NON_VEG", "BOTH").forEach { type ->
                    FilterChip(
                        selected = current.foodType == type,
                        onClick = { onFilterChange { copy(foodType = if (current.foodType == type) null else type) } },
                        label = { Text(type.replace("_", " ")) }
                    )
                }
            }

            Text("Price Level", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                listOf("BUDGET" to "Budget", "MODERATE" to "Moderate", "PREMIUM" to "Premium").forEach { (value, label) ->
                    FilterChip(
                        selected = current.priceLevel == value,
                        onClick = { onFilterChange { copy(priceLevel = if (current.priceLevel == value) null else value) } },
                        label = { Text(label) }
                    )
                }
            }

            Text("Rating", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                FilterChip(
                    selected = current.minRating == 4f,
                    onClick = { onFilterChange { copy(minRating = if (current.minRating == 4f) null else 4f) } },
                    label = { Text("4+ ⭐") }
                )
                FilterChip(
                    selected = current.minRating == 4.5f,
                    onClick = { onFilterChange { copy(minRating = if (current.minRating == 4.5f) null else 4.5f) } },
                    label = { Text("4.5+ ⭐") }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = { onClearAll(); onDismiss() }, modifier = Modifier.weight(1f)) { Text("Clear All") }
                Button(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Apply") }
            }
        }
    }
}

private fun priceLevelDots(level: String) = when (level) {
    "BUDGET" -> "₹"
    "MODERATE" -> "₹₹"
    "PREMIUM" -> "₹₹₹"
    else -> "₹₹"
}

private fun foodTypeEmoji(type: String) = when (type) {
    "VEG" -> "🌱"
    "NON_VEG" -> "🍗"
    else -> "🍽️"
}

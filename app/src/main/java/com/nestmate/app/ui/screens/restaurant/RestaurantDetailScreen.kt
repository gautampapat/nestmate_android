package com.nestmate.app.ui.screens.restaurant

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.nestmate.app.data.model.Restaurant
import com.nestmate.app.ui.components.NestMateTopBar

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun RestaurantDetailScreen(
    restaurantId: String,
    onNavigateBack: () -> Unit,
    viewModel: RestaurantViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val allRestaurants by viewModel.allRestaurants.collectAsStateWithLifecycle()
    val savedIds by viewModel.savedRestaurantIds.collectAsStateWithLifecycle()
    val userRatingState by viewModel.getUserRating(restaurantId).collectAsStateWithLifecycle()

    val restaurant = allRestaurants.firstOrNull { it.id == restaurantId }

    // Log visit once
    LaunchedEffect(restaurantId) {
        viewModel.logVisit(restaurantId)
    }

    Scaffold(
        topBar = {
            NestMateTopBar(
                title = { Text(restaurant?.name ?: "Restaurant") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleSave(restaurantId) }) {
                        Icon(
                            if (restaurantId in savedIds) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            "Save",
                            tint = if (restaurantId in savedIds) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (restaurant == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Photo gallery
            item {
                if (restaurant.photoUrls.isNotEmpty()) {
                    val pagerState = rememberPagerState(pageCount = { restaurant.photoUrls.size })
                    HorizontalPager(state = pagerState) { page ->
                        AsyncImage(
                            model = restaurant.photoUrls[page],
                            contentDescription = "Icon",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(220.dp)
                        )
                    }
                } else {
                    Box(
                        Modifier.fillMaxWidth().height(180.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("🍽️", style = MaterialTheme.typography.displayLarge) }
                }
            }

            // Core info
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(restaurant.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        StarRatingDisplay(restaurant.overallRating, restaurant.ratingCount)
                        Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(priceLevelText(restaurant.priceLevel), style = MaterialTheme.typography.bodyMedium)
                        Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${restaurant.distanceKm} km away", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(8.dp))
                    if (restaurant.openingHours.isNotBlank()) {
                        Text("⏰ ${restaurant.openingHours}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            // Type/Category chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    item { AssistChip(onClick = {}, label = { Text(restaurant.foodType.replace("_", " ")) }) }
                    item { AssistChip(onClick = {}, label = { Text(restaurant.category.replace("_", " ")) }) }
                    restaurant.mealTimes.forEach { mealTime ->
                        item { AssistChip(onClick = {}, label = { Text(mealTime) }) }
                    }
                }
            }

            // Description
            if (restaurant.description.isNotBlank()) {
                item {
                    Text(restaurant.description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }

            // Student tags
            if (restaurant.studentTags.isNotEmpty()) {
                item {
                    Text("Great For", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        items(restaurant.studentTags) { tag ->
                            SuggestionChip(onClick = {}, label = { Text(tagToLabel(tag)) })
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }

            // Active discounts
            if (restaurant.activeDiscounts.isNotEmpty()) {
                item {
                    Text("Current Offers 🎉", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    restaurant.activeDiscounts.forEach { discount ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(discount["title"] ?: "", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                discount["validUntil"]?.let {
                                    Text("Valid until: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }

            // Google Maps card
            if (restaurant.googleMapsLink.isNotBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Map, null, tint = MaterialTheme.colorScheme.primary)
                                Text("View on Google Maps", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            }
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(restaurant.googleMapsLink))
                                    context.startActivity(intent)
                                }
                            ) { Text("Open") }
                        }
                    }
                }
            }

            // Rate this place
            item {
                var selectedRating by remember { mutableIntStateOf(userRatingState?.rating?.toInt() ?: 0) }
                var ratingSubmitted by remember { mutableStateOf(false) }

                LaunchedEffect(userRatingState) {
                    userRatingState?.rating?.let { selectedRating = it.toInt() }
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Rate This Place", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..5).forEach { star ->
                            IconButton(onClick = {
                                selectedRating = star
                                viewModel.submitRating(restaurantId, star.toFloat())
                                ratingSubmitted = true
                            }) {
                                Icon(
                                    if (star <= selectedRating) Icons.Default.Star else Icons.Default.StarBorder,
                                    null,
                                    tint = if (star <= selectedRating) Color(0xFFFFA000) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                    if (ratingSubmitted) {
                        Text("Thanks for rating! ✅", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun StarRatingDisplay(rating: Float, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Star, null, Modifier.size(16.dp), tint = Color(0xFFFFA000))
        Text(" ${"%.1f".format(rating)} ($count)", style = MaterialTheme.typography.bodyMedium)
    }
}

private fun priceLevelText(level: String) = when (level) {
    "BUDGET" -> "₹ Budget"
    "MODERATE" -> "₹₹ Moderate"
    "PREMIUM" -> "₹₹₹ Premium"
    else -> "₹₹ Moderate"
}

private fun tagToLabel(tag: String) = when (tag) {
    "BUDGET_FRIENDLY" -> "Budget Friendly"
    "GROUP_HANGOUT" -> "Group Hangout"
    "LATE_NIGHT" -> "Late Night"
    "QUICK_BITES" -> "Quick Bites"
    "STUDY_FRIENDLY" -> "Study Friendly"
    else -> tag.replace("_", " ")
}

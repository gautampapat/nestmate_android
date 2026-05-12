package com.nestmate.app.ui.screens.housing

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.nestmate.app.data.model.ProviderListing
import com.nestmate.app.ui.components.EmptyState
import com.nestmate.app.ui.components.GlassCard
import com.nestmate.app.ui.components.GlassSurface
import com.nestmate.app.ui.components.NestMateTopBar
import com.nestmate.app.ui.components.ShimmerCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HousingListScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToAddListing: () -> Unit,
    viewModel: HousingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    GlassSurface {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                NestMateTopBar(title = "Housing Deals")
            },
            floatingActionButton = {
                FloatingActionButton(onClick = onNavigateToAddListing, containerColor = MaterialTheme.colorScheme.primary) {
                    Icon(Icons.Default.Add, contentDescription = "Add Listing")
                }
            },
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                // Filter chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        FilterChip(selected = filterState.femaleOnly, onClick = { viewModel.toggleFemaleOnly() }, label = { Text("Female Only") })
                    }
                    item {
                        FilterChip(selected = filterState.bachelorFriendly, onClick = { viewModel.toggleBachelorFriendly() }, label = { Text("Bachelor Friendly") })
                    }
                }

                when (uiState) {
                    is HousingState.Loading -> Column(modifier = Modifier.padding(16.dp)) { repeat(5) { ShimmerCard() } }
                    is HousingState.Error -> EmptyState(
                        title = "Network Error",
                        message = (uiState as HousingState.Error).message,
                        buttonText = "Retry",
                        onButtonClick = {},
                    )
                    is HousingState.Success -> {
                        val listings = (uiState as HousingState.Success).listings
                        if (listings.isEmpty()) {
                            EmptyState(title = "No Listings", message = "No housing listings available yet.")
                        } else {
                            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(listings, key = { it.id }) { listing ->
                                    ProviderListingCard(listing = listing) { 
                                        if (currentUser?.isVerified == true) {
                                            onNavigateToDetail(listing.id)
                                        } else {
                                            scope.launch { snackbarHostState.showSnackbar("Verify your college email to use this feature") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderListingCard(listing: ProviderListing, onClick: () -> Unit) {
    GlassCard(onClick = onClick) {
        Column {
            // PHOTO SECTION — only if photos exist (no placeholder for housing)
            if (listing.photoUrls.isNotEmpty()) {
                AsyncImage(
                    model = listing.photoUrls.first(),
                    contentDescription = listing.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop,
                )
            }
            // Text content
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = listing.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    if (listing.isFemaleOnly) {
                        Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFFCE4EC)) {
                            Text(
                                "Girls Only",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFC2185B),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(listing.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (listing.rentPaise > 0) "₹${listing.rentRupees}/month" else "Rent not listed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (listing.bhkType.isNotBlank()) {
                        Spacer(Modifier.width(16.dp))
                        Text(listing.bhkType, style = MaterialTheme.typography.labelMedium)
                    }
                }
                if (listing.isVerifiedByAdmin) {
                    Spacer(Modifier.height(6.dp))
                    Text("✓ Verified Provider", style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold)
                }

                if (listing.realityScore > 0.0 || listing.greenScore > 0.0) {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (listing.realityScore > 0.0) {
                            val realityColor = when {
                                listing.realityScore >= 7.0 -> MaterialTheme.colorScheme.primaryContainer
                                listing.realityScore < 5.0 -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                            Surface(shape = RoundedCornerShape(8.dp), color = realityColor) {
                                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Verified, contentDescription = "Reality Score", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Reality ${listing.realityScore}/10", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        
                        if (listing.greenScore > 0.0) {
                            val greenColor = if (listing.greenScore >= 7.0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            Surface(shape = RoundedCornerShape(8.dp), color = greenColor) {
                                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Eco, contentDescription = "Green Score", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Green ${listing.greenScore}/10", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.nestmate.app.ui.screens.services

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nestmate.app.data.model.ProviderListing
import com.nestmate.app.ui.components.NestMatePrimaryButton
import com.nestmate.app.ui.components.NestMateTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServicesHubScreen(
    onNavigateToRideShare: () -> Unit,
    viewModel: ServicesViewModel = hiltViewModel()
) {
    val state by viewModel.servicesState.collectAsStateWithLifecycle()
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf(
        Category("All", Icons.Default.Apps),
        Category("Laundry", Icons.Default.LocalLaundryService),
        Category("Xerox", Icons.Default.ContentCopy),
        Category("Grocery", Icons.Default.ShoppingCart),
        Category("Tutor", Icons.Default.School),
        Category("Other", Icons.Default.Build),
    )

    Scaffold(
        topBar = {
            NestMateTopBar(
                title = { Text("Local Services", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Category bubbles (Zomato-style)
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(categories) { category ->
                    CategoryBubble(
                        item = category,
                        isSelected = selectedCategory == category.name,
                        onClick = { selectedCategory = category.name }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)

            // Dynamic Content depending on State
            if (state is ServicesState.Loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state is ServicesState.Success) {
                val list = (state as ServicesState.Success).services
                val categoryMap = mapOf(
                    "Laundry" to "LAUNDRY",
                    "Xerox" to "XEROX",
                    "Grocery" to "GROCERY",
                    "Tutor" to "TUTOR",
                    "Other" to "OTHER_SERVICE",
                )
                val filteredList = if (selectedCategory == "All") list
                    else list.filter { it.listingType.equals(categoryMap[selectedCategory] ?: selectedCategory, ignoreCase = true) }

                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        TransportBanner(onNavigateToRideShare)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Verified Providers", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                    }

                    if (filteredList.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                Text("No services found for $selectedCategory", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        items(filteredList, key = { it.id }) { service ->
                            ServiceCardModern(service)
                        }
                    }
                }
            }
        }
    }
}

data class Category(val name: String, val icon: ImageVector)

@Composable
fun CategoryBubble(item: Category, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val iconTint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(item.icon, contentDescription = item.name, tint = iconTint, modifier = Modifier.size(28.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = item.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor
        )
    }
}

@Composable
fun TransportBanner(onNavigateToRideShare: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        onClick = onNavigateToRideShare
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.DirectionsCar, contentDescription = "Icon", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Going Home?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text("Find a ride buddy and split travel costs.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "Icon", tint = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun ServiceCardModern(service: ProviderListing) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = when (service.listingType) {
                        "LAUNDRY" -> Icons.Default.LocalLaundryService
                        "XEROX" -> Icons.Default.ContentCopy
                        "GROCERY" -> Icons.Default.ShoppingCart
                        "TUTOR" -> Icons.Default.School
                        else -> Icons.Default.Build
                    }
                    Icon(icon, contentDescription = "Icon", tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = service.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (service.timings.isNotBlank()) {
                        Text(text = service.timings, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (service.priceDescription.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Column {
                    Text(text = "Pricing", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = service.priceDescription, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (service.googleMapsLink.isNotBlank()) {
                    OutlinedButton(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(service.googleMapsLink))) },
                        modifier = Modifier.weight(1f)
                    ) { Text("Maps") }
                }
            }
        }
    }
}

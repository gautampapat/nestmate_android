package com.nestmate.app.ui.screens.provider

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.MiscellaneousServices
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.nestmate.app.data.model.Inquiry
import com.nestmate.app.data.model.InquiryStatus
import com.nestmate.app.data.model.ListingStatus
import com.nestmate.app.data.model.ProviderListing
import com.nestmate.app.ui.components.EmptyState
import com.nestmate.app.ui.components.StatCard
import com.nestmate.app.utils.TimeUtils
import com.nestmate.app.ui.components.NestMateTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderDashboardScreen(
    onSignOut: () -> Unit,
    onNavigateToAddListing: (type: String) -> Unit,
    onNavigateToEditListing: (listingId: String) -> Unit,
    onNavigateToInquiries: () -> Unit,
    onNavigateToChat: (inquiryId: String) -> Unit,
    onNavigateToVerification: () -> Unit = {},
    onNavigateToSplash: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    viewModel: ProviderDashboardViewModel = hiltViewModel(),
) {
    var selectedItem by remember { mutableStateOf(0) }
    
    val navItems = listOf(
        Triple("Dashboard", Icons.Default.Home, null),
        Triple("Profile", Icons.Default.Person, null)
    )

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(item.second, contentDescription = item.first) },
                        label = { Text(item.first) },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (selectedItem == 0) {
                ProviderDashboardView(
                    viewModel = viewModel,
                    onSignOut = onSignOut,
                    onNavigateToAddListing = onNavigateToAddListing,
                    onNavigateToEditListing = onNavigateToEditListing,
                    onNavigateToInquiries = onNavigateToInquiries,
                    onNavigateToChat = onNavigateToChat,
                    onNavigateToVerification = onNavigateToVerification
                )
            } else if (selectedItem == 1) {
                com.nestmate.app.ui.screens.profile.ProfileScreen(
                    onNavigateToSplash = onNavigateToSplash,
                    onNavigateToEditProfile = onNavigateToEditProfile,
                    onNavigateToVerification = onNavigateToVerification
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderDashboardView(
    viewModel: ProviderDashboardViewModel,
    onSignOut: () -> Unit,
    onNavigateToAddListing: (type: String) -> Unit,
    onNavigateToEditListing: (listingId: String) -> Unit,
    onNavigateToInquiries: () -> Unit,
    onNavigateToChat: (inquiryId: String) -> Unit,
    onNavigateToVerification: () -> Unit,
) {
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val listings by viewModel.myListings.collectAsStateWithLifecycle()
    val activeCount by viewModel.activeListingsCount.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadInquiriesCount.collectAsStateWithLifecycle()
    val pendingInquiries by viewModel.pendingInquiriesCount.collectAsStateWithLifecycle()
    val totalViews by viewModel.totalViewsThisWeek.collectAsStateWithLifecycle()
    val avgResponse by viewModel.avgResponseTimeHours.collectAsStateWithLifecycle()
    val healthIssues by viewModel.listingHealthIssues.collectAsStateWithLifecycle()
    val recentInquiries by viewModel.recentInquiries.collectAsStateWithLifecycle()
    val verStatus by viewModel.verificationStatus.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.snackbarEvent.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            NestMateTopBar(
                title = { Text("Provider Dashboard", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                actions = {
                    BadgedBox(badge = {
                        if (unreadCount > 0) Badge { Text("$unreadCount") }
                    }) {
                        IconButton(onClick = onNavigateToInquiries) {
                            Icon(Icons.Default.Inbox, contentDescription = "Inquiries")
                        }
                    }
                    IconButton(onClick = {
                        viewModel.signOut()
                        onSignOut()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign Out")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToAddListing("FLAT_PG_HOSTEL") },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Add Listing") },
                containerColor = MaterialTheme.colorScheme.primary,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Welcome header
            item {
                Text(
                    text = "Welcome, $userName",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (verStatus == "idle" || verStatus == "Rejected") {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onNavigateToVerification() },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = "Verify", tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (verStatus == "Rejected") "Verification Rejected! Please re-verify to unlock all features." else "Verify your provider account to unlock all features.",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Text("Verify Now →", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            } else if (verStatus == "Submitted") {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = "Pending", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(12.dp))
                            Text("Verification Pending. We'll notify you once approved.", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // ── Section A — Quick Stats Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    StatCard("Active", "$activeCount", Icons.Default.Home, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    StatCard("Pending", "$pendingInquiries", Icons.Default.Inbox, badgeCount = unreadCount, modifier = Modifier.weight(1f), onClick = onNavigateToInquiries)
                    Spacer(Modifier.width(8.dp))
                    StatCard("Views", "$totalViews", Icons.AutoMirrored.Filled.TrendingUp, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    StatCard("Avg Resp", avgResponse, Icons.Default.Schedule, modifier = Modifier.weight(1f))
                }
            }

            // ── Section B — Action Required
            if (healthIssues.isNotEmpty()) {
                item {
                    Text("Action Required 🔧", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(healthIssues) { issue ->
                            val label = when(issue.issueType) {
                                IssueType.NO_PHOTOS -> "Add photos"
                                IssueType.NO_PRICE_SET -> "Set price"
                                IssueType.PAUSED_TOO_LONG -> "Reactivate listing"
                                IssueType.LOW_INQUIRY_RATE -> "Boost visibility"
                            }
                            SuggestionChip(
                                onClick = { onNavigateToEditListing(issue.listingId) },
                                label = { Text("${issue.listingTitle} — $label") },
                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                border = null
                            )
                        }
                    }
                }
            }

            // ── Section C — Recent Inquiries
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent Inquiries", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = onNavigateToInquiries) {
                        Text("View All")
                    }
                }
            }

            if (recentInquiries.isEmpty()) {
                item {
                    EmptyState(
                        title = "No inquiries yet",
                        message = "Your first tenant is just around the corner!"
                    )
                }
            } else {
                items(recentInquiries, key = { "recent_${it.id}" }) { inquiry ->
                    RecentInquiryCard(
                        inquiry = inquiry,
                        onNavigateToChat = { onNavigateToChat(inquiry.id) }
                    )
                }
            }

            // ── Section D — My Listings
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("My Listings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { onNavigateToAddListing("FLAT_PG_HOSTEL") }) {
                        Text("Add New")
                    }
                }
            }

            if (listings.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            "No listings yet. Tap Add New to add your first listing.",
                            modifier = Modifier.padding(20.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(listings, key = { it.id }) { listing ->
                    ProviderListingManageCard(
                        listing = listing,
                        onEdit = { onNavigateToEditListing(listing.id) },
                        onTogglePause = { viewModel.togglePause(listing.id, listing.status) },
                        onDelete = { viewModel.deleteListing(listing.id) },
                    )
                }
            }

            // ── Section E — Tips & Checklist
            item {
                TipsAndChecklistCard()
            }
            
            item { Spacer(Modifier.height(80.dp)) } // padding for FAB
        }
    }
}

// ── Components ───────────────────────────────────────────────────────────────

@Composable
private fun RecentInquiryCard(
    inquiry: Inquiry,
    onNavigateToChat: () -> Unit
) {
    val timeStr: String = if (inquiry.createdAt > 0L) {
        TimeUtils.formatTimeAgo(inquiry.createdAt)
    } else "—"

    OutlinedCard(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = inquiry.studentName.ifBlank { "Student" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
            SuggestionChip(
                onClick = {},
                label = { Text(inquiry.listingTitle.ifBlank { "Listing" }, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.height(24.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (inquiry.message.length > 60) inquiry.message.take(60) + "..." else inquiry.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (statusText, statusColor) = when (inquiry.status) {
                    InquiryStatus.UNREAD.name -> "PENDING" to Color(0xFFFF8F00) // Amber
                    InquiryStatus.READ.name -> "PENDING" to Color(0xFFFF8F00) // Treat read but not responded as pending
                    InquiryStatus.RESPONDED.name -> "RESPONDED" to Color(0xFF2E7D32) // Green
                    else -> "CLOSED" to Color.Gray
                }
                
                SuggestionChip(
                    onClick = {},
                    label = { Text(statusText, color = statusColor, style = MaterialTheme.typography.labelSmall) },
                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = statusColor.copy(alpha = 0.1f)),
                    border = null
                )
                
                if (inquiry.status == InquiryStatus.UNREAD.name || inquiry.status == InquiryStatus.READ.name) {
                    FilledTonalButton(onClick = onNavigateToChat) {
                        Text("Reply")
                    }
                } else {
                    TextButton(onClick = onNavigateToChat) {
                        Text("View Chat")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderListingManageCard(
    listing: ProviderListing,
    onEdit: () -> Unit,
    onTogglePause: () -> Unit,
    onDelete: () -> Unit,
) {
    val statusColor = when (listing.status) {
        ListingStatus.ACTIVE.name -> Color(0xFF2E7D32)
        ListingStatus.PAUSED.name -> Color.Gray
        else -> Color(0xFFFF8F00) // Pending approval fallback
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Thumbnail
                if (listing.photoUrls.isNotEmpty()) {
                    AsyncImage(
                        model = listing.firstPhotoUrl,
                        contentDescription = "Listing thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(64.dp).padding(4.dp),
                    )
                } else {
                    Box(
                        modifier = Modifier.size(64.dp).padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "No photos", tint = Color.Gray)
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        listing.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).padding(end = 4.dp)) {
                            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(color = statusColor)
                            }
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = listing.status,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor
                        )
                    }
                }

                // Actions
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onTogglePause, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (listing.isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                            null,
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFFFF8F00),
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "👁 ${listing.viewCount} views",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "💬 ${listing.inquiryCount} inquiries",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TipsAndChecklistCard() {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        onClick = { isExpanded = !isExpanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Listing Tips 💡",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Toggle tips",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            if (isExpanded) {
                Spacer(Modifier.height(16.dp))
                TipRow("Listings with 3+ photos get 4× more inquiries")
                TipRow("Respond to inquiries within 2 hours for better ranking")
                TipRow("Keep your price updated to show in search results")
                TipRow("Enable WhatsApp contact for faster responses")
            }
        }
    }
}

@Composable
private fun TipRow(text: String) {
    Row(
        modifier = Modifier.padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Check",
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

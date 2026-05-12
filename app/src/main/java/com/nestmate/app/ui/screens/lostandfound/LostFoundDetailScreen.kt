package com.nestmate.app.ui.screens.lostandfound

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.nestmate.app.data.model.LostFoundStatus
import com.nestmate.app.data.model.LostFoundType
import com.nestmate.app.ui.components.GlassCard
import com.nestmate.app.ui.components.GlowButton
import com.nestmate.app.ui.components.NestMateTopBar
import com.nestmate.app.utils.TimeUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LostFoundDetailScreen(
    itemId: String,
    onNavigateBack: () -> Unit,
    viewModel: LostFoundViewModel = hiltViewModel()
) {
    val items by viewModel.feedItems.collectAsStateWithLifecycle()
    val myItems by viewModel.myItems.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    
    // Check both feed items and my items
    val item = items.find { it.id == itemId } ?: myItems.find { it.id == itemId }
    
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    var showClaimSheet by remember { mutableStateOf(false) }
    var showResolveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(itemId) {
        viewModel.incrementViewCount(itemId)
    }

    Scaffold(
        topBar = {
            NestMateTopBar(
                title = "Item Details",
                onBack = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (item == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Photo Carousel
                if (item.photoUrls.isNotEmpty()) {
                    val pagerState = rememberPagerState(pageCount = { item.photoUrls.size })
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(20.dp))
                    ) { page ->
                        AsyncImage(
                            model = item.photoUrls[page],
                            contentDescription = "Item Photo $page",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    // Dot indicators
                    if (item.photoUrls.size > 1) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(item.photoUrls.size) { iteration ->
                                val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                Box(
                                    modifier = Modifier
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .size(8.dp)
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = "No Photo",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            item {
                // Header section
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val badgeColor = if (item.type == LostFoundType.LOST) Color(0xFFFFA000) else Color(0xFF388E3C)
                    Surface(shape = RoundedCornerShape(4.dp), color = badgeColor.copy(alpha = 0.1f)) {
                        Text(
                            text = item.type.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    if (item.status == LostFoundStatus.RESOLVED) {
                        Surface(shape = RoundedCornerShape(4.dp), color = Color.Green.copy(alpha = 0.1f)) {
                            Text(
                                text = "RESOLVED",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Green,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text(
                            text = item.category.name.replace("_", " "),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text(
                            text = item.location,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            item {
                // Details Card
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Description", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(item.description, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val locationPrefix = if (item.type == LostFoundType.LOST) "Last seen at" else "Found at"
                        Text("$locationPrefix: ${item.location}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Date Reported: ${TimeUtils.formatTimeAgo(item.createdAt)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                // Reporter info row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.reportedByPhotoUrl != null) {
                        AsyncImage(
                            model = item.reportedByPhotoUrl,
                            contentDescription = "Reporter Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.reportedByName.take(1).uppercase(),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(item.reportedByName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text("Posted ${TimeUtils.formatTimeAgo(item.createdAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                if (item.status == LostFoundStatus.OPEN) {
                    if (currentUserId != null && currentUserId != item.reportedByUserId) {
                        // Action Section for viewers
                        if (item.type == LostFoundType.FOUND) {
                            GlowButton(
                                text = "This is mine — Claim it!",
                                onClick = { showClaimSheet = true },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            GlowButton(
                                text = "I found this!",
                                onClick = { showClaimSheet = true },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else if (currentUserId == item.reportedByUserId) {
                        // Owner Actions
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { showResolveDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Mark as Resolved")
                            }
                            TextButton(
                                onClick = { showDeleteDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Delete Post", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (showClaimSheet) {
            ClaimBottomSheet(
                posterName = item.reportedByName,
                onDismiss = { showClaimSheet = false },
                onSubmit = { message ->
                    viewModel.submitClaim(item.id, message)
                    showClaimSheet = false
                    scope.launch { snackbarHostState.showSnackbar("Your message has been sent!") }
                }
            )
        }

        if (showResolveDialog) {
            AlertDialog(
                onDismissRequest = { showResolveDialog = false },
                title = { Text("Resolve Item") },
                text = { Text("Are you sure you want to mark this item as resolved? This will hide it from the active feed.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.markResolved(item.id)
                        showResolveDialog = false
                    }) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResolveDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Post") },
                text = { Text("Are you sure you want to delete this post? This cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteItem(item.id)
                        showDeleteDialog = false
                        onNavigateBack()
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (errorMessage != null) {
            LaunchedEffect(errorMessage) {
                snackbarHostState.showSnackbar(errorMessage!!)
                viewModel.clearError()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClaimBottomSheet(
    posterName: String,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var message by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Send a message to $posterName",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = message,
                onValueChange = { if (it.length <= 200) message = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Tell them why this is yours / where you found it") },
                maxLines = 4,
                supportingText = { Text("${message.length}/200") }
            )
            Spacer(modifier = Modifier.height(24.dp))
            GlowButton(
                text = "Send Claim",
                onClick = { onSubmit(message) },
                modifier = Modifier.fillMaxWidth(),
                enabled = message.isNotBlank()
            )
        }
    }
}

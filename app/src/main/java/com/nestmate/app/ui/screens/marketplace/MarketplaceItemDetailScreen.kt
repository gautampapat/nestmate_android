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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.nestmate.app.data.model.ItemStatus
import com.nestmate.app.data.model.MarketplaceItem
import com.nestmate.app.ui.components.NestMateCard
import com.nestmate.app.ui.components.NestMatePrimaryButton
import com.nestmate.app.utils.CurrencyFormatter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import com.nestmate.app.ui.components.NestMateTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceItemDetailScreen(
    itemId: String,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToEdit: (String) -> Unit,
) {
    val viewModel: MarketplaceViewModel = hiltViewModel()
    val savedIds by viewModel.savedItemIds.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var item by remember { mutableStateOf<MarketplaceItem?>(null) }
    var bundleChildren by remember { mutableStateOf<List<MarketplaceItem>>(emptyList()) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(itemId) {
        viewModel.incrementView(itemId)
        viewModel.getListing(itemId).collectLatest { item = it }
    }

    LaunchedEffect(item?.bundleItemIds) {
        val ids = item?.bundleItemIds.orEmpty()
        if (ids.isNotEmpty()) {
            viewModel.getItemsByIds(ids).collectLatest { bundleChildren = it }
        } else {
            bundleChildren = emptyList()
        }
    }

    LaunchedEffect(error) {
        error?.let { snackbar.showSnackbar(it); viewModel.clearError() }
    }

    val isOwner by remember(item, viewModel.currentUserId) {
        derivedStateOf<Boolean> { item != null && item?.sellerId == viewModel.currentUserId }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            NestMateTopBar(
                title = { Text(item?.title.orEmpty(), maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isOwner) {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = {
                                    menuOpen = false
                                    onNavigateToEdit(itemId)
                                },
                                leadingIcon = { Icon(Icons.Filled.Edit, null) },
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = { menuOpen = false; showDeleteDialog = true },
                                leadingIcon = { Icon(Icons.Filled.Delete, null) },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            val current = item ?: return@Scaffold
            BottomBar(
                item = current,
                isOwner = isOwner,
                saved = current.id in savedIds,
                onToggleSave = { viewModel.toggleSave(current.id) },
                onContactSeller = {
                    scope.launch {
                        viewModel.openItemChat(current)
                            .onSuccess { chatId -> onNavigateToChat(chatId) }
                            .onFailure { snackbar.showSnackbar(it.message ?: "Couldn't start chat") }
                    }
                },
                onMarkSold = {
                    viewModel.markSold(current.id) { ok ->
                        scope.launch {
                            snackbar.showSnackbar(
                                if (ok) "Marked as sold" else "Could not mark sold",
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        val current = item
        if (current == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Loading…", color = MaterialTheme.colorScheme.onBackground)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            PhotoGallery(current.photoUrls)
            Spacer(Modifier.height(16.dp))
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = current.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = CurrencyFormatter.formatPaise(current.price) +
                        if (current.isNegotiable) "  •  Negotiable" else "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AssistChip(onClick = {}, label = { Text(current.category.label) })
                    AssistChip(onClick = {}, label = { Text(current.condition.label) })
                    if (current.status == ItemStatus.SOLD) {
                        AssistChip(onClick = {}, label = { Text("SOLD") })
                    }
                }
                current.tags.forEach { tag ->
                    Text(
                        text = tag.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Seller: ${current.sellerName.ifBlank { "Student" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = current.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                if (current.isBundleListing) {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = "Bundle contents (${bundleChildren.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    current.bundleExpiryDate?.let { expiryMs ->
                        val remainingMs = expiryMs - System.currentTimeMillis()
                        val remainingDays = TimeUnit.MILLISECONDS.toDays(remainingMs)
                        val label = when {
                            remainingDays < 0 -> "Expired"
                            remainingDays == 0L -> "Ends today"
                            else -> "Ends in $remainingDays day${if (remainingDays == 1L) "" else "s"}"
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    bundleChildren.forEach { child ->
                        NestMateCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AsyncImage(
                                    model = child.photoUrls.firstOrNull(),
                                    contentDescription = "Icon",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surface),
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = child.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatPaise(child.price),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Views: ${current.viewCount}  •  Saves: ${current.saveCount}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(40.dp))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete listing?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteListing(itemId) { ok ->
                        if (ok) onNavigateBack()
                    }
                }) { Text("Delete") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PhotoGallery(urls: List<String>) {
    if (urls.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(MaterialTheme.colorScheme.surface),
        )
        return
    }
    val pagerState = rememberPagerState { urls.size }
    Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            AsyncImage(
                model = urls[page],
                contentDescription = "Icon",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (urls.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(urls.size) { i ->
                    val isActive = pagerState.currentPage == i
                    Box(
                        modifier = Modifier
                            .size(if (isActive) 9.dp else 7.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomBar(
    item: MarketplaceItem,
    isOwner: Boolean,
    saved: Boolean,
    onToggleSave: () -> Unit,
    onContactSeller: () -> Unit,
    onMarkSold: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!isOwner) {
                IconButton(
                    onClick = onToggleSave,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = if (saved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = if (saved) "Unsave" else "Save",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                NestMatePrimaryButton(
                    text = "Contact Seller",
                    onClick = onContactSeller,
                    modifier = Modifier.weight(1f),
                    enabled = item.status != ItemStatus.SOLD,
                )
            } else {
                if (item.status != ItemStatus.SOLD) {
                    NestMatePrimaryButton(
                        text = "Mark as Sold",
                        onClick = onMarkSold,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Text(
                        "This listing is sold.",
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

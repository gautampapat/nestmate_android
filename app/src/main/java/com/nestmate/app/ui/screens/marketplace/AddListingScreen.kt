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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.nestmate.app.data.model.ItemCategory
import com.nestmate.app.data.model.ItemCondition
import com.nestmate.app.data.model.ItemTag
import com.nestmate.app.data.model.MarketplaceItem
import com.nestmate.app.ui.components.NestMatePrimaryButton
import com.nestmate.app.utils.imageupload.rememberImagePicker
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.nestmate.app.ui.components.NestMateTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddListingScreen(
    editItemId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToBundleCreate: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: MarketplaceViewModel = hiltViewModel()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var existingItem by remember { mutableStateOf<MarketplaceItem?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priceRupees by remember { mutableStateOf("") }
    var isNegotiable by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf(ItemCategory.MISCELLANEOUS) }
    var condition by remember { mutableStateOf(ItemCondition.USED) }
    val tags = remember { mutableStateListOf<ItemTag>() }
    val photoUris = remember { mutableStateListOf<android.net.Uri>() }

    LaunchedEffect(editItemId) {
        if (editItemId == null) return@LaunchedEffect
        viewModel.getListing(editItemId).collectLatest { item ->
            if (item != null && existingItem == null) {
                existingItem = item
                title = item.title
                description = item.description
                priceRupees = (item.price / 100L).toString()
                isNegotiable = item.isNegotiable
                category = item.category
                condition = item.condition
                tags.clear(); tags.addAll(item.tags)
            }
        }
    }

    val remainingSlots = (4 - photoUris.size).coerceAtLeast(1)
    val picker = rememberImagePicker(
        maxCount = remainingSlots,
        onPicked = { uris ->
            uris.take(4 - photoUris.size).forEach { photoUris.add(it) }
        },
    )

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(error) {
        error?.let { snackbar.showSnackbar(it); viewModel.clearError() }
    }

    val isEditMode = editItemId != null

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            NestMateTopBar(
                title = { Text(if (isEditMode) "Edit Listing" else "Post Item") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionLabel(if (isEditMode) "Photos (existing)" else "Photos")
            if (isEditMode && photoUris.isEmpty()) {
                Text(
                    text = "Pick photos to replace; leave empty to keep current photos.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PhotoPickerRow(
                photoUris = photoUris,
                onRemove = { idx -> photoUris.removeAt(idx) },
                onAdd = { picker.launch() },
                maxCount = 4,
            )

            SectionLabel("Details")
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
            )

            SectionLabel("Category")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ItemCategory.values().toList()) { cat ->
                    FilterChip(
                        selected = cat == category,
                        onClick = { category = cat },
                        label = { Text(cat.label) },
                    )
                }
            }

            SectionLabel("Condition")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ItemCondition.values().forEach { c ->
                    FilterChip(
                        selected = c == condition,
                        onClick = { condition = c },
                        label = { Text(c.label) },
                    )
                }
            }

            SectionLabel("Pricing")
            OutlinedTextField(
                value = priceRupees,
                onValueChange = { priceRupees = it.filter { ch -> ch.isDigit() } },
                label = { Text("Price (Rs)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                ),
                singleLine = true,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = isNegotiable, onCheckedChange = { isNegotiable = it })
                Spacer(Modifier.width(12.dp))
                Text("Price is negotiable", color = MaterialTheme.colorScheme.onBackground)
            }

            SectionLabel("Sale tags")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ItemTag.values().forEach { tag ->
                    FilterChip(
                        selected = tag in tags,
                        onClick = {
                            if (tag in tags) tags.remove(tag) else tags.add(tag)
                        },
                        label = { Text(tag.label) },
                    )
                }
            }

            if (ItemTag.MOVING_OUT_SALE in tags && !isEditMode) {
                Text(
                    text = "Bundle multiple items together for a single sale.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                com.nestmate.app.ui.components.NestMateOutlinedButton(
                    text = "Create a Moving-Out Bundle",
                    onClick = onNavigateToBundleCreate,
                )
            }

            Spacer(Modifier.height(8.dp))
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                NestMatePrimaryButton(
                    text = if (isEditMode) "Save Changes" else "Post Listing",
                    onClick = submit@{
                        val priceRs = priceRupees.toLongOrNull() ?: 0L
                        if (title.isBlank() || description.isBlank() || priceRs <= 0) {
                            scope.launch { snackbar.showSnackbar("Fill title, description and price") }
                            return@submit
                        }
                        if (!isEditMode && photoUris.isEmpty()) {
                            scope.launch { snackbar.showSnackbar("Add at least one photo") }
                            return@submit
                        }
                        val input = MarketplaceViewModel.ListingInput(
                            title = title.trim(),
                            description = description.trim(),
                            pricePaise = priceRs * 100L,
                            isNegotiable = isNegotiable,
                            category = category,
                            condition = condition,
                            tags = tags.toList(),
                        )
                        if (isEditMode) {
                            val current = existingItem ?: return@submit
                            viewModel.updateListing(
                                context = context,
                                existing = current,
                                input = input,
                                newPhotoUris = photoUris.toList(),
                            ) { result ->
                                result.onSuccess { onNavigateBack() }
                            }
                        } else {
                            viewModel.createListing(
                                context = context,
                                input = input,
                                photoUris = photoUris.toList(),
                            ) { result ->
                                result.onSuccess { onNavigateBack() }
                            }
                        }
                    },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun PhotoPickerRow(
    photoUris: List<android.net.Uri>,
    onRemove: (Int) -> Unit,
    onAdd: () -> Unit,
    maxCount: Int,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().height(104.dp),
    ) {
        itemsIndexed(photoUris) { index, uri ->
            Box {
                AsyncImage(
                    model = uri,
                    contentDescription = "Icon",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
                IconButton(
                    onClick = { onRemove(index) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Remove",
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        if (photoUris.size < maxCount) {
            item {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    IconButton(onClick = onAdd, modifier = Modifier.size(56.dp)) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Add photo",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}


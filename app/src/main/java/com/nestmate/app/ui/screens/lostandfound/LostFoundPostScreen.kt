package com.nestmate.app.ui.screens.lostandfound

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.nestmate.app.data.model.LostFoundCategory
import com.nestmate.app.data.model.LostFoundType
import com.nestmate.app.ui.components.GlowButton
import com.nestmate.app.ui.components.NestMateTopBar
import com.nestmate.app.utils.imageupload.rememberImagePicker
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LostFoundPostScreen(
    onNavigateBack: () -> Unit,
    viewModel: LostFoundViewModel = hiltViewModel()
) {
    var selectedType by remember { mutableStateOf<LostFoundType?>(null) }
    var selectedCategory by remember { mutableStateOf<LostFoundCategory?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var contactPreference by remember { mutableStateOf("IN_APP") }
    var contactDetail by remember { mutableStateOf("") }
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val postingState by viewModel.postingState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val picker = rememberImagePicker(maxCount = 3 - imageUris.size, onPicked = { uris ->
        if (uris.isNotEmpty()) {
            val newUris = (imageUris + uris).take(3)
            imageUris = newUris
        }
    })

    LaunchedEffect(postingState) {
        if (postingState is PostingState.Success) {
            val msg = "Your ${selectedType?.name?.lowercase() ?: "item"} has been posted!"
            scope.launch { snackbarHostState.showSnackbar(msg) }
            viewModel.resetPostingState()
            onNavigateBack()
        } else if (postingState is PostingState.Error) {
            val msg = (postingState as PostingState.Error).message
            scope.launch { snackbarHostState.showSnackbar(msg) }
            viewModel.resetPostingState()
        }
    }

    val isFormValid = selectedType != null && selectedCategory != null && title.isNotBlank() && location.isNotBlank()

    Scaffold(
        topBar = {
            NestMateTopBar(
                title = "Post Item",
                onBack = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Type Selector
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { selectedType = LostFoundType.LOST },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedType == LostFoundType.LOST) Color(0xFFFFA000) else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selectedType == LostFoundType.LOST) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("I Lost Something")
                }
                Button(
                    onClick = { selectedType = LostFoundType.FOUND },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedType == LostFoundType.FOUND) Color(0xFF388E3C) else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selectedType == LostFoundType.FOUND) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("I Found Something")
                }
            }

            // Category Selector
            Text("Category", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(LostFoundCategory.values()) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 80) title = it },
                label = { Text("What is the item? (e.g. Blue backpack)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = title.length > 80,
                supportingText = { Text("${title.length}/80") }
            )

            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= 500) description = it },
                label = { Text("Describe it — colour, brand, marks") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4,
                isError = description.length > 500,
                supportingText = { Text("${description.length}/500") }
            )

            OutlinedTextField(
                value = location,
                onValueChange = { if (it.length <= 100) location = it },
                label = { Text("Where was it lost/found? (e.g. Library 2nd floor)") },
                modifier = Modifier.fillMaxWidth(),
                isError = location.length > 100,
                supportingText = { Text("${location.length}/100") }
            )

            // Photos
            Text("Photos (Max 3)", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                imageUris.forEach { uri ->
                    Box(modifier = Modifier.size(80.dp)) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Selected image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .clickable { imageUris = imageUris.filter { it != uri } },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                
                if (imageUris.size < 3) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { picker.launch() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = "Add Photo", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Contact Preference
            Text("Contact Preference", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = contactPreference == "IN_APP", onClick = { contactPreference = "IN_APP" })
                Text("In-App Message")
                Spacer(modifier = Modifier.width(8.dp))
                RadioButton(selected = contactPreference == "PHONE", onClick = { contactPreference = "PHONE" })
                Text("Phone")
                Spacer(modifier = Modifier.width(8.dp))
                RadioButton(selected = contactPreference == "EMAIL", onClick = { contactPreference = "EMAIL" })
                Text("Email")
            }

            if (contactPreference == "PHONE" || contactPreference == "EMAIL") {
                OutlinedTextField(
                    value = contactDetail,
                    onValueChange = { contactDetail = it },
                    label = { Text(if (contactPreference == "PHONE") "Phone Number" else "Email Address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Post Button
            if (postingState is PostingState.Uploading) {
                val progress = (postingState as PostingState.Uploading).progress
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Uploading... $progress%")
                }
            } else {
                GlowButton(
                    text = "Post Item",
                    onClick = {
                        viewModel.postItem(
                            type = selectedType!!,
                            title = title,
                            description = description,
                            category = selectedCategory!!,
                            location = location,
                            imageUris = imageUris,
                            contactPreference = contactPreference,
                            contactDetail = contactDetail
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isFormValid
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

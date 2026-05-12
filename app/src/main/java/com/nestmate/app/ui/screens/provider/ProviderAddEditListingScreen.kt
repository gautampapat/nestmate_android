package com.nestmate.app.ui.screens.provider

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.nestmate.app.ui.components.NestMateTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderAddEditListingScreen(
    listingType: String,
    listingId: String?,
    onNavigateBack: () -> Unit,
    viewModel: ProviderAddEditListingViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val photoStates by viewModel.photoUploadStates.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val saveError by viewModel.saveError.collectAsStateWithLifecycle()
    val saveSuccess by viewModel.saveSuccess.collectAsStateWithLifecycle()
    var step by remember { mutableStateOf(1) }

    LaunchedEffect(Unit) { viewModel.init(listingType, listingId) }
    LaunchedEffect(saveSuccess) { if (saveSuccess) onNavigateBack() }

    val photoPicker = rememberLauncherForActivityResult(PickVisualMedia()) { uri: Uri? ->
        uri?.let { viewModel.pickAndUploadPhoto(context, it) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NestMateTopBar(
                title = { Text(if (listingId == null) "Add Listing" else "Edit Listing", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            // Step indicator
            StepIndicator(currentStep = step, totalSteps = 5)
            Spacer(Modifier.height(20.dp))

            when (step) {
                1 -> Step1BasicInfo(draft, onUpdate = { viewModel.updateDraft(it) })
                2 -> Step2Photos(
                    photoStates = photoStates,
                    onPickPhoto = { photoPicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
                    onRemovePhoto = { viewModel.removePhoto(it) },
                )
                3 -> Step3Location(
                    address = draft.address,
                    mapsLink = draft.googleMapsLink,
                    parsedCoords = viewModel.parsedCoords.collectAsStateWithLifecycle().value,
                    onAddressChange = { viewModel.updateDraft(draft.copy(address = it)) },
                    onMapsLinkChange = {
                        viewModel.updateDraft(draft.copy(googleMapsLink = it))
                        if (it.contains("maps.app") || it.contains("goo.gl") || it.contains("maps.google")) {
                            viewModel.parseMapsLink(it)
                        }
                    },
                )
                4 -> Step4TypeDetails(draft, listingType, onUpdate = { viewModel.updateDraft(it) })
                5 -> Step5Review(draft, photoStates, listingType)
            }

            Spacer(Modifier.height(24.dp))

            // Error message
            saveError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
            }

            // Navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (step > 1) {
                    OutlinedButton(
                        onClick = { step-- },
                        modifier = Modifier.weight(1f),
                    ) { Text("Back") }
                }
                if (step < 5) {
                    Button(
                        onClick = { step++ },
                        modifier = Modifier.weight(1f),
                    ) { Text("Next") }
                } else {
                    val canSubmit = photoStates.values.none { it is PhotoUploadState.Uploading } && draft.title.isNotBlank() && draft.address.isNotBlank()
                    Button(
                        onClick = { viewModel.saveListing() },
                        modifier = Modifier.weight(1f),
                        enabled = canSubmit && !isSaving,
                    ) {
                        if (isSaving) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text(if (listingId == null) "Publish Listing" else "Save Changes")
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StepIndicator(currentStep: Int, totalSteps: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        (1..totalSteps).forEach { i ->
            val active = i <= currentStep
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                    ),
            )
        }
    }
}

@Composable
private fun Step1BasicInfo(draft: ListingDraft, onUpdate: (ListingDraft) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Basic Info", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = draft.title,
            onValueChange = { onUpdate(draft.copy(title = it)) },
            label = { Text("Listing Title *") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = draft.description,
            onValueChange = { onUpdate(draft.copy(description = it)) },
            label = { Text("Description") },
            minLines = 3,
            maxLines = 6,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Step2Photos(
    photoStates: Map<Int, PhotoUploadState>,
    onPickPhoto: () -> Unit,
    onRemovePhoto: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Photos (up to 6)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("Add clear photos to attract more students.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
            itemsIndexed((0..5).toList()) { idx, slot ->
                val state = photoStates[slot] ?: PhotoUploadState.Idle
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                        .clickable(enabled = state is PhotoUploadState.Idle || state is PhotoUploadState.Failed) { onPickPhoto() },
                    contentAlignment = Alignment.Center,
                ) {
                    when (state) {
                        is PhotoUploadState.Idle -> Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        is PhotoUploadState.Uploading -> CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 3.dp)
                        is PhotoUploadState.Success -> {
                            AsyncImage(model = state.url, contentDescription = "Icon", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            IconButton(onClick = { onRemovePhoto(slot) }, modifier = Modifier.align(Alignment.TopEnd).size(24.dp)) {
                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                        is PhotoUploadState.Failed -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error)
                                Text("Retry", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Step3Location(
    address: String,
    mapsLink: String,
    parsedCoords: Pair<Double, Double>?,
    onAddressChange: (String) -> Unit,
    onMapsLinkChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Location", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = address,
            onValueChange = onAddressChange,
            label = { Text("Full Address *") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = mapsLink,
            onValueChange = onMapsLinkChange,
            label = { Text("Google Maps Link (optional)") },
            placeholder = { Text("Open Maps → Share → Copy Link → paste here") },
            modifier = Modifier.fillMaxWidth(),
        )
        parsedCoords?.let { (lat, lng) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Check, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                Text("Coordinates parsed: %.5f, %.5f".format(lat, lng), style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Step4TypeDetails(
    draft: ListingDraft,
    listingType: String,
    onUpdate: (ListingDraft) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        when (listingType) {
            "FLAT_PG_HOSTEL" -> {
                OutlinedTextField(
                    value = draft.rentRupees,
                    onValueChange = { onUpdate(draft.copy(rentRupees = it)) },
                    label = { Text("Monthly Rent (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = draft.depositRupees,
                    onValueChange = { onUpdate(draft.copy(depositRupees = it)) },
                    label = { Text("Security Deposit (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                var bhkExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = bhkExpanded, onExpandedChange = { bhkExpanded = !bhkExpanded }) {
                    OutlinedTextField(
                        value = draft.bhkType.ifBlank { "Select BHK Type" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("BHK Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(bhkExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = bhkExpanded, onDismissRequest = { bhkExpanded = false }) {
                        listOf("1RK", "1BHK", "2BHK", "3BHK", "PG Room", "Hostel Room").forEach { opt ->
                            DropdownMenuItem(text = { Text(opt) }, onClick = { onUpdate(draft.copy(bhkType = opt)); bhkExpanded = false })
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = draft.isBachelorFriendly, onClick = { onUpdate(draft.copy(isBachelorFriendly = !draft.isBachelorFriendly)) }, label = { Text("Bachelor Friendly") })
                    FilterChip(selected = draft.isFemaleOnly, onClick = { onUpdate(draft.copy(isFemaleOnly = !draft.isFemaleOnly)) }, label = { Text("Female Only") })
                }
            }
            "MESS" -> {
                OutlinedTextField(
                    value = draft.monthlyChargeRupees,
                    onValueChange = { onUpdate(draft.copy(monthlyChargeRupees = it)) },
                    label = { Text("Monthly Charge (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = draft.isVegOnly, onClick = { onUpdate(draft.copy(isVegOnly = !draft.isVegOnly)) }, label = { Text("Veg Only") })
                    FilterChip(selected = draft.trialAvailable, onClick = { onUpdate(draft.copy(trialAvailable = !draft.trialAvailable)) }, label = { Text("Trial Available") })
                }
                OutlinedTextField(
                    value = draft.menuText,
                    onValueChange = { onUpdate(draft.copy(menuText = it)) },
                    label = { Text("Today's Menu (optional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            else -> {
                OutlinedTextField(
                    value = draft.priceDescription,
                    onValueChange = { onUpdate(draft.copy(priceDescription = it)) },
                    label = { Text("Price / Rate") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = draft.timings,
                    onValueChange = { onUpdate(draft.copy(timings = it)) },
                    label = { Text("Working Hours") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (listingType == "TUTOR") {
                    OutlinedTextField(
                        value = draft.specialisation,
                        onValueChange = { onUpdate(draft.copy(specialisation = it)) },
                        label = { Text("Subjects / Specialisation") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun Step5Review(
    draft: ListingDraft,
    photoStates: Map<Int, PhotoUploadState>,
    listingType: String,
) {
    val uploadedCount = photoStates.values.count { it is PhotoUploadState.Success }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Review & Publish", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        ReviewRow("Title", draft.title)
        ReviewRow("Address", draft.address)
        ReviewRow("Photos", "$uploadedCount uploaded")
        if (draft.title.isBlank() || draft.address.isBlank()) {
            Text(
                "⚠ Please fill in all required fields (marked *) before publishing.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (photoStates.values.any { it is PhotoUploadState.Uploading }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Text("Photo upload in progress…", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(100.dp))
        Text(value.ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
    }
}

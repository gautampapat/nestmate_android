package com.nestmate.app.ui.screens.housing

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Phone
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.google.firebase.auth.FirebaseAuth
import com.nestmate.app.data.model.ProviderListing
import com.nestmate.app.ui.screens.provider.ProviderInquiriesViewModel
import kotlinx.coroutines.launch
import com.nestmate.app.ui.components.NestMateTopBar

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HousingDetailScreen(
    listingId: String,
    onNavigateBack: () -> Unit,
    viewModel: HousingViewModel = hiltViewModel(),
) {
    val detailState by viewModel.detailState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showInquirySheet by remember { mutableStateOf(false) }

    LaunchedEffect(listingId) { viewModel.loadListingDetail(listingId) }

    Scaffold(
        topBar = {
            NestMateTopBar(
                title = { Text("Listing Details") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
            )
        },
    ) { padding ->
        when (val state = detailState) {
            null, is HousingDetailState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is HousingDetailState.Error -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text(state.message, color = MaterialTheme.colorScheme.error) }
            is HousingDetailState.Success -> {
                val listing = state.listing
                // Photo gallery — HorizontalPager with dot indicators
                val photoCount = listing.photoUrls.size.coerceAtLeast(1)
                val pagerState = rememberPagerState(pageCount = { photoCount })

                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 32.dp)) {
                    item {
                        Column {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                            ) { page ->
                                if (listing.photoUrls.isNotEmpty()) {
                                    AsyncImage(
                                        model = listing.photoUrls[page],
                                        contentDescription = "Listing photo ${page + 1}",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Home,
                                            contentDescription = "No photo",
                                            modifier = Modifier.size(64.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            // Dot indicators
                            if (listing.photoUrls.size > 1) {
                                LazyRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                                ) {
                                    items(listing.photoUrls.size) { index ->
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (pagerState.currentPage == index)
                                                        MaterialTheme.colorScheme.primary
                                                    else
                                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Column(modifier = Modifier.padding(20.dp)) {
                            // Title + rent
                            Text(listing.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            if (listing.rentPaise > 0) {
                                Text("₹${listing.rentRupees}/month", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                if (listing.depositPaise > 0) Text("Deposit: ₹${listing.depositPaise / 100}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Spacer(Modifier.height(12.dp))

                            // Address
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(listing.address, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Spacer(Modifier.height(12.dp))

                            // Chips
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (listing.bhkType.isNotBlank()) ChipLabel(listing.bhkType)
                                if (listing.isFemaleOnly) ChipLabel("Female Only", Color(0xFFC2185B), Color(0xFFFCE4EC))
                                if (listing.isBachelorFriendly) ChipLabel("Bachelor Friendly", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                            }

                            Spacer(Modifier.height(16.dp))

                            // Description
                            if (listing.description.isNotBlank()) {
                                Text("About this listing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(4.dp))
                                Text(listing.description, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(16.dp))
                            }

                            // Google Maps card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Default.Map, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("Location", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                        Text(listing.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    FilledTonalButton(onClick = {
                                        val uri = if (listing.googleMapsLink.isNotBlank()) Uri.parse(listing.googleMapsLink)
                                        else Uri.parse("geo:0,0?q=${Uri.encode(listing.address)}")
                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    }) { Text("Open Maps") }
                                }
                            }

                            Spacer(Modifier.height(20.dp))

                            // Provider info + contact
                            Text("Owner", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(listing.ownerName.ifBlank { "NestMate Provider" }, style = MaterialTheme.typography.bodyMedium)
                                if (listing.isVerifiedByAdmin) {
                                    Spacer(Modifier.width(8.dp))
                                    Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFE8F5E9)) {
                                        Text("✓ Verified", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))

                            if (isLoggedIn) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    FilledTonalButton(
                                        onClick = { showInquirySheet = true },
                                        modifier = Modifier.weight(1f),
                                    ) { Text("Send Inquiry") }
                                }
                            }
                        }
                    }
                }

                if (showInquirySheet) {
                    InquiryBottomSheet(
                        listing = listing,
                        sheetState = sheetState,
                        onDismiss = { showInquirySheet = false },
                        onSubmit = { msg ->
                            viewModel.submitInquiry(msg, listing) { success ->
                                // success state handled in bottom sheet if needed, or just let it close
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChipLabel(
    text: String,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    bgColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    androidx.compose.material3.Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InquiryBottomSheet(
    listing: ProviderListing,
    sheetState: androidx.compose.material3.SheetState,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    val context = LocalContext.current
    val user = FirebaseAuth.getInstance().currentUser
    var message by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom = 32.dp)) {
            Text("Contact Provider", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Re: ${listing.title}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Your message *") },
                placeholder = { Text("Hi, I'm interested in this listing. Is it available?") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            if (submitted) {
                Text("✓ Inquiry sent! The provider will reply shortly.", color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold)
            } else {
                Button(
                    onClick = {
                        onSubmit(message)
                        submitted = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = message.isNotBlank(),
                ) { Text("Send Inquiry") }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        }
    }
}

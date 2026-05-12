package com.nestmate.app.ui.screens.profile

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nestmate.app.data.model.SanitisedUserProfile
import com.nestmate.app.ui.components.EmptyState
import com.nestmate.app.ui.components.NestMateTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherUserProfileScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val profile by viewModel.otherUserProfile.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        viewModel.loadOtherUserProfile(userId)
    }

    Scaffold(
        topBar = {
            NestMateTopBar(
                title = { Text(profile?.name ?: "") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, "More") }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text("Report User") }, onClick = { showMenu = false; viewModel.reportUser(userId, "Inappropriate Content") })
                        DropdownMenuItem(text = { Text("Block User", color = MaterialTheme.colorScheme.error) }, onClick = { showMenu = false; viewModel.blockUser(userId); onNavigateBack() })
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isLoading && profile == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        if (error != null) {
            Box(Modifier.padding(padding)) { EmptyState("Error", error!!) }
            return@Scaffold
        }

        val user = profile ?: return@Scaffold

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { OtherProfileHeader(user) }

            if (user.bio.isNotBlank()) {
                item { Text(user.bio, style = MaterialTheme.typography.bodyMedium) }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Academic Info", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                        if (user.courseOrDepartment.isNotBlank()) Text("Course: ${user.courseOrDepartment}", style = MaterialTheme.typography.bodyMedium)
                        if (user.yearOfStudy.isNotBlank()) Text("Year: ${user.yearOfStudy}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (user.phone != null || user.linkedinUrl != null || user.instagramUrl != null) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Contact & Socials", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                            if (user.phone != null) ContactRow(Icons.Default.Phone, user.phone)
                            if (user.linkedinUrl != null) ContactRow(Icons.Default.Link, "LinkedIn") {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(user.linkedinUrl.let { if (it.startsWith("http")) it else "https://$it" })))
                            }
                            if (user.instagramUrl != null) ContactRow(Icons.Default.CameraAlt, "Instagram") {
                                val handle = user.instagramUrl.removePrefix("@")
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/$handle")))
                            }
                        }
                    }
                }
            }

            item {
                Text("Activity & Listings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
                Text("User's public listings and posts will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun OtherProfileHeader(user: SanitisedUserProfile) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(user.name.take(1).uppercase(), style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(user.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                if (user.trustRating >= 4.0f) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.Verified, "Trusted", tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                }
            }
            Text(user.college.ifBlank { "Student" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text("${user.trustRating} ⭐") }, modifier = Modifier.height(24.dp))
                AssistChip(onClick = {}, label = { Text("${user.successfulTrades} Trades") }, modifier = Modifier.height(24.dp))
            }
        }
    }
}

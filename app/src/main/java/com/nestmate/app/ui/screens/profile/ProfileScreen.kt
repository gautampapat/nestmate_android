package com.nestmate.app.ui.screens.profile

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nestmate.app.data.model.User
import com.nestmate.app.ui.components.EmptyState
import com.nestmate.app.ui.components.GlassCard
import com.nestmate.app.ui.components.GlassSurface
import com.nestmate.app.ui.theme.SuccessGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToSplash: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToVerification: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Activity", "Saved", "Settings")

    if (isLoading && profile == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else if (error != null) {
        EmptyState("Error", error!!)
    } else if (profile != null) {
        val user = profile!!
        GlassSurface {
        Column(modifier = Modifier.fillMaxSize()) {
        // Header
        ProfileHeader(user)

        // Tabs with custom colors for glass theme
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Tab Content
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            when (selectedTab) {
                0 -> { // Overview
                    item { OverviewTab(user, onNavigateToEditProfile, onNavigateToVerification) }
                }
                1 -> { // Activity
                    item { Text("Your recent listings, posts, and rides will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                2 -> { // Saved
                    item { Text("Your saved restaurants, items, and housing listings will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                3 -> { // Settings
                    item { SettingsTab(user, viewModel, onNavigateToSplash) }
                }
            }
        }
        }
        }
    }
}

@Composable
fun ProfileHeader(user: User) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                user.name.take(1).uppercase(),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(user.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                if (user.isVerified) {
                    Spacer(Modifier.width(8.dp))
                    com.nestmate.app.ui.components.VerifiedBadge(isProvider = false, showText = false)
                }
                if (user.trustRating >= 4.0f) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.Verified, "Trusted", tint = SuccessGreen, modifier = Modifier.size(20.dp))
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

@Composable
fun OverviewTab(user: User, onEdit: () -> Unit, onVerify: () -> Unit) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (user.bio.isNotBlank()) {
            Text(user.bio, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("Academic Info", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 8.dp))
                if (user.courseOrDepartment.isNotBlank()) Text("Course: ${user.courseOrDepartment}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                if (user.yearOfStudy.isNotBlank()) Text("Year: ${user.yearOfStudy}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("Contact & Socials", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 8.dp))
                if (user.phone.isNotBlank()) ContactRow(Icons.Default.Phone, user.phone)
                if (user.linkedinUrl?.isNotBlank() == true) ContactRow(Icons.Default.Link, "LinkedIn") {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(user.linkedinUrl.let { if (it.startsWith("http")) it else "https://$it" })))
                }
                if (user.instagramUrl?.isNotBlank() == true) ContactRow(Icons.Default.CameraAlt, "Instagram") {
                    val handle = user.instagramUrl.removePrefix("@")
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/$handle")))
                }
            }
        }

        if (!user.isVerified) {
            OutlinedButton(
                onClick = onVerify,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Verify Account") }
        }
        OutlinedButton(
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) { Text("Edit Profile") }
    }
}

@Composable
fun SettingsTab(user: User, viewModel: ProfileViewModel, onLogOut: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Account", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        ListItem(
            headlineContent = { Text("Email") },
            supportingContent = { Text(user.email) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        Text("Privacy", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        ListItem(
            headlineContent = { Text("Phone Visibility") },
            supportingContent = { Text(user.phoneVisibility) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
        ListItem(
            headlineContent = { Text("Social Visibility") },
            supportingContent = { Text(user.socialVisibility) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        Button(
            onClick = { viewModel.logout(onSuccess = onLogOut) },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Log Out")
        }
    }
}

@Composable
fun ContactRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: (() -> Unit)? = null) {
    val modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).let {
        if (onClick != null) it.clickable(onClick = onClick) else it
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Icon(icon, contentDescription = "Icon", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
}

package com.nestmate.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nestmate.app.ui.components.GlassCard
import com.nestmate.app.ui.components.GlassSurface
import com.nestmate.app.ui.theme.*
import com.nestmate.app.ui.components.NestMateCard
import com.nestmate.app.ui.navigation.Screen
import com.nestmate.app.ui.screens.home.HomeViewModel
import com.nestmate.app.ui.screens.home.CommunityAlert
import com.nestmate.app.ui.screens.home.ForumPost
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.nestmate.app.R
import com.nestmate.app.ui.screens.profile.ProfileScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateTo: (String) -> Unit,
    onNavigateToSplash: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val latestAlert by viewModel.latestAlert.collectAsStateWithLifecycle()
    val latestPost by viewModel.latestPost.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    
    val connectionViewModel: com.nestmate.app.ui.screens.connections.ConnectionViewModel = hiltViewModel()
    val incomingCount by connectionViewModel.incomingCount.collectAsStateWithLifecycle()

    var selectedItem by rememberSaveable { mutableStateOf(0) }
    
    val navItems = listOf(
        Triple("Home", Icons.Default.Home, null),
        Triple("Mess", Icons.Default.Restaurant, Screen.MessList.route),
        Triple("Market", Icons.Default.ShoppingCart, Screen.MarketplaceList.route),
        Triple("Finance", Icons.Default.AccountBalanceWallet, Screen.BillList.route),
        Triple("Profile", Icons.Default.Person, null)
    )

    GlassSurface {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                ) {
                    navItems.forEachIndexed { index, item ->
                        NavigationBarItem(
                            icon = { Icon(item.second, contentDescription = item.first) },
                            label = { Text(item.first) },
                            selected = selectedItem == index,
                            onClick = {
                                if (item.third != null) {
                                    onNavigateTo(item.third!!)
                                } else {
                                    selectedItem = index
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (selectedItem == 0) {
                DashboardView(onNavigateTo, viewModel, latestAlert, latestPost, userProfile, incomingCount)
            } else if (selectedItem == 4) {
                ProfileScreen(
                    onNavigateToSplash = onNavigateToSplash,
                    onNavigateToEditProfile = { onNavigateTo(Screen.EditProfile.route) },
                    onNavigateToVerification = { onNavigateTo(Screen.StudentVerification.route) }
                )
            }
        }
    }
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardView(
    onNavigateTo: (String) -> Unit,
    viewModel: HomeViewModel,
    latestAlert: CommunityAlert?,
    latestPost: ForumPost?,
    userProfile: com.nestmate.app.data.model.User?,
    incomingCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .imePadding()
    ) {
        Spacer(Modifier.height(8.dp))
        
        // Header — simple row, not a GlassCard top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    viewModel.timeGreeting,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { onNavigateTo(Screen.EditProfile.route) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Verification Banner
        if (userProfile != null && !userProfile.isVerified) {
            GlassCard(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                cornerRadius = 14.dp,
                glowColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                onClick = { onNavigateTo(Screen.StudentVerification.route) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Alert",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Verify your college email to unlock all features",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Text(
                        "Verify Now →",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Alert Strip
        if (latestAlert != null) {
            val contentColor = when (latestAlert.type) {
                "DANGER" -> MaterialTheme.colorScheme.error
                "WARNING" -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.primary
            }

            GlassCard(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                cornerRadius = 14.dp,
                glowColor = contentColor.copy(alpha = 0.06f),
                onClick = { onNavigateTo(Screen.SafetyHub.route) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = "Alert", tint = contentColor, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Safety Alert", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = contentColor)
                        Spacer(Modifier.height(2.dp))
                        Text(latestAlert.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        // Quick Actions
        Text(
            "Quick Actions",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            QuickActionItem("SOS", Icons.Default.Emergency, MaterialTheme.colorScheme.error) { onNavigateTo(Screen.SafetyHub.route) }
            QuickActionItem("Ride", Icons.Default.DirectionsCar, MaterialTheme.colorScheme.secondary) { onNavigateTo(Screen.RideSharing.route) }
            QuickActionItem("Mess", Icons.Default.Restaurant, MaterialTheme.colorScheme.primary) { onNavigateTo(Screen.MessList.route) }
            QuickActionItem("Split", Icons.Default.Receipt, MaterialTheme.colorScheme.tertiary) { onNavigateTo(Screen.BillList.route) }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Category Carousel
        Text(
            "Explore Modules",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        data class ModuleInfo(val title: String, val icon: ImageVector, val route: String, val tint: Color? = null, val badge: Int = 0)
        val modules = listOf(
            ModuleInfo("Housing", Icons.Default.HomeWork, Screen.HousingList.route),
            ModuleInfo("Roommates", Icons.Default.AccountCircle, Screen.RoommateMatching.route),
            ModuleInfo("Connections", Icons.Default.People, Screen.Connections.route, badge = incomingCount),
            ModuleInfo("Restaurants", Icons.Default.Restaurant, Screen.RestaurantBrowse.route),
            ModuleInfo("Spending", Icons.Default.AccountBalanceWallet, Screen.SpendingDashboard.route),
            ModuleInfo("Services", Icons.Default.Build, Screen.ServicesHub.route),
            ModuleInfo("Health", Icons.Default.MedicalServices, Screen.HealthHub.route),
            ModuleInfo("Finance", Icons.Default.AccountBalanceWallet, Screen.BillList.route),
            ModuleInfo("Lost & Found", Icons.Default.FindInPage, Screen.LostFound.route, WarningAmber)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            items(modules) { mod ->
                CategoryCard(mod.title, mod.icon, mod.tint, mod.badge) { onNavigateTo(mod.route) }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Buddy Prompt
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp,
            glowColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            onClick = { onNavigateTo(Screen.BuddyHome.route) }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "New to College?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Connect with a senior Buddy for guidance & tips.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.GroupAdd, contentDescription = "Icon", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Community Pulse
        Text(
            "Community Pulse",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onNavigateTo(Screen.CommunityHub.route) }
        ) {
            Column {
                if (latestPost != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val initial = latestPost.authorName.take(1).uppercase()
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(initial, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("${latestPost.authorName} • Recent", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(latestPost.content, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("💬 ${latestPost.commentCount} Comments", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text("No recent posts. Be the first to share something with the community!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun QuickActionItem(title: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f))
                .border(0.5.dp, color.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(26.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryCard(title: String, icon: ImageVector, tint: Color? = null, badgeCount: Int = 0, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier.width(110.dp).height(110.dp),
        cornerRadius = 16.dp,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val iconTint = tint ?: MaterialTheme.colorScheme.primary
            if (badgeCount > 0) {
                BadgedBox(badge = { Badge { Text(badgeCount.toString()) } }) {
                    Icon(icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(32.dp))
                }
            } else {
                Icon(icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

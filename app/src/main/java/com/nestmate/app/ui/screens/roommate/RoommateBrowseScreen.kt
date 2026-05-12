package com.nestmate.app.ui.screens.roommate

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.nestmate.app.data.model.Gender
import com.nestmate.app.data.model.RoomType
import com.nestmate.app.ui.components.EmptyState
import com.nestmate.app.ui.components.NestMateCard
import com.nestmate.app.ui.components.NestMatePrimaryButton
import com.nestmate.app.ui.components.NestMateTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoommateBrowseScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSetup: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToConnections: () -> Unit,
    onNavigateToGroups: () -> Unit,
) {
    val viewModel: RoommateViewModel = hiltViewModel()
    val scored by viewModel.scoredProfiles.collectAsStateWithLifecycle()
    val selfProfile by viewModel.currentUserProfile.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NestMateTopBar(
                title = { Text("Roommates") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToGroups) {
                        Icon(Icons.Filled.Group, contentDescription = "Groups")
                    }
                    IconButton(onClick = onNavigateToConnections) {
                        Icon(Icons.Filled.Person, contentDescription = "Connections")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        if (selfProfile == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Set up your roommate profile",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.size(12.dp))
                Text(
                    text = "We score compatibility based on your lifestyle preferences.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(24.dp))
                NestMatePrimaryButton(text = "Get Started", onClick = onNavigateToSetup)
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                FilterBar(
                    selectedGender = filters.gender,
                    onGenderChange = viewModel::setGenderFilter,
                    selectedRoom = filters.roomType,
                    onRoomChange = viewModel::setRoomTypeFilter,
                    onClearFilters = viewModel::clearFilters,
                )

                Spacer(Modifier.size(8.dp))

                if (scored.isEmpty()) {
                    EmptyState(
                        title = "No matches yet",
                        message = "Check back later or invite friends to NestMate.",
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(scored, key = { it.profile.userId }) { sp ->
                            ProfileRow(sp = sp, onClick = { 
                                if (currentUser?.isVerified == true) {
                                    onNavigateToDetail(sp.profile.userId)
                                } else {
                                    scope.launch { snackbarHostState.showSnackbar("Verify your college email to use this feature") }
                                }
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterBar(
    selectedGender: Gender?,
    onGenderChange: (Gender?) -> Unit,
    selectedRoom: RoomType?,
    onRoomChange: (RoomType?) -> Unit,
    onClearFilters: () -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(Gender.values().toList()) { g ->
            FilterChip(
                selected = selectedGender == g,
                onClick = { onGenderChange(if (selectedGender == g) null else g) },
                label = { Text(g.label) },
            )
        }
        items(RoomType.values().toList()) { r ->
            FilterChip(
                selected = selectedRoom == r,
                onClick = { onRoomChange(if (selectedRoom == r) null else r) },
                label = { Text(r.label) },
            )
        }
        item {
            AssistChip(
                onClick = onClearFilters,
                label = { Text("Clear") },
                leadingIcon = { Icon(Icons.Filled.Tune, contentDescription = "Icon") },
            )
        }
    }
}

@Composable
private fun ProfileRow(sp: ScoredProfile, onClick: () -> Unit) {
    NestMateCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (!sp.profile.photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = sp.profile.photoUrl,
                        contentDescription = "Icon",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                    )
                } else {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = "Icon",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = sp.profile.name.ifBlank { "Student" },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (sp.isRecentlyActive) {
                        AssistChip(
                            onClick = {},
                            label = { Text("Active") },
                        )
                    }
                }
                Text(
                    text = "${sp.profile.collegeName}  •  ${sp.profile.course}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Text(
                    text = "Rs ${sp.profile.minBudget}–${sp.profile.maxBudget}  •  ${sp.profile.roomTypePreference.label}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = "${sp.score}%",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = when {
                    sp.score >= 70 -> MaterialTheme.colorScheme.primary
                    sp.score >= 40 -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

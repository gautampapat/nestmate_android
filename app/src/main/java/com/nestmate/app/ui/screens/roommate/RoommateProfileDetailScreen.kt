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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.nestmate.app.data.model.ConnectionStatus
import com.nestmate.app.data.model.RoommateConnection
import com.nestmate.app.ui.components.NestMateCard
import com.nestmate.app.ui.components.NestMatePrimaryButton
import com.nestmate.app.ui.components.NestMateTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoommateProfileDetailScreen(
    targetUserId: String,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToCreateBill: (String) -> Unit = {},
) {
    val viewModel: RoommateViewModel = hiltViewModel()
    val scoredList by viewModel.scoredProfiles.collectAsStateWithLifecycle()

    var connection by remember { mutableStateOf<RoommateConnection?>(null) }
    LaunchedEffect(targetUserId) {
        viewModel.observeConnectionWith(targetUserId).collect { connection = it }
    }

    val snackbar = remember { SnackbarHostState() }
    val error by viewModel.error.collectAsStateWithLifecycle()
    LaunchedEffect(error) { error?.let { snackbar.showSnackbar(it); viewModel.clearError() } }

    val sp = scoredList.firstOrNull { it.profile.userId == targetUserId }
    var menuOpen by remember { mutableStateOf(false) }
    var blockDialog by remember { mutableStateOf(false) }
    var showConnectSheet by remember { mutableStateOf(false) }
    var connectNote by remember { mutableStateOf("") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            NestMateTopBar(
                title = { Text(sp?.profile?.name.orEmpty().ifBlank { "Profile" }) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        if (connection?.status == ConnectionStatus.ACCEPTED) {
                            DropdownMenuItem(
                                text = { Text("Split Bill") },
                                onClick = { menuOpen = false; onNavigateToCreateBill(targetUserId) },
                                leadingIcon = { Icon(Icons.Filled.Receipt, null) },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Block user") },
                            onClick = { menuOpen = false; blockDialog = true },
                            leadingIcon = { Icon(Icons.Filled.Block, null) },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            val c = connection
            BottomBar(
                connection = c,
                canChat = c?.status == ConnectionStatus.ACCEPTED && c.chatId != null,
                onConnect = { menuOpen = false; showConnectSheet = true },
                onAccept = { viewModel.respondToRequest(c!!.id, true) },
                onChat = { c?.chatId?.let(onNavigateToChat) },
                isIncomingRequest = c?.receiverId == viewModel.currentUserId && c?.status == ConnectionStatus.PENDING,
            )
        },
    ) { padding ->
        if (sp == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { Text("Loading…", color = MaterialTheme.colorScheme.onBackground) }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
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
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = sp.profile.name,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = "${sp.profile.collegeName}  •  ${sp.profile.course}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (sp.isRecentlyActive) {
                            AssistChip(onClick = {}, label = { Text("Recently active") })
                        }
                    }
                    Text(
                        text = "${sp.score}%",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                if (sp.profile.bio.isNotBlank()) {
                    Text(
                        text = sp.profile.bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }

                Text(
                    text = "Compatibility breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                sp.breakdown.breakdown.forEach { row ->
                    NestMateCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = row.dimension,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "You: ${row.selfValue}   •   Them: ${row.otherValue}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = if (row.matched) "+${row.pointsAwarded.toInt()}" else "0",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (row.matched) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }

    if (blockDialog) {
        AlertDialog(
            onDismissRequest = { blockDialog = false },
            title = { Text("Block this user?") },
            text = { Text("They won't appear in your browse results and can't contact you.") },
            confirmButton = {
                TextButton(onClick = {
                    blockDialog = false
                    viewModel.blockUser(targetUserId)
                    onNavigateBack()
                }) { Text("Block") }
            },
            dismissButton = {
                TextButton(onClick = { blockDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showConnectSheet) {
        ModalBottomSheet(
            onDismissRequest = { showConnectSheet = false }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Send Connection Request", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = connectNote,
                    onValueChange = { if (it.length <= 120) connectNote = it },
                    label = { Text("Optional note (max 120 chars)") },
                    modifier = Modifier.fillMaxWidth()
                )
                NestMatePrimaryButton(
                    text = "Send Request",
                    onClick = {
                        viewModel.sendConnectRequest(targetUserId, connectNote) {
                        }
                        showConnectSheet = false
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun BottomBar(
    connection: RoommateConnection?,
    canChat: Boolean,
    isIncomingRequest: Boolean,
    onConnect: () -> Unit,
    onAccept: () -> Unit,
    onChat: () -> Unit,
) {
    androidx.compose.material3.Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (canChat) {
                com.nestmate.app.ui.components.NestMateOutlinedButton(
                    text = "Connected ✓",
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.weight(1f)
                )
                NestMatePrimaryButton(
                    text = "Chat",
                    onClick = onChat,
                    modifier = Modifier.weight(1f),
                )
            } else if (isIncomingRequest) {
                FilledTonalButton(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                ) { Text("Accept Request") }
            } else if (connection?.status == ConnectionStatus.PENDING) {
                com.nestmate.app.ui.components.NestMateOutlinedButton(
                    text = "Request Sent",
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.weight(1f),
                )
            } else if (connection?.status == ConnectionStatus.REJECTED) {
                NestMatePrimaryButton(
                    text = "Connect Again",
                    onClick = onConnect,
                    modifier = Modifier.weight(1f),
                )
            } else {
                NestMatePrimaryButton(
                    text = "Connect",
                    onClick = onConnect,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

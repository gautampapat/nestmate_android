package com.nestmate.app.ui.screens.connections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.nestmate.app.data.model.RoommateConnection
import com.nestmate.app.ui.components.NestMateCard
import com.nestmate.app.ui.components.NestMatePrimaryButton
import kotlinx.coroutines.launch
import com.nestmate.app.ui.components.NestMateTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToBrowse: () -> Unit,
    onNavigateToCreateBill: (String) -> Unit
) {
    val viewModel: ConnectionViewModel = hiltViewModel()
    val connections by viewModel.connections.collectAsStateWithLifecycle()
    val incomingRequests by viewModel.incomingRequests.collectAsStateWithLifecycle()
    val outgoingRequests by viewModel.outgoingRequests.collectAsStateWithLifecycle()
    val incomingCount by viewModel.incomingCount.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
                viewModel.clearError()
            }
        }
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Connected", "Requests", "Sent")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            NestMateTopBar(
                title = { Text("Connections") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            if (index == 1 && incomingCount > 0) {
                                BadgedBox(badge = { Badge { Text(incomingCount.toString()) } }) {
                                    Text(title)
                                }
                            } else {
                                Text(title)
                            }
                        }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> ConnectedTab(
                    connections = connections,
                    onNavigateToBrowse = onNavigateToBrowse,
                    onNavigateToCreateBill = onNavigateToCreateBill,
                    onNavigateToChat = onNavigateToChat,
                    onRemoveConnection = { viewModel.removeConnection(it) }
                )
                1 -> RequestsTab(
                    incomingRequests = incomingRequests,
                    onAccept = { viewModel.acceptRequest(it) },
                    onReject = { viewModel.rejectRequest(it) }
                )
                2 -> SentTab(
                    outgoingRequests = outgoingRequests,
                    onCancel = { viewModel.removeConnection(it) }
                )
            }
        }
    }
}

@Composable
fun ConnectedTab(
    connections: List<RoommateConnection>,
    onNavigateToBrowse: () -> Unit,
    onNavigateToCreateBill: (String) -> Unit,
    onNavigateToChat: (String) -> Unit,
    onRemoveConnection: (String) -> Unit
) {
    if (connections.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "No connections yet.\nBrowse roommates to connect with others.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.size(16.dp))
                NestMatePrimaryButton(
                    text = "Browse Roommates",
                    onClick = onNavigateToBrowse
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(connections) { connection ->
                ConnectionCard(
                    connection = connection,
                    onPrimaryAction = { connection.chatId?.let { onNavigateToChat(it) } },
                    primaryActionText = "Message",
                    onSecondaryAction = { onNavigateToCreateBill(connection.receiverId) },
                    secondaryActionText = "Split Bill",
                    onRemove = { onRemoveConnection(connection.id) }
                )
            }
        }
    }
}

@Composable
fun RequestsTab(
    incomingRequests: List<RoommateConnection>,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit
) {
    if (incomingRequests.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No incoming requests.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(incomingRequests) { request ->
                var showDeclineDialog by remember { mutableStateOf(false) }

                ConnectionCard(
                    connection = request,
                    onPrimaryAction = { onAccept(request.id) },
                    primaryActionText = "Accept",
                    onSecondaryAction = { showDeclineDialog = true },
                    secondaryActionText = "Decline",
                    isSecondaryOutlined = true
                )

                if (showDeclineDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeclineDialog = false },
                        title = { Text("Decline Request") },
                        text = { Text("Are you sure you want to decline this request?") },
                        confirmButton = {
                            TextButton(onClick = {
                                showDeclineDialog = false
                                onReject(request.id)
                            }) { Text("Decline") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeclineDialog = false }) { Text("Cancel") }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SentTab(
    outgoingRequests: List<RoommateConnection>,
    onCancel: (String) -> Unit
) {
    if (outgoingRequests.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No pending sent requests.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(outgoingRequests) { request ->
                NestMateCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "To: ${request.receiverId}",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "Pending",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.size(8.dp))
                        TextButton(onClick = { onCancel(request.id) }) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionCard(
    connection: RoommateConnection,
    onPrimaryAction: () -> Unit,
    primaryActionText: String,
    onSecondaryAction: () -> Unit = {},
    secondaryActionText: String = "",
    isSecondaryOutlined: Boolean = false,
    onRemove: (() -> Unit)? = null
) {
    var showRemoveDialog by remember { mutableStateOf(false) }

    NestMateCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (!connection.connectedUserPhotoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = connection.connectedUserPhotoUrl,
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = "Avatar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = connection.connectedUserName.ifBlank { "User" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (connection.connectedUserCollege.isNotBlank()) {
                        Text(
                            text = connection.connectedUserCollege,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (onRemove != null) {
                    IconButton(onClick = { showRemoveDialog = true }) {
                        Icon(
                            Icons.Filled.Person, // Ideally a morevert or remove icon
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (connection.note.isNotBlank()) {
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "\"${connection.note}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.size(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (secondaryActionText.isNotBlank()) {
                    if (isSecondaryOutlined) {
                        OutlinedButton(
                            onClick = onSecondaryAction,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(secondaryActionText)
                        }
                    } else {
                        TextButton(
                            onClick = onSecondaryAction,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(secondaryActionText)
                        }
                    }
                }
                NestMatePrimaryButton(
                    text = primaryActionText,
                    onClick = onPrimaryAction,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showRemoveDialog && onRemove != null) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Remove Connection") },
            text = { Text("Are you sure you want to remove this connection?") },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveDialog = false
                    onRemove()
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) { Text("Cancel") }
            }
        )
    }
}

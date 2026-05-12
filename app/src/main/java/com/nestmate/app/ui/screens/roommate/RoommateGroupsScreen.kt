package com.nestmate.app.ui.screens.roommate

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nestmate.app.ui.components.EmptyState
import com.nestmate.app.ui.components.NestMateCard
import com.nestmate.app.ui.components.NestMatePrimaryButton
import com.nestmate.app.utils.CurrencyFormatter
import com.nestmate.app.ui.components.NestMateTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoommateGroupsScreen(onNavigateBack: () -> Unit) {
    val viewModel: RoommateViewModel = hiltViewModel()
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val currentUid = viewModel.currentUserId

    var showCreate by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NestMateTopBar(
                title = { Text("Group Housing") },
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
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreate = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = "Icon") },
                text = { Text("Create Group") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { padding ->
        if (groups.isEmpty()) {
            EmptyState(
                title = "No open groups",
                message = "Start a group if you've found a flat and need roommates.",
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(groups, key = { it.id }) { g ->
                    NestMateCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = g.flatDescription.ifBlank { "Untitled group" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${g.location}  •  ${CurrencyFormatter.formatRupees(g.rent)}/month",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "Spots: ${g.spotsConfirmed}/${g.spotsNeeded}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "By ${g.creatorName.ifBlank { "Student" }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            when {
                                g.creatorId == currentUid && g.pendingRequestIds.isNotEmpty() -> {
                                    Text(
                                        text = "Pending requests (${g.pendingRequestIds.size})",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    g.pendingRequestIds.forEachIndexed { index, uid ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            NestMatePrimaryButton(
                                                text = "Approve request ${index + 1}",
                                                onClick = { viewModel.approveJoinRequest(g.id, uid) },
                                                modifier = Modifier.weight(1f),
                                            )
                                        }
                                    }
                                }
                                currentUid != null && currentUid !in g.memberIds
                                    && currentUid !in g.pendingRequestIds -> {
                                    NestMatePrimaryButton(
                                        text = "Request to Join",
                                        onClick = { viewModel.requestJoinGroup(g.id) },
                                    )
                                }
                                currentUid in g.pendingRequestIds -> {
                                    NestMatePrimaryButton(
                                        text = "Request Sent",
                                        onClick = {},
                                        enabled = false,
                                    )
                                }
                                currentUid in g.memberIds -> {
                                    NestMatePrimaryButton(
                                        text = "Member",
                                        onClick = {},
                                        enabled = false,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateGroupDialog(
            onDismiss = { showCreate = false },
            onCreate = { desc, loc, rent, spots ->
                viewModel.createGroup(desc, loc, rent, spots) { result ->
                    if (result.isSuccess) showCreate = false
                }
            },
        )
    }
}

@Composable
private fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, Long, Int) -> Unit,
) {
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var rent by remember { mutableStateOf("") }
    var spots by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Group") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(description, { description = it }, label = { Text("Flat description") }, modifier = Modifier.fillMaxWidth().height(100.dp))
                OutlinedTextField(location, { location = it }, label = { Text("Location") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    rent, { rent = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Rent (Rs)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    spots, { spots = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Total spots needed") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val rentL = rent.toLongOrNull() ?: 0L
                val spotsInt = spots.toIntOrNull() ?: 0
                if (description.isNotBlank() && location.isNotBlank() && rentL > 0 && spotsInt > 1) {
                    onCreate(description.trim(), location.trim(), rentL, spotsInt)
                }
            }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

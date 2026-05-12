package com.nestmate.app.ui.screens.rides

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nestmate.app.data.model.RideRequest
import com.nestmate.app.data.model.RideStatus
import com.nestmate.app.data.model.VehicleType
import com.nestmate.app.ui.components.NestMateCard
import com.nestmate.app.ui.components.NestMateOutlinedButton
import com.nestmate.app.ui.components.NestMatePrimaryButton
import com.nestmate.app.ui.screens.safety.SafetyViewModel
import com.nestmate.app.utils.CurrencyFormatter
import com.nestmate.app.ui.components.NestMateTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveRideScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRating: (rideId: String) -> Unit,
) {
    val viewModel: RideViewModel = hiltViewModel()
    val safetyViewModel: SafetyViewModel = hiltViewModel()
    val ride by viewModel.activeRide.collectAsStateWithLifecycle()
    val emergencyContacts by safetyViewModel.contacts.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(error) {
        error?.let { snackbar.showSnackbar(it); viewModel.clearError() }
    }

    val context = LocalContext.current
    var showStartDialog by remember { mutableStateOf(false) }
    var showCompleteDialog by remember { mutableStateOf(false) }

    val currentRide = ride
    val isOwner = currentRide?.requesterId == viewModel.currentUserId

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            NestMateTopBar(
                title = { Text("Active ride") },
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
    ) { padding ->
        if (currentRide == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { Text("No active ride.", color = MaterialTheme.colorScheme.onBackground) }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = "Icon",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "Live map coming soon",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            NestMateCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "${currentRide.pickupLocation} → ${currentRide.dropLocation}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Status: ${currentRide.status}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (currentRide.confirmedPassengerIds.isNotEmpty()) {
                        Text(
                            text = "Passengers: ${currentRide.confirmedPassengerIds.size + 1}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            NestMateCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Driver",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.LocalTaxi,
                            contentDescription = "Icon",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(8.dp))
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(
                                text = currentRide.driverName ?: "Driver to be assigned",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            currentRide.vehicleNumber?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            currentRide.farePerPassenger?.let {
                Text(
                    text = "Your share: ${CurrencyFormatter.formatRupees(it)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            val phoneNumber = emergencyContacts.firstOrNull()?.phone ?: "112"
            NestMatePrimaryButton(
                text = "Emergency: call $phoneNumber",
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                },
            )

            NestMateOutlinedButton(
                text = "Share location",
                onClick = {
                    val text = "I'm on an active ride: ${currentRide.pickupLocation} → ${currentRide.dropLocation}"
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    val chooser = Intent.createChooser(intent, "Share via")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                },
            )

            when (currentRide.status) {
                RideStatus.SEARCHING, RideStatus.MATCHED -> {
                    if (isOwner) {
                        NestMatePrimaryButton(
                            text = "Start Ride (enter driver)",
                            onClick = { showStartDialog = true },
                        )
                    }
                    NestMateOutlinedButton(
                        text = "Cancel Ride",
                        onClick = { viewModel.cancelRide(currentRide.id) },
                    )
                }
                RideStatus.IN_PROGRESS -> {
                    if (isOwner) {
                        NestMatePrimaryButton(
                            text = "Complete Ride",
                            onClick = { showCompleteDialog = true },
                        )
                    }
                }
                else -> Unit
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showStartDialog) {
        StartRideDialog(
            onDismiss = { showStartDialog = false },
            onConfirm = { name, vehicleNum, vType ->
                showStartDialog = false
                viewModel.startRide(currentRide!!.id, name, vehicleNum, vType)
            },
        )
    }

    if (showCompleteDialog) {
        CompleteRideDialog(
            onDismiss = { showCompleteDialog = false },
            onConfirm = { fareRs ->
                showCompleteDialog = false
                viewModel.completeRide(currentRide!!.id, fareRs) { ok ->
                    if (ok) onNavigateToRating(currentRide.id)
                }
            },
        )
    }
}

@Composable
private fun StartRideDialog(
    onDismiss: () -> Unit,
    onConfirm: (driverName: String, vehicleNumber: String, VehicleType) -> Unit,
) {
    var driverName by remember { mutableStateOf("") }
    var vehicleNumber by remember { mutableStateOf("") }
    var vehicleType by remember { mutableStateOf(VehicleType.AUTO) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start ride") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(driverName, { driverName = it }, label = { Text("Driver name") }, singleLine = true)
                OutlinedTextField(vehicleNumber, { vehicleNumber = it }, label = { Text("Vehicle number") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VehicleType.values().forEach { t ->
                        androidx.compose.material3.FilterChip(
                            selected = t == vehicleType,
                            onClick = { vehicleType = t },
                            label = { Text(t.label) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (driverName.isNotBlank() && vehicleNumber.isNotBlank()) {
                    onConfirm(driverName.trim(), vehicleNumber.trim(), vehicleType)
                }
            }) { Text("Start") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun CompleteRideDialog(
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    var fareStr by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Complete ride") },
        text = {
            OutlinedTextField(
                fareStr, { fareStr = it.filter { ch -> ch.isDigit() } },
                label = { Text("Total fare paid (Rs)") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val fare = fareStr.toLongOrNull() ?: 0L
                if (fare > 0) onConfirm(fare)
            }) { Text("Done") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

package com.nestmate.app.ui.screens.rides

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nestmate.app.data.model.RideRequest
import com.nestmate.app.data.model.RideType
import com.nestmate.app.ui.components.EmptyState
import com.nestmate.app.ui.components.NestMateCard
import com.nestmate.app.ui.components.NestMateOutlinedButton
import com.nestmate.app.ui.components.NestMatePrimaryButton
import com.nestmate.app.utils.CurrencyFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.nestmate.app.ui.components.NestMateTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedRideMatchScreen(
    pickup: String,
    drop: String,
    scheduledAt: Long?,
    onNavigateBack: () -> Unit,
    onNavigateToActive: () -> Unit,
) {
    val viewModel: RideViewModel = hiltViewModel()
    val matches = viewModel.findMatchingShared(drop, scheduledAt).collectAsStateSafe(emptyList())
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val error by viewModel.error.collectAsStateWithLifecycle()
    LaunchedEffect(error) {
        error?.let { snackbar.showSnackbar(it); viewModel.clearError() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            NestMateTopBar(
                title = { Text("Shared ride matches") },
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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "$pickup → $drop",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                scheduledAt?.let {
                    val label = SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(Date(it))
                    Text(
                        text = "Scheduled for $label",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (matches.value.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    EmptyState(
                        title = "No matching rides",
                        message = "You can create a new shared ride — others can join yours.",
                    )
                    Column(modifier = Modifier.padding(16.dp)) {
                        NestMatePrimaryButton(
                            text = "Create New Shared Ride",
                            onClick = {
                                viewModel.createRide(
                                    pickup = pickup,
                                    drop = drop,
                                    rideType = RideType.SHARED,
                                    scheduledAt = scheduledAt,
                                    maxBudget = null,
                                    route = null,
                                ) { result ->
                                    result.onSuccess { onNavigateToActive() }
                                }
                            },
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(matches.value, key = { it.id }) { ride ->
                        MatchCard(
                            ride = ride,
                            onJoin = {
                                viewModel.joinRide(ride.id) { ok ->
                                    if (ok) onNavigateToActive()
                                }
                            },
                        )
                    }
                    item {
                        NestMateOutlinedButton(
                            text = "Create New Shared Ride Instead",
                            onClick = {
                                viewModel.createRide(
                                    pickup = pickup,
                                    drop = drop,
                                    rideType = RideType.SHARED,
                                    scheduledAt = scheduledAt,
                                    maxBudget = null,
                                    route = null,
                                ) { result ->
                                    result.onSuccess { onNavigateToActive() }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchCard(ride: RideRequest, onJoin: () -> Unit) {
    NestMateCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${ride.pickupLocation} → ${ride.dropLocation}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            ride.scheduledAt?.let { ts ->
                val label = SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(Date(ts))
                Text(
                    text = "Scheduled: $label",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val currentPassengers = ride.confirmedPassengerIds.size + 1
            val passengersAfterJoin = currentPassengers + 1
            val perPerson = if (ride.estimatedFare > 0L) {
                // Ceiling div so rounding doesn't underquote the fare.
                (ride.estimatedFare + passengersAfterJoin - 1) / passengersAfterJoin
            } else null
            Text(
                text = "Passengers: $currentPassengers (becomes $passengersAfterJoin if you join)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            perPerson?.let {
                Text(
                    text = "~${CurrencyFormatter.formatRupees(it)}/person",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(8.dp))
            NestMatePrimaryButton(text = "Join", onClick = onJoin)
        }
    }
}

@Composable
private fun <T> kotlinx.coroutines.flow.Flow<T>.collectAsStateSafe(
    initial: T,
): androidx.compose.runtime.State<T> {
    val state = remember { mutableStateOf(initial) }
    LaunchedEffect(this) {
        collect { state.value = it }
    }
    return state
}

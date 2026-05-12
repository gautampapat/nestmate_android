package com.nestmate.app.ui.screens.rides

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nestmate.app.data.model.PredefinedRoute
import com.nestmate.app.data.model.RideType
import com.nestmate.app.ui.components.NestMateCard
import com.nestmate.app.ui.components.NestMatePrimaryButton
import com.nestmate.app.utils.CurrencyFormatter
import kotlinx.coroutines.launch
import com.nestmate.app.ui.components.NestMateTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideHomeScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSharedMatch: (pickup: String, drop: String, scheduledAt: Long?) -> Unit,
    onNavigateToActive: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToScheduled: () -> Unit,
) {
    val viewModel: RideViewModel = hiltViewModel()
    val routes by viewModel.predefinedRoutes.collectAsStateWithLifecycle()
    val activeRide by viewModel.activeRide.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(error) {
        error?.let { snackbar.showSnackbar(it); viewModel.clearError() }
    }

    var pickup by remember { mutableStateOf("") }
    var drop by remember { mutableStateOf("") }
    var rideType by remember { mutableStateOf(RideType.SHARED) }
    var maxBudget by remember { mutableStateOf("") }
    var selectedRoute by remember { mutableStateOf<PredefinedRoute?>(null) }
    var isScheduled by remember { mutableStateOf(false) }

    val estimate by remember(rideType, selectedRoute) {
        derivedStateOf {
            viewModel.estimateFare(rideType, selectedRoute, passengers = 2)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            NestMateTopBar(
                title = { Text("Book a ride") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToScheduled) {
                        Icon(Icons.Filled.Schedule, contentDescription = "Scheduled")
                    }
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Filled.History, contentDescription = "History")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (activeRide != null) {
                NestMateCard(modifier = Modifier.fillMaxWidth(), onClick = onNavigateToActive) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "You have an active ride",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            "${activeRide?.pickupLocation} → ${activeRide?.dropLocation}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            "Tap to view",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !isScheduled,
                    onClick = { isScheduled = false },
                    label = { Text("Book Now") },
                )
                FilterChip(
                    selected = isScheduled,
                    onClick = { isScheduled = true },
                    label = { Text("Scheduled (+15 min)") },
                )
            }

            OutlinedTextField(
                value = pickup,
                onValueChange = { pickup = it; selectedRoute = null },
                label = { Text("Pickup") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = drop,
                onValueChange = { drop = it; selectedRoute = null },
                label = { Text("Drop") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (routes.isNotEmpty()) {
                Text(
                    "Common routes",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(routes) { route ->
                        FilterChip(
                            selected = selectedRoute?.id == route.id,
                            onClick = {
                                selectedRoute = route
                                pickup = route.pickup
                                drop = route.drop
                            },
                            label = { Text(route.label) },
                        )
                    }
                }
            }

            Text(
                "Ride type",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RideType.values().forEach { t ->
                    FilterChip(
                        selected = t == rideType,
                        onClick = { rideType = t },
                        label = { Text(t.label) },
                    )
                }
            }

            OutlinedTextField(
                value = maxBudget,
                onValueChange = { maxBudget = it.filter { ch -> ch.isDigit() } },
                label = { Text("Max budget, Rs (optional)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            NestMateCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Fare estimate",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val fareLabel = if (estimate.minRupees == estimate.maxRupees) {
                        "Rs ${estimate.minRupees}"
                    } else "Rs ${estimate.minRupees}–${estimate.maxRupees}"
                    Text(
                        text = fareLabel,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    estimate.perPassengerRupees?.let {
                        Text(
                            "~${CurrencyFormatter.formatRupees(it)}/person when shared",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                NestMatePrimaryButton(
                    text = when {
                        rideType == RideType.SHARED -> "Find Shared Ride"
                        isScheduled -> "Schedule Ride"
                        else -> "Book Ride"
                    },
                    onClick = submit@{
                        if (pickup.isBlank() || drop.isBlank()) {
                            scope.launch { snackbar.showSnackbar("Enter pickup and drop") }
                            return@submit
                        }
                        val budget = maxBudget.toLongOrNull()
                        val scheduledAt = if (isScheduled) {
                            System.currentTimeMillis() + 15L * 60L * 1000L
                        } else null

                        if (rideType == RideType.SHARED) {
                            onNavigateToSharedMatch(pickup, drop, scheduledAt)
                            return@submit
                        }

                        viewModel.createRide(
                            pickup = pickup,
                            drop = drop,
                            rideType = rideType,
                            scheduledAt = scheduledAt,
                            maxBudget = budget,
                            route = selectedRoute,
                        ) { result ->
                            result.onSuccess {
                                if (scheduledAt != null) onNavigateToScheduled()
                                else onNavigateToActive()
                            }
                        }
                    },
                )
            }
            Spacer(Modifier.width(0.dp))
        }
    }
}

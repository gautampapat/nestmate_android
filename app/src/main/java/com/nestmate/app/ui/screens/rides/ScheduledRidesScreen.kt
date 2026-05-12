package com.nestmate.app.ui.screens.rides

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nestmate.app.ui.components.EmptyState
import com.nestmate.app.ui.components.NestMateCard
import com.nestmate.app.ui.components.NestMateOutlinedButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.nestmate.app.ui.components.NestMateTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledRidesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit,
) {
    val viewModel: RideViewModel = hiltViewModel()
    val scheduled by viewModel.scheduledRides.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NestMateTopBar(
                title = { Text("Scheduled rides") },
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
                onClick = onNavigateToHome,
                icon = { Icon(Icons.Filled.Add, contentDescription = "Icon") },
                text = { Text("Book Another") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { padding ->
        if (scheduled.isEmpty()) {
            EmptyState(
                title = "No scheduled rides",
                message = "Schedule a ride to reserve an auto ahead of time.",
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(scheduled, key = { it.id }) { ride ->
                NestMateCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "${ride.pickupLocation} → ${ride.dropLocation}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        ride.scheduledAt?.let {
                            Text(
                                text = SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault())
                                    .format(Date(it)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        NestMateOutlinedButton(
                            text = "Cancel",
                            onClick = { viewModel.cancelRide(ride.id) },
                        )
                    }
                }
            }
        }
    }
}

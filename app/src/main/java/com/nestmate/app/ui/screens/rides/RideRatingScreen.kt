package com.nestmate.app.ui.screens.rides

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nestmate.app.data.model.RideRequest
import com.nestmate.app.ui.components.NestMateCard
import com.nestmate.app.ui.components.NestMatePrimaryButton
import kotlinx.coroutines.flow.collectLatest
import com.nestmate.app.ui.components.NestMateTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideRatingScreen(
    rideId: String,
    onNavigateBack: () -> Unit,
) {
    val viewModel: RideViewModel = hiltViewModel()

    var ride by remember { mutableStateOf<RideRequest?>(null) }
    LaunchedEffect(rideId) {
        viewModel.getRideById(rideId).collectLatest { ride = it }
    }

    var driverStars by remember { mutableStateOf(0) }
    var driverComment by remember { mutableStateOf("") }
    val passengerStars = remember { mutableStateMapOf<String, Int>() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NestMateTopBar(
                title = { Text("Rate your ride") },
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
        val current = ride
        if (current == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Loading…", color = MaterialTheme.colorScheme.onBackground)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            NestMateCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Driver: ${current.driverName ?: "—"}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    StarRow(stars = driverStars, onChange = { driverStars = it })
                    OutlinedTextField(
                        value = driverComment,
                        onValueChange = { driverComment = it },
                        label = { Text("Leave a comment (optional)") },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                    )
                }
            }

            val otherPassengers = current.confirmedPassengerIds.filter { it != viewModel.currentUserId }
            if (otherPassengers.isNotEmpty()) {
                Text(
                    "Rate co-passengers",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                otherPassengers.forEachIndexed { index, uid ->
                    NestMateCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Passenger ${index + 1}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            val starsForThis = passengerStars[uid] ?: 0
                            StarRow(stars = starsForThis, onChange = { passengerStars[uid] = it })
                        }
                    }
                }
            }

            NestMatePrimaryButton(
                text = "Submit reviews",
                enabled = driverStars > 0,
                onClick = {
                    val driverId = current.driverId ?: current.driverName ?: "driver"
                    viewModel.submitReview(
                        rideId = rideId,
                        targetId = driverId,
                        rating = driverStars.toFloat(),
                        comment = driverComment.takeIf { it.isNotBlank() },
                    )
                    passengerStars.forEach { (uid, stars) ->
                        if (stars > 0) {
                            viewModel.submitReview(
                                rideId = rideId,
                                targetId = uid,
                                rating = stars.toFloat(),
                                comment = null,
                            )
                        }
                    }
                    onNavigateBack()
                },
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StarRow(stars: Int, onChange: (Int) -> Unit) {
    Row(modifier = Modifier.padding(vertical = 8.dp)) {
        (1..5).forEach { n ->
            IconButton(onClick = { onChange(n) }, modifier = Modifier.size(44.dp)) {
                Icon(
                    imageVector = if (n <= stars) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = "$n stars",
                    tint = if (n <= stars) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

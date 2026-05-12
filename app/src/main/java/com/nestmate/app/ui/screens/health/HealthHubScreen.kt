package com.nestmate.app.ui.screens.health

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nestmate.app.data.model.Clinic
import com.nestmate.app.ui.components.EmptyState
import com.nestmate.app.ui.components.NestMateCard
import com.nestmate.app.ui.components.NestMateTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthHubScreen(
    onNavigateBack: () -> Unit,
    viewModel: HealthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NestMateTopBar(
                title = { androidx.compose.material3.Text("Health Hub", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onNavigateBack) {
                        androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(
                text = "Nearest Clinics & Hospitals near WCE",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 24.dp)
            )

        when (uiState) {
            is HealthState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is HealthState.Error -> {
                EmptyState(
                    title = "Failed Loading",
                    message = (uiState as HealthState.Error).message,
                    buttonText = "Retry",
                    onButtonClick = { viewModel.loadClinics() }
                )
            }
            is HealthState.Success -> {
                val state = uiState as HealthState.Success
                LazyColumn {
                    items(state.clinics) { clinic ->
                        ClinicCard(clinic)
                    }
                }
            }
        }
    }
    }
}

@Composable
fun ClinicCard(clinic: Clinic) {
    val context = LocalContext.current
    NestMateCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = clinic.name, style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Text(text = "${clinic.type} • ${clinic.distanceKm} km away", color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = clinic.address, style = MaterialTheme.typography.bodyMedium)
                Text(text = "Hours: ${clinic.operatingHours}", style = MaterialTheme.typography.bodyMedium)
                
                if (clinic.isEmergency24x7) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "24x7 Emergency Services",
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.error, RoundedCornerShape(4.dp))
                            .padding(4.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${clinic.contactNumber}"))
                    context.startActivity(intent)
                }
            ) {
                Icon(Icons.Default.Call, contentDescription = "Call Clinic", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

package com.nestmate.app.ui.screens.safety

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPolice
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.nestmate.app.ui.components.NestMateCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.nestmate.app.ui.components.NestMateTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyHubScreen(
    onNavigateToContacts: () -> Unit,
    viewModel: SafetyViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasSmsPermission = granted
    }

    // Modern color palette
    val dangerRed = Color(0xFFD32F2F)
    val policeBlue = Color(0xFF1976D2)
    val medicalGreen = Color(0xFF388E3C)

    Scaffold(
        topBar = {
            NestMateTopBar(
                title = { Text("Safety Hub", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Emergency SOS",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = dangerRed
            )
            Text(
                text = "Hold for 3 seconds to alert emergency contacts",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
            )

            // Hold to SOS Button UI
            SOSHoldButton(
                onTriggerSOS = {
                    if (!hasSmsPermission) {
                        permissionLauncher.launch(Manifest.permission.SEND_SMS)
                    } else {
                        viewModel.triggerSOS(context)
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Check permission warning
            if (!hasSmsPermission) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = "Icon", tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "SMS Permission denied. SOS will open a standard share dialog instead of sending silent alerts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Quick Actions
            Text("Quick Actions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SafetyActionCard(
                    title = "Campus Security",
                    icon = Icons.Default.Security,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    onClick = { /* TODO: Call campus */ }
                )
                SafetyActionCard(
                    title = "Local Police",
                    icon = Icons.Default.LocalPolice,
                    color = policeBlue,
                    modifier = Modifier.weight(1f),
                    onClick = { /* TODO: Call police */ }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SafetyActionCard(
                    title = "Medical Emergency",
                    icon = Icons.Default.LocalHospital,
                    color = medicalGreen,
                    modifier = Modifier.weight(1f),
                    onClick = { /* TODO: Call ambulance */ }
                )
                SafetyActionCard(
                    title = "Manage Contacts",
                    icon = Icons.Filled.AccountCircle,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToContacts
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun SOSHoldButton(onTriggerSOS: () -> Unit) {
    var isHolding by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    
    // Animate scale based on hold
    val scale by animateFloatAsState(if (isHolding) 1.2f else 1f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(240.dp)
    ) {
        // Outer progress ring
        CircularProgressIndicator(
            progress = { holdProgress },
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFD32F2F),
            strokeWidth = 8.dp,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        
        // Inner Button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(180.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(Color(0xFFD32F2F))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isHolding = true
                            var trigger = false
                            val job = scope.launch {
                                // 3 seconds = 30 intervals of 100ms
                                for (i in 1..30) {
                                    delay(100)
                                    holdProgress = i / 30f
                                    if (i == 30) {
                                        trigger = true
                                        onTriggerSOS()
                                    }
                                }
                            }
                            
                            tryAwaitRelease()
                            isHolding = false
                            holdProgress = 0f
                            job.cancel()
                        }
                    )
                }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "Icon",
                    modifier = Modifier.size(48.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "SOS",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun SafetyActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.defaultMinSize(minHeight = 110.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = "Icon", tint = color, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title, 
                style = MaterialTheme.typography.titleMedium, 
                fontWeight = FontWeight.Bold, 
                color = color,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

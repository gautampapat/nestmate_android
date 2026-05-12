package com.nestmate.app.ui.screens.profile

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import com.nestmate.app.ui.components.NestMateTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentVerificationScreen(
    onNavigateBack: () -> Unit,
    viewModel: StudentVerificationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var emailInput by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }

    // Clear error on input change
    LaunchedEffect(emailInput, otpInput) {
        if (error != null) viewModel.clearError()
    }

    Scaffold(
        topBar = {
            NestMateTopBar(
                title = { Text("Verify Account") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (uiState) {
                is VerificationState.Idle, is VerificationState.Loading -> {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Icon",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Verify with College Email",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Enter your official college (.edu/.ac.in) email to verify your student status.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("College Email") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        isError = error != null
                    )
                    if (error != null) {
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, start = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = {
                            val lower = emailInput.lowercase().trim()
                            val blockedDomains = listOf(
                                "@gmail.com", "@yahoo.com", "@outlook.com",
                                "@hotmail.com", "@icloud.com", "@protonmail.com",
                                "@rediffmail.com", "@live.com", "@yahoo.in"
                            )
                            val isPersonalEmail = blockedDomains.any { lower.endsWith(it) }
                            if (isPersonalEmail) {
                                viewModel.setError("Please use your official college email (.edu/.ac.in), not a personal email.")
                            } else {
                                viewModel.sendOtp(emailInput.trim())
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = emailInput.isNotBlank() && uiState !is VerificationState.Loading
                    ) {
                        if (uiState is VerificationState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Send Verification Code")
                        }
                    }
                }

                is VerificationState.OtpSent -> {
                    val expiresAt = (uiState as VerificationState.OtpSent).expiresAt
                    var timeLeftMs by remember { mutableLongStateOf(expiresAt - System.currentTimeMillis()) }

                    LaunchedEffect(expiresAt) {
                        while (timeLeftMs > 0) {
                            delay(1000)
                            timeLeftMs = expiresAt - System.currentTimeMillis()
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Icon",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Enter Code",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "We sent a 6-digit code to $emailInput",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    val focusRequesters = remember { List(6) { FocusRequester() } }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (i in 0 until 6) {
                            val char = otpInput.getOrNull(i)?.toString() ?: ""
                            BasicTextField(
                                value = char,
                                onValueChange = { newValue ->
                                    if (newValue.length <= 1) {
                                        val newOtp = otpInput.toMutableList()
                                        if (newValue.isEmpty()) {
                                            if (i < newOtp.size) newOtp.removeAt(i)
                                        } else {
                                            if (i < newOtp.size) newOtp[i] = newValue.first()
                                            else newOtp.add(newValue.first())
                                        }
                                        val finalOtp = newOtp.joinToString("").take(6)
                                        if (finalOtp.length <= 6) {
                                            otpInput = finalOtp
                                        }
                                        if (newValue.isNotEmpty() && i < 5) {
                                            focusRequesters[i + 1].requestFocus()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp, 56.dp)
                                    .focusRequester(focusRequesters[i])
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                    .onKeyEvent { event ->
                                        if (event.type == KeyEventType.KeyUp && event.key == Key.Backspace && char.isEmpty() && i > 0) {
                                            val newOtp = otpInput.toMutableList()
                                            if (i - 1 < newOtp.size) {
                                                newOtp.removeAt(i - 1)
                                                otpInput = newOtp.joinToString("")
                                            }
                                            focusRequesters[i - 1].requestFocus()
                                            true
                                        } else {
                                            false
                                        }
                                    },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = MaterialTheme.typography.titleLarge.copy(
                                    textAlign = TextAlign.Center,
                                    color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                ),
                                singleLine = true,
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.Center) {
                                        innerTextField()
                                    }
                                }
                            )
                        }
                    }

                    if (error != null) {
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    val mins = (timeLeftMs / 1000) / 60
                    val secs = (timeLeftMs / 1000) % 60
                    val timeStr = if (timeLeftMs > 0) String.format("%02d:%02d", mins, secs) else "Expired"
                    Text(
                        text = "Time remaining: $timeStr",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (timeLeftMs > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { viewModel.verifyOtp(otpInput) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = otpInput.length == 6 && timeLeftMs > 0
                    ) {
                        Text("Verify")
                    }

                    // BUG FIX 4: Show Resend Code button when timer has expired.
                    // emailInput is hoisted at the top of the composable (outside the when block)
                    // so it is accessible here across state transitions.
                    if (timeLeftMs <= 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(
                            onClick = { viewModel.sendOtp(emailInput) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Resend Code")
                        }
                    }
                }

                is VerificationState.Success -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Icon",
                        modifier = Modifier.size(80.dp),
                        tint = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Verified Successfully!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Your student account is now verified.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Continue")
                    }
                }
            }
        }
    }
}

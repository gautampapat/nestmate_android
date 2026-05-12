package com.nestmate.app.ui.screens.auth

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nestmate.app.data.model.User
import com.nestmate.app.ui.components.NestMatePrimaryButton
import com.nestmate.app.ui.components.NestMateTextButton
import com.nestmate.app.ui.components.GlassSurface
import com.nestmate.app.ui.components.GlowButton

private val SERVICE_TYPES = listOf(
    "Flat/PG/Hostel Owner",
    "Mess Owner",
    "Laundry",
    "Xerox",
    "Grocery",
    "Tutor",
    "Other",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    role: String = "student",
    onNavigateToVerification: () -> Unit,
    onNavigateToProviderDashboard: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    // Sync the role into the ViewModel so it is reflected in the submitted User object
    LaunchedEffect(role) { viewModel.setSelectedRole(role) }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            viewModel.resetState()
            if (role == "service_provider") {
                onNavigateToProviderDashboard()
            } else {
                onNavigateToVerification()
            }
        }
    }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var college by remember { mutableStateOf("Walchand College of Engineering") }
    var year by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }

    // Provider-only fields
    var serviceType by remember { mutableStateOf(SERVICE_TYPES.first()) }
    var serviceTypeExpanded by remember { mutableStateOf(false) }

    GlassSurface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
        Text(
            text = if (role == "service_provider") "Create Provider Account" else "Create Account",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(bottom = 32.dp, top = 24.dp),
        )

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))
        OutlinedTextField(value = college, onValueChange = { college = it }, label = { Text("College") }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), enabled = false)

        if (role == "student") {
            OutlinedTextField(value = year, onValueChange = { year = it }, label = { Text("Year (e.g. FY, SY)") }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))
            OutlinedTextField(value = gender, onValueChange = { gender = it }, label = { Text("Gender") }, modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp))
        }

        // Service type dropdown — providers only
        if (role == "service_provider") {
            ExposedDropdownMenuBox(
                expanded = serviceTypeExpanded,
                onExpandedChange = { serviceTypeExpanded = !serviceTypeExpanded },
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            ) {
                OutlinedTextField(
                    value = serviceType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Service Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(serviceTypeExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = serviceTypeExpanded,
                    onDismissRequest = { serviceTypeExpanded = false },
                ) {
                    SERVICE_TYPES.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                serviceType = option
                                serviceTypeExpanded = false
                            },
                        )
                    }
                }
            }
        }

        if (authState is AuthState.Loading) {
            CircularProgressIndicator(modifier = Modifier.padding(bottom = 16.dp))
        } else {
            GlowButton(
                text = "Register",
                onClick = {
                    val user = User(
                        name = name,
                        email = email,
                        collegeId = "wce_sangli",
                        year = year,
                        gender = gender,
                        role = role,
                        serviceType = if (role == "service_provider") serviceType else "",
                    )
                    viewModel.register(user, password)
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            )
        }

        if (authState is AuthState.Error) {
            Text(
                text = (authState as AuthState.Error).message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }

        NestMateTextButton(
            text = "Already have an account? Login",
            onClick = onNavigateToLogin,
        )
    }
    }
}

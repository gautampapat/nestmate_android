package com.nestmate.app.ui.screens.profile

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nestmate.app.ui.components.NestMatePrimaryButton
import com.nestmate.app.ui.components.NestMateTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var name by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var linkedinUrl by remember { mutableStateOf("") }
    var instagramUrl by remember { mutableStateOf("") }
    var courseOrDepartment by remember { mutableStateOf("") }
    var yearOfStudy by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }

    // Privacy
    var phoneVisibility by remember { mutableStateOf("ONLY_ME") }
    var socialVisibility by remember { mutableStateOf("EVERYONE") }
    var budgetVisibility by remember { mutableStateOf("ONLY_ME") }

    var isInitialized by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(profile) {
        if (!isInitialized && profile != null) {
            val user = profile!!
            name = user.name
            bio = user.bio
            phone = user.phone
            linkedinUrl = user.linkedinUrl ?: ""
            instagramUrl = user.instagramUrl ?: ""
            courseOrDepartment = user.courseOrDepartment
            yearOfStudy = user.yearOfStudy
            age = user.age?.toString() ?: ""
            phoneVisibility = user.phoneVisibility
            socialVisibility = user.socialVisibility
            budgetVisibility = user.budgetVisibility
            isInitialized = true
        }
    }

    LaunchedEffect(updateState) {
        when (updateState) {
            true -> {
                isSaving = false
                snackbarHostState.showSnackbar("Profile updated successfully!")
                viewModel.resetUpdateState()
                onNavigateBack()
            }
            false -> {
                isSaving = false
                snackbarHostState.showSnackbar("Failed to update profile. Please try again.")
                viewModel.resetUpdateState()
            }
            null -> { /* idle */ }
        }
    }

    Scaffold(
        topBar = {
            NestMateTopBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Basic Info", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = bio, onValueChange = { bio = it }, label = { Text("Bio") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            }

            item {
                Text("Academic Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = courseOrDepartment, onValueChange = { courseOrDepartment = it }, label = { Text("Course") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = yearOfStudy, onValueChange = { yearOfStudy = it }, label = { Text("Year (e.g. 2nd)") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("Age") }, modifier = Modifier.fillMaxWidth())
            }

            item {
                Text("Contact & Socials", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = linkedinUrl, onValueChange = { linkedinUrl = it }, label = { Text("LinkedIn URL") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = instagramUrl, onValueChange = { instagramUrl = it }, label = { Text("Instagram Handle") }, modifier = Modifier.fillMaxWidth())
            }

            item {
                Text("Privacy Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                Text("Phone Visibility", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                    listOf("EVERYONE", "CONNECTED_ONLY", "ONLY_ME").forEach { option ->
                        FilterChip(selected = phoneVisibility == option, onClick = { phoneVisibility = option }, label = { Text(option.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) })
                    }
                }
                
                Text("Social Visibility", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                    listOf("EVERYONE", "CONNECTED_ONLY", "ONLY_ME").forEach { option ->
                        FilterChip(selected = socialVisibility == option, onClick = { socialVisibility = option }, label = { Text(option.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) })
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                if (isSaving) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else {
                    NestMatePrimaryButton(
                        text = "Save Changes",
                        enabled = name.isNotBlank(),
                        onClick = {
                            isSaving = true
                            viewModel.updateProfile(
                                mapOf(
                                    "name" to name,
                                    "bio" to bio,
                                    "phone" to phone,
                                    "linkedinUrl" to linkedinUrl,
                                    "instagramUrl" to instagramUrl,
                                    "courseOrDepartment" to courseOrDepartment,
                                    "yearOfStudy" to yearOfStudy,
                                    "age" to age.toIntOrNull(),
                                    "phoneVisibility" to phoneVisibility,
                                    "socialVisibility" to socialVisibility,
                                    "budgetVisibility" to budgetVisibility
                                )
                            )
                        }
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

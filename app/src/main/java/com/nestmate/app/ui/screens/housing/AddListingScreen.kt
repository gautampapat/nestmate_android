package com.nestmate.app.ui.screens.housing

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.auth.FirebaseAuth
import com.nestmate.app.data.model.Listing
import com.nestmate.app.ui.components.NestMatePrimaryButton
import com.nestmate.app.utils.FirebaseConstants
import com.nestmate.app.ui.components.NestMateTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddListingScreen(
    onNavigateBack: () -> Unit,
    viewModel: HousingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var title by remember { mutableStateOf("") }
    var rent by remember { mutableStateOf("") }
    var deposit by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("flat") }
    var isBachelorFriendly by remember { mutableStateOf(false) }
    var isFemaleOnly by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Navigate back after successful submission (state resets to Success with new listings)
    LaunchedEffect(uiState) {
        if (isSubmitting && uiState is HousingState.Success) {
            isSubmitting = false
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            NestMateTopBar(
                title = { Text("Add Listing") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title (e.g. 2BHK near WCE)") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Address") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = rent,
                onValueChange = { rent = it.filter { c -> c.isDigit() } },
                label = { Text("Monthly Rent (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value = deposit,
                onValueChange = { deposit = it.filter { c -> c.isDigit() } },
                label = { Text("Security Deposit (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )


            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isBachelorFriendly, onCheckedChange = { isBachelorFriendly = it })
                Text("Bachelor Friendly", modifier = Modifier.padding(start = 8.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 24.dp)) {
                Checkbox(checked = isFemaleOnly, onCheckedChange = { isFemaleOnly = it })
                Text("Female Only", modifier = Modifier.padding(start = 8.dp))
            }

            if (isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                NestMatePrimaryButton(
                    text = "Submit Listing",
                    enabled = title.isNotBlank() && address.isNotBlank() && rent.isNotBlank(),
                    onClick = {
                        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@NestMatePrimaryButton
                        val listing = Listing(
                            title = title,
                            description = description,
                            address = address,
                            rent = rent.toIntOrNull() ?: 0,
                            deposit = deposit.toIntOrNull() ?: 0,
                            type = type,
                            isBachelorFriendly = isBachelorFriendly,
                            isFemaleOnly = isFemaleOnly,
                            ownerId = userId,
                            collegeId = FirebaseConstants.DEFAULT_COLLEGE_ID,
                            isActive = true
                        )
                        isSubmitting = true
                        // viewModel.addListing(listing) -> Legacy, no longer supported.
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

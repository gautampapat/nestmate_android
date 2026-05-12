package com.nestmate.app.ui.screens.buddy

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nestmate.app.data.model.User
import com.nestmate.app.ui.components.EmptyState
import com.nestmate.app.ui.components.NestMateCard
import com.nestmate.app.ui.components.NestMatePrimaryButton

@Composable
fun BuddyMatchingScreen(
    onNavigateBack: () -> Unit,
    viewModel: BuddyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Verified Seniors", style = MaterialTheme.typography.headlineLarge)

        if (uiState is BuddyState.Success) {
            val seniors = (uiState as BuddyState.Success).availableSeniors
            if (seniors.isEmpty()) {
                EmptyState(title = "No Seniors Found", message = "Check back later!")
            } else {
                LazyColumn(modifier = Modifier.padding(top = 16.dp)) {
                    items(seniors) { senior ->
                        SeniorProfileCard(senior) {
                            viewModel.requestBuddy(senior.userId)
                            onNavigateBack()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SeniorProfileCard(user: User, onRequest: () -> Unit) {
    NestMateCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = user.name, style = MaterialTheme.typography.headlineMedium)
            Text(text = "Year: ${user.year}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Speaks: English, Marathi", color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            NestMatePrimaryButton(text = "Send Request", onClick = onRequest)
        }
    }
}

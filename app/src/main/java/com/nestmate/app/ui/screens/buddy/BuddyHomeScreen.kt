package com.nestmate.app.ui.screens.buddy

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nestmate.app.data.model.BuddyPair
import com.nestmate.app.ui.components.EmptyState
import com.nestmate.app.ui.components.NestMateCard
import com.nestmate.app.ui.components.NestMatePrimaryButton

@Composable
fun BuddyHomeScreen(
    onNavigateToMatching: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    viewModel: BuddyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadBuddyHome()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Buddy System", style = MaterialTheme.typography.headlineLarge)
        
        when (uiState) {
            is BuddyState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is BuddyState.Error -> {
                EmptyState(
                    title = "Connection Error",
                    message = (uiState as BuddyState.Error).message,
                    buttonText = "Retry",
                    onButtonClick = { viewModel.loadBuddyHome() }
                )
            }
            is BuddyState.Success -> {
                val state = uiState as BuddyState.Success
                
                NestMateCard(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Find a Senior Buddy", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = "Connect with seniors to learn the best areas, mess spots, and college hacks.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        NestMatePrimaryButton(text = "Browse Seniors", onClick = onNavigateToMatching)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Text(text = "Active Chats", style = MaterialTheme.typography.headlineMedium)

                if (state.pairs.isEmpty()) {
                    Text(
                        text = "You don't have any active buddies yet.",
                        modifier = Modifier.padding(top = 16.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                } else {
                    LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                        items(state.pairs) { pair ->
                            BuddyChatCard(pair) { onNavigateToChat(pair.pairId) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BuddyChatCard(pair: BuddyPair, onClick: () -> Unit) {
    NestMateCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Chat with Buddy", style = MaterialTheme.typography.headlineMedium)
            Text(text = "Status: ${pair.status}", color = MaterialTheme.colorScheme.primary)
        }
    }
}

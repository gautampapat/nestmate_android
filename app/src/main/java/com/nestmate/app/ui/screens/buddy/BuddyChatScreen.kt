package com.nestmate.app.ui.screens.buddy

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nestmate.app.data.model.BuddyMessage
import com.nestmate.app.ui.components.NestMatePrimaryButton

@Composable
fun BuddyChatScreen(
    pairId: String,
    onNavigateBack: () -> Unit,
    viewModel: BuddyViewModel = hiltViewModel()
) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    var currentText by remember { mutableStateOf("") }
    val userId = viewModel.getCurrentUserId() ?: ""

    LaunchedEffect(pairId) {
        viewModel.observeChat(pairId)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = "Chat", style = MaterialTheme.typography.headlineLarge)
        
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 16.dp),
            reverseLayout = false
        ) {
            items(messages) { msg ->
                val isMe = msg.senderId == userId
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = if (isMe) androidx.compose.foundation.layout.Arrangement.End else androidx.compose.foundation.layout.Arrangement.Start
                ) {
                    Text(
                        text = msg.text,
                        modifier = Modifier
                            .background(
                                color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = currentText,
                onValueChange = { currentText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            NestMatePrimaryButton(
                text = "Send",
                modifier = Modifier.width(80.dp),
                onClick = {
                    if (currentText.isNotBlank()) {
                        viewModel.sendMessage(pairId, currentText)
                        currentText = ""
                    }
                }
            )
        }
    }
}

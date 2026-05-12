package com.nestmate.app.ui.screens.marketplace

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.nestmate.app.data.model.ChatContextType
import com.nestmate.app.ui.components.EmptyState
import com.nestmate.app.ui.components.chat.ChatThreadRow
import com.nestmate.app.ui.components.NestMateTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceChatsListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
) {
    val viewModel: MarketplaceChatViewModel = hiltViewModel()
    val threads by viewModel.threads.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            NestMateTopBar(
                title = { Text("Chats") },
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
        if (threads.isEmpty()) {
            EmptyState(
                title = "No conversations yet",
                message = "Chats with sellers and buyers show up here.",
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(threads, key = { it.id }) { thread ->
                val title = thread.itemTitle.ifBlank {
                    if (thread.contextType == ChatContextType.WANTED) "Wanted request" else "Listing"
                }
                val subtitle = when (thread.contextType) {
                    ChatContextType.WANTED -> "Wanted"
                    ChatContextType.ITEM -> if (thread.isItemSold) "Sold" else "Item"
                }
                ChatThreadRow(
                    title = title,
                    subtitle = subtitle,
                    lastMessage = thread.lastMessage,
                    lastMessageAt = thread.lastMessageAt,
                    isUnread = viewModel.isUnread(thread),
                    onClick = { onNavigateToChat(thread.id) },
                )
            }
        }
    }
}

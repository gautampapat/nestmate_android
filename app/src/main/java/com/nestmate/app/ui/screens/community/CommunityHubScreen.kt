package com.nestmate.app.ui.screens.community

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nestmate.app.data.model.Event
import com.nestmate.app.data.model.ForumPost
import com.nestmate.app.ui.components.EmptyState
import com.nestmate.app.ui.components.NestMatePrimaryButton
import com.nestmate.app.ui.components.NestMateTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityHubScreen(
    onNavigateToPostDetail: (String) -> Unit,
    onNavigateToAddPost: () -> Unit,
    viewModel: CommunityViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Forums", "Local Events")

    Scaffold(
        topBar = {
            NestMateTopBar(
                title = { Text("Community Hub", fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = onNavigateToAddPost,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Post")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Tab(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        text = { 
                            Text(
                                title, 
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            ) 
                        }
                    )
                }
            }

            if (selectedTab == 0) {
                ForumsTab(viewModel, onNavigateToPostDetail)
            } else {
                EventsTab(viewModel)
            }
        }
    }
}

@Composable
fun ForumsTab(viewModel: CommunityViewModel, onNavigateToPostDetail: (String) -> Unit) {
    val state by viewModel.forumState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (state is ForumState.Success) {
            val posts = (state as ForumState.Success).posts
            if (posts.isEmpty()) {
                EmptyState(title = "No Posts", message = "Be the first to start a discussion!")
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
                    // Header Pin
                    item {
                        PinnedAnnouncementCard()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Recent Discussions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                    }
                    
                    items(posts) { post ->
                        ForumPostCardDynamic(post, onUpvote = { viewModel.upvotePost(post.postId) }, onClick = { onNavigateToPostDetail(post.postId) })
                    }
                }
            }
        } else if (state is ForumState.Loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
fun PinnedAnnouncementCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Campaign, contentDescription = "Icon", tint = MaterialTheme.colorScheme.onTertiary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("College Fest '26 Announced!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                Text("Registrations opining next week. Stay tuned.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumPostCardDynamic(post: ForumPost, onUpvote: () -> Unit, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(post.authorName.take(1).uppercase(), color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = post.authorName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Text(text = "2 hours ago", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = post.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (post.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = post.content.take(100) + if (post.content.length > 100) "..." else "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onUpvote, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Upvote", tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = post.upvotes.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Comments", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "${post.commentsCount} Comments", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun EventsTab(viewModel: CommunityViewModel) {
    val state by viewModel.eventsState.collectAsStateWithLifecycle()
    
    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (state is EventsState.Success) {
            val events = (state as EventsState.Success).events
            if (events.isEmpty()) {
                EmptyState(title = "No Events", message = "No upcoming events right now.")
            } else {
                LazyColumn {
                    items(events) { event ->
                        EventCardModern(event)
                    }
                }
            }
        } else if (state is EventsState.Loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
fun EventCardModern(event: Event) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFE3F2FD)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Event, contentDescription = "Icon", tint = Color(0xFF1976D2))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = event.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = event.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surface).padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = "Icon", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${event.rsvpCount} Attending", style = MaterialTheme.typography.labelMedium)
                }
                Text("RSVP NOW", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

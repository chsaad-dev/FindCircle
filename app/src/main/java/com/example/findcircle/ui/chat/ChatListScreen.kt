package com.example.findcircle.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.findcircle.di.ServiceLocator
import com.example.findcircle.domain.model.Chat
import java.text.SimpleDateFormat
import java.util.*
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onChatClick: (String, String) -> Unit, // chatId, otherUserName
    viewModel: ChatListViewModel = viewModel(factory = ChatListViewModelFactory())
) {
    val state by viewModel.state.collectAsState()
    val profileUrls by viewModel.profileUrls.collectAsState()
    val currentUserId = ServiceLocator.auth.currentUser?.uid ?: ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                windowInsets = WindowInsets(0.dp)
            )
        }
    ) { paddingValues ->
        when (val currentState = state) {
            is ChatListState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is ChatListState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error loading chats", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(currentState.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            is ChatListState.Success -> {
                if (currentState.chats.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        com.example.findcircle.ui.components.EmptyState(
                            icon = androidx.compose.material.icons.Icons.Default.Email,
                            title = "No Messages Yet",
                            description = "Start a conversation from a post"
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp) // Padding for bottom bar
                    ) {
                        items(currentState.chats, key = { it.id }) { chat ->
                            val otherUserId = chat.participantIds.firstOrNull { it != currentUserId } ?: ""
                            val profileUrl = profileUrls[otherUserId]
                            
                            ChatListItem(
                                chat = chat,
                                currentUserId = currentUserId,
                                profileImageUrl = profileUrl,
                                onClick = { 
                                    // Find the other user's name
                                    val otherUserId = chat.participantIds.firstOrNull { it != currentUserId } ?: ""
                                    val otherUserName = chat.participantNames[otherUserId] ?: "Unknown User"
                                    onChatClick(chat.id, otherUserName)
                                }
                            )
                            Divider(modifier = Modifier.padding(start = 72.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatListItem(
    chat: Chat,
    currentUserId: String,
    profileImageUrl: String?,
    onClick: () -> Unit
) {
    val otherUserId = chat.participantIds.firstOrNull { it != currentUserId } ?: ""
    val otherUserName = chat.participantNames[otherUserId] ?: "Unknown User"
    
    // Format timestamp
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(chat.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!profileImageUrl.isNullOrEmpty()) {
            AsyncImage(
                model = profileImageUrl,
                contentDescription = "Profile Photo",
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            // Avatar Placeholder person icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile Placeholder",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Message Content
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = otherUserName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chat.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                // Unread Indicator (Placeholder, always showing if there's a message for demo, ideally backed by data)
                if (chat.lastMessage.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(10.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                }
            }
        }
    }
}

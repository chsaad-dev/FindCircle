package com.example.findcircle.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.findcircle.di.ServiceLocator
import com.example.findcircle.domain.model.Chat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onChatClick: (String, String) -> Unit, // chatId, otherUserName
    viewModel: ChatListViewModel = viewModel(factory = ChatListViewModelFactory())
) {
    val state by viewModel.state.collectAsState()
    val currentUserId = ServiceLocator.auth.currentUser?.uid ?: ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
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
                    Text("Error loading chats: ${currentState.message}", color = MaterialTheme.colorScheme.error)
                }
            }
            is ChatListState.Success -> {
                if (currentState.chats.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No messages yet", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(currentState.chats, key = { it.id }) { chat ->
                            ChatListItem(
                                chat = chat,
                                currentUserId = currentUserId,
                                onClick = { 
                                    // Find the other user's name
                                    val otherUserId = chat.participantIds.firstOrNull { it != currentUserId } ?: ""
                                    val otherUserName = chat.participantNames[otherUserId] ?: "Unknown User"
                                    onChatClick(chat.id, otherUserName)
                                }
                            )
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
    onClick: () -> Unit
) {
    val otherUserId = chat.participantIds.firstOrNull { it != currentUserId } ?: ""
    val otherUserName = chat.participantNames[otherUserId] ?: "Unknown User"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = otherUserName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = chat.lastMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

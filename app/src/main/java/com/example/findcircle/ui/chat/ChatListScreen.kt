package com.example.findcircle.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.findcircle.di.ServiceLocator
import com.example.findcircle.domain.model.Chat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onChatClick: (String, String) -> Unit,
    viewModel: ChatListViewModel = viewModel(factory = ChatListViewModelFactory())
) {
    val state by viewModel.state.collectAsState()
    val profileUrls by viewModel.profileUrls.collectAsState()
    val currentUserId = ServiceLocator.auth.currentUser?.uid ?: ""
    var chatToDelete by remember { mutableStateOf<Chat?>(null) }

    if (chatToDelete != null) {
        val otherUserId = chatToDelete?.participantIds?.firstOrNull { it != currentUserId } ?: ""
        val otherUserName = chatToDelete?.participantNames?.get(otherUserId) ?: "this user"

        AlertDialog(
            onDismissRequest = { chatToDelete = null },
            title = { Text("Delete Chat", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    "Are you sure you want to delete your conversation with $otherUserName? This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        chatToDelete?.let { viewModel.deleteChat(it.id) }
                        chatToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { chatToDelete = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Messages",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                windowInsets = WindowInsets(0.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        when (val currentState = state) {
            is ChatListState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is ChatListState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Error loading chats",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            currentState.message,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            is ChatListState.Success -> {
                if (currentState.chats.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.ChatBubbleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No Messages Yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Start a conversation from a post",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
                    ) {
                        items(currentState.chats, key = { it.id }) { chat ->
                            val otherUserId = chat.participantIds.firstOrNull { it != currentUserId } ?: ""
                            val profileUrl = profileUrls[otherUserId]

                            ChatListItem(
                                chat = chat,
                                currentUserId = currentUserId,
                                profileImageUrl = profileUrl,
                                onClick = {
                                    val otherUserId = chat.participantIds.firstOrNull { it != currentUserId } ?: ""
                                    val otherUserName = chat.participantNames[otherUserId] ?: "Unknown User"
                                    onChatClick(chat.id, otherUserName)
                                },
                                onLongClick = {
                                    chatToDelete = chat
                                }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 80.dp, end = 16.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatListItem(
    chat: Chat,
    currentUserId: String,
    profileImageUrl: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val otherUserId = chat.participantIds.firstOrNull { it != currentUserId } ?: ""
    val otherUserName = chat.participantNames[otherUserId] ?: "Unknown User"

    // Relative time
    val timeDiff = System.currentTimeMillis() - chat.timestamp
    val formattedTime = when {
        timeDiff < 60_000L -> "Now"
        timeDiff < 3_600_000L -> "${timeDiff / 60_000L}m"
        timeDiff < 86_400_000L -> "${timeDiff / 3_600_000L}h"
        timeDiff < 604_800_000L -> "${timeDiff / 86_400_000L}d"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(chat.timestamp))
    }

    // Simple unread visual indicator
    val hasUnread = false

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        if (!profileImageUrl.isNullOrEmpty()) {
            AsyncImage(
                model = profileImageUrl,
                contentDescription = "Profile Photo",
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = otherUserName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = otherUserName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (hasUnread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = chat.lastMessage.ifEmpty { "No messages yet" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (hasUnread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (hasUnread) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (hasUnread) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                }
            }
        }
    }
}

package com.example.findcircle.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.findcircle.domain.model.Message
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatMessageScreen(
    chatId: String,
    otherUserName: String,
    onNavigateBack: () -> Unit,
    viewModel: ChatMessageViewModel = viewModel(factory = ChatMessageViewModelFactory(chatId))
) {
    val state by viewModel.state.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(otherUserName) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .navigationBarsPadding()
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message") },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                viewModel.sendMessage(messageText)
                                messageText = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(24.dp)
                            )
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Send",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        when (val currentState = state) {
            is MessageState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is MessageState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error loading messages: ${currentState.message}", color = MaterialTheme.colorScheme.error)
                }
            }
            is MessageState.Success -> {
                // Auto scroll to bottom when new messages arrive
                LaunchedEffect(currentState.messages.size) {
                    if (currentState.messages.isNotEmpty()) {
                        coroutineScope.launch {
                            listState.animateScrollToItem(currentState.messages.size - 1)
                        }
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(currentState.messages, key = { it.id }) { message ->
                        val isCurrentUser = message.senderId == viewModel.currentUserId
                        MessageBubble(message = message, isCurrentUser = isCurrentUser)
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message, isCurrentUser: Boolean) {
    val backgroundColor = if (isCurrentUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val textColor = if (isCurrentUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val alignment = if (isCurrentUser) Alignment.End else Alignment.Start
    val shape = if (isCurrentUser) {
        RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
    }

    val dateFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val timeString = dateFormatter.format(Date(message.timestamp))

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .background(backgroundColor, shape)
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = message.text,
                    color = textColor,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = timeString,
                    color = textColor.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

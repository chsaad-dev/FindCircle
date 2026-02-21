package com.example.findcircle.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.findcircle.data.repository.ChatRepository
import com.example.findcircle.di.ServiceLocator
import com.example.findcircle.domain.model.Chat
import com.example.findcircle.domain.model.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

// --- Chat List ViewModel ---
sealed class ChatListState {
    object Loading : ChatListState()
    data class Success(val chats: List<Chat>) : ChatListState()
    data class Error(val message: String) : ChatListState()
}

class ChatListViewModel(
    private val chatRepository: ChatRepository = ServiceLocator.chatRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ChatListState>(ChatListState.Loading)
    val state: StateFlow<ChatListState> = _state.asStateFlow()

    init {
        val currentUserId = ServiceLocator.auth.currentUser?.uid
        if (currentUserId != null) {
            viewModelScope.launch {
                chatRepository.getUserChats(currentUserId)
                    .catch { e ->
                        _state.value = ChatListState.Error(e.message ?: "Failed to load chats")
                    }
                    .collect { chats ->
                        _state.value = ChatListState.Success(chats)
                    }
            }
        } else {
            _state.value = ChatListState.Error("User not logged in")
        }
    }
}

// --- Specific Chat Message ViewModel ---
sealed class MessageState {
    object Loading : MessageState()
    data class Success(val messages: List<Message>) : MessageState()
    data class Error(val message: String) : MessageState()
}

class ChatMessageViewModel(
    private val chatId: String,
    private val chatRepository: ChatRepository = ServiceLocator.chatRepository
) : ViewModel() {

    private val _state = MutableStateFlow<MessageState>(MessageState.Loading)
    val state: StateFlow<MessageState> = _state.asStateFlow()
    
    val currentUserId = ServiceLocator.auth.currentUser?.uid ?: ""

    init {
        viewModelScope.launch {
            chatRepository.getChatMessages(chatId)
                .catch { e ->
                    _state.value = MessageState.Error(e.message ?: "Failed to load messages")
                }
                .collect { messages ->
                    _state.value = MessageState.Success(messages)
                }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        
        viewModelScope.launch {
            chatRepository.sendMessage(
                chatId = chatId,
                senderId = currentUserId,
                text = text
            )
        }
    }
}

// --- Factories ---
class ChatListViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatListViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class ChatMessageViewModelFactory(private val chatId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatMessageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatMessageViewModel(chatId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

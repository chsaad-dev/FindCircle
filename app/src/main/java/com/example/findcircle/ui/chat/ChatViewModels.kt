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
import kotlinx.coroutines.tasks.await

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

    private val _profileUrls = MutableStateFlow<Map<String, String>>(emptyMap())
    val profileUrls: StateFlow<Map<String, String>> = _profileUrls.asStateFlow()
    
    private val fetchedProfileIds = mutableSetOf<String>()

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
                        chats.forEach { chat ->
                            val otherUserId = chat.participantIds.firstOrNull { it != currentUserId }
                            if (otherUserId != null) {
                                fetchProfileUrl(otherUserId)
                            }
                        }
                    }
            }
        } else {
            _state.value = ChatListState.Error("User not logged in")
        }
    }

    private fun fetchProfileUrl(userId: String) {
        if (fetchedProfileIds.contains(userId)) return
        fetchedProfileIds.add(userId)
        
        viewModelScope.launch {
            try {
                val doc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .get()
                    .await()
                val url = doc.getString("profileImageUrl")
                if (url != null) {
                    _profileUrls.value = _profileUrls.value + (userId to url)
                }
            } catch (e: Exception) {
                fetchedProfileIds.remove(userId)
            }
        }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            chatRepository.deleteSingleChat(chatId)
        }
    }
}

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
    
    private val _otherUserProfileUrl = MutableStateFlow<String?>(null)
    val otherUserProfileUrl: StateFlow<String?> = _otherUserProfileUrl.asStateFlow()
    
    private val _showVerifyOwnership = MutableStateFlow(false)
    val showVerifyOwnership: StateFlow<Boolean> = _showVerifyOwnership.asStateFlow()
    
    val currentUserId = ServiceLocator.auth.currentUser?.uid ?: ""

    init {
        loadOtherUserProfile()
        checkIfUserIsFinder()
        
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

    private fun loadOtherUserProfile() {
        viewModelScope.launch {
            try {
                val chatDoc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("chats")
                    .document(chatId)
                    .get()
                    .await()
                
                val participantIds = chatDoc.get("participantIds") as? List<String> ?: return@launch
                val otherUserId = participantIds.firstOrNull { it != currentUserId } ?: return@launch
                
                val userDoc = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(otherUserId)
                    .get()
                    .await()
                    
                _otherUserProfileUrl.value = userDoc.getString("profileImageUrl")
            } catch (e: Exception) {
            }
        }
    }

    private fun checkIfUserIsFinder() {
        viewModelScope.launch {
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val chatDoc = db.collection("chats").document(chatId).get().await()
                
                val postId = chatDoc.getString("postId")
                if (postId.isNullOrEmpty()) {
                    _showVerifyOwnership.value = false
                    return@launch
                }
                val postDoc = db.collection("posts").document(postId).get().await()
                val post = postDoc.toObject(com.example.findcircle.domain.model.Post::class.java)
                if (post != null) {
                    _showVerifyOwnership.value = (
                        post.ownerId == currentUserId &&
                        post.type == com.example.findcircle.domain.model.PostType.FOUND &&
                        post.status == com.example.findcircle.domain.model.PostStatus.OPEN &&
                        post.secretQuestion.isNotBlank()
                    )
                } else {
                    _showVerifyOwnership.value = false
                }
            } catch (e: Exception) {
                // Ignore
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

    fun sendOwnershipChallenge() {
        viewModelScope.launch {
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val chatDoc = db.collection("chats").document(chatId).get().await()
                
                val postId = chatDoc.getString("postId") ?: return@launch
                val postDoc = db.collection("posts").document(postId).get().await()
                val post = postDoc.toObject(com.example.findcircle.domain.model.Post::class.java)
                
                val question = post?.secretQuestion
                if (!question.isNullOrBlank()) {
                    sendMessage("🔒 To verify ownership, please answer this secret question:\n$question")
                } else {
                    sendMessage("🔒 Please verify ownership to proceed.")
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun submitRatingForOtherUser(rating: Int) {
        viewModelScope.launch {
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val chatDoc = db.collection("chats").document(chatId).get().await()
                
                val participantIds = chatDoc.get("participantIds") as? List<String> ?: return@launch
                val otherUserId = participantIds.firstOrNull { it != currentUserId } ?: return@launch
                
                val userRef = db.collection("users").document(otherUserId)

                db.runTransaction { transaction ->
                    val snapshot = transaction.get(userRef)
                    val currentRating = snapshot.getDouble("rating") ?: 0.0
                    val currentCount = snapshot.getLong("ratingCount") ?: 0L
                    
                    val newCount = currentCount + 1
                    val newRating = ((currentRating * currentCount) + rating) / newCount
                    
                    transaction.update(userRef, "rating", newRating)
                    transaction.update(userRef, "ratingCount", newCount)
                    null
                }.await()
            } catch (e: Exception) {
                // Background execution, safe to ignore UI notification for this demo since there's no UI listener for rating success attached yet
            }
        }
    }
}

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

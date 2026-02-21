package com.example.findcircle.ui.postdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.findcircle.data.repository.ChatRepository
import com.example.findcircle.data.repository.PostRepository
import com.example.findcircle.di.ServiceLocator
import com.example.findcircle.domain.model.Comment
import com.example.findcircle.domain.model.Post
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed class PostDetailState {
    object Loading : PostDetailState()
    data class Success(val post: Post, val comments: List<Comment>) : PostDetailState()
    data class Error(val message: String) : PostDetailState()
}

class PostDetailViewModel(
    private val postId: String,
    private val postRepository: PostRepository = ServiceLocator.postRepository,
    private val chatRepository: ChatRepository = ServiceLocator.chatRepository
) : ViewModel() {

    private val _state = MutableStateFlow<PostDetailState>(PostDetailState.Loading)
    val state: StateFlow<PostDetailState> = _state.asStateFlow()
    
    val currentUserId = ServiceLocator.auth.currentUser?.uid ?: ""
    val currentUserName = ServiceLocator.auth.currentUser?.displayName ?: ServiceLocator.auth.currentUser?.email ?: "Unknown User"

    private var currentPost: Post? = null

    init {
        loadPostAndComments()
    }

    private fun loadPostAndComments() {
        viewModelScope.launch {
            _state.value = PostDetailState.Loading
            
            val postResult = postRepository.getPostById(postId)
            if (postResult.isSuccess) {
                currentPost = postResult.getOrNull()
                
                if (currentPost != null) {
                    // Start observing comments
                    postRepository.getPostComments(postId)
                        .catch { e ->
                            _state.value = PostDetailState.Error(e.message ?: "Failed to load comments")
                        }
                        .collect { comments ->
                            _state.value = PostDetailState.Success(currentPost!!, comments)
                        }
                } else {
                    _state.value = PostDetailState.Error("Post not found")
                }
            } else {
                _state.value = PostDetailState.Error(postResult.exceptionOrNull()?.message ?: "Failed to load post")
            }
        }
    }

    fun addComment(text: String) {
        if (text.isBlank()) return
        
        viewModelScope.launch {
            postRepository.addComment(
                postId = postId,
                text = text,
                authorId = currentUserId,
                authorName = currentUserName
            )
        }
    }

    fun createOrGetChat(otherUserId: String, otherUserName: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val result = chatRepository.createOrGetChat(otherUserId, otherUserName)
            result.onSuccess { chatId ->
                onResult(chatId)
            }.onFailure {
                onResult(null)
            }
        }
    }
}

class PostDetailViewModelFactory(private val postId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PostDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PostDetailViewModel(
                postId = postId,
                postRepository = ServiceLocator.postRepository,
                chatRepository = ServiceLocator.chatRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

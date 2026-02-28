// HistoryViewModel.kt
package com.example.findcircle.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.findcircle.data.repository.AuthRepository
import com.example.findcircle.data.repository.PostRepository
import com.example.findcircle.di.ServiceLocator
import com.example.findcircle.domain.model.Post
import com.example.findcircle.domain.model.PostStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class HistoryState {
    object Loading : HistoryState()
    data class Success(val posts: List<Post>) : HistoryState()
    data class Error(val message: String) : HistoryState()
}

class HistoryViewModel(
    private val postRepository: PostRepository = ServiceLocator.postRepository,
    private val authRepository: AuthRepository = ServiceLocator.authRepository
) : ViewModel() {

    private val _state = MutableStateFlow<HistoryState>(HistoryState.Loading)
    val state: StateFlow<HistoryState> = _state.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _state.value = HistoryState.Loading
            val userId = authRepository.getCurrentUserId()
            if (userId != null) {
                val result = postRepository.getPostsByOwnerId(userId)
                if (result.isSuccess) {
                    _state.value = HistoryState.Success(result.getOrNull() ?: emptyList())
                } else {
                    _state.value = HistoryState.Error("Failed to load history")
                }
            } else {
                _state.value = HistoryState.Error("User not logged in")
            }
        }
    }

    fun markAsResolved(postId: String) {
        viewModelScope.launch {
            val result = postRepository.updatePostStatus(postId, PostStatus.RESOLVED)
            if (result.isSuccess) {
                loadHistory() // Reload the list
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            val result = postRepository.deletePost(postId)
            if (result.isSuccess) {
                loadHistory()
            }
        }
    }
}

class HistoryViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

package com.example.findcircle.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.findcircle.data.repository.PostRepository
import com.example.findcircle.di.ServiceLocator
import com.example.findcircle.domain.model.Post
import com.example.findcircle.domain.model.PostStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class HomeState {
    object Loading : HomeState()
    data class Success(val posts: List<Post>) : HomeState()
    data class Error(val message: String) : HomeState()
}

class HomeViewModel(
    private val postRepository: PostRepository = ServiceLocator.postRepository
) : ViewModel() {

    private val _state = MutableStateFlow<HomeState>(HomeState.Loading)
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        fetchPosts()
    }

    fun fetchPosts() {
        _state.value = HomeState.Loading
        viewModelScope.launch {
            val result = postRepository.getRecentPosts()
            result.onSuccess { posts ->
                val openPosts = posts.filter { it.status == PostStatus.OPEN }
                _state.value = HomeState.Success(openPosts)
            }.onFailure { e ->
                _state.value = HomeState.Error(e.message ?: "Failed to fetch posts")
            }
        }
    }
}

class HomeViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

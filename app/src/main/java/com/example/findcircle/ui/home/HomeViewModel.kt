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
import kotlinx.coroutines.tasks.await

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
    
    private val _isCircleMode = MutableStateFlow(false)
    val isCircleMode: StateFlow<Boolean> = _isCircleMode.asStateFlow()
    
    private var currentUserNeighborhood: String = ""

    init {
        fetchPosts()
    }

    fun toggleCircleMode(enabled: Boolean) {
        _isCircleMode.value = enabled
        fetchPosts()
    }

    fun fetchPosts() {
        _state.value = HomeState.Loading
        viewModelScope.launch {
            if (currentUserNeighborhood.isEmpty() && _isCircleMode.value) {
                try {
                    val uid = ServiceLocator.auth.currentUser?.uid
                    if (uid != null) {
                        val userDoc = ServiceLocator.firestore.collection("users").document(uid).get().await()
                        currentUserNeighborhood = userDoc.getString("neighborhood") ?: ""
                    }
                } catch (e: Exception) {
                }
            }
            
            val result = postRepository.getRecentPosts()
            result.onSuccess { posts ->
                val openPosts = posts.filter { 
                    it.status == PostStatus.OPEN && 
                    (!_isCircleMode.value || it.neighborhood.equals(currentUserNeighborhood, ignoreCase = true))
                }
                
                val currentTime = System.currentTimeMillis()
                val fortyEightHoursInMillis = 48L * 60L * 60L * 1000L
                
                val sortedPosts = openPosts.sortedWith(Comparator { p1, p2 ->
                    val p1IsRecentUrgent = p1.isUrgent && (currentTime - p1.timestamp <= fortyEightHoursInMillis)
                    val p2IsRecentUrgent = p2.isUrgent && (currentTime - p2.timestamp <= fortyEightHoursInMillis)
                    
                    if (p1IsRecentUrgent && !p2IsRecentUrgent) {
                        return@Comparator -1
                    } else if (!p1IsRecentUrgent && p2IsRecentUrgent) {
                        return@Comparator 1
                    } else {
                        return@Comparator p2.timestamp.compareTo(p1.timestamp)
                    }
                })

                _state.value = HomeState.Success(sortedPosts)
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

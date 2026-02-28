package com.example.findcircle.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.findcircle.data.repository.AuthRepository
import com.example.findcircle.data.repository.ImageRepository
import com.example.findcircle.di.ServiceLocator
import com.example.findcircle.domain.model.User
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

import com.example.findcircle.data.repository.ChatRepository
import com.example.findcircle.data.repository.PostRepository
import com.example.findcircle.domain.model.PostStatus
import com.example.findcircle.domain.model.PostType

data class ProfileStats(
    val totalPosts: Int = 0,
    val itemsFound: Int = 0,
    val successfulMatches: Int = 0
)

sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(val user: User?, val stats: ProfileStats) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

class ProfileViewModel(
    private val authRepository: AuthRepository = ServiceLocator.authRepository,
    private val imageRepository: ImageRepository = ServiceLocator.imageRepository,
    private val postRepository: PostRepository = ServiceLocator.postRepository,
    private val chatRepository: ChatRepository = ServiceLocator.chatRepository
) : ViewModel() {

    private val _state = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _state.value = ProfileState.Loading
            try {

                val currentUserId = authRepository.getCurrentUserId()
                if (currentUserId != null) {
                    val snapshot = ServiceLocator.firestore.collection("users").document(currentUserId).get().await()
                    val user = snapshot.toObject(User::class.java)
                    
                    var stats = ProfileStats()
                    val postsResult = postRepository.getPostsByOwnerId(currentUserId)
                    if (postsResult.isSuccess) {
                        val posts = postsResult.getOrNull() ?: emptyList()
                        val total = posts.size
                        val found = posts.count { it.type == PostType.FOUND }
                        val matches = posts.count { it.status == PostStatus.RESOLVED }
                        stats = ProfileStats(totalPosts = total, itemsFound = found, successfulMatches = matches)
                    }

                    _state.value = ProfileState.Success(user, stats)
                } else {
                    _state.value = ProfileState.Error("User not logged in")
                }
            } catch (e: Exception) {
                 _state.value = ProfileState.Error(e.message ?: "Failed to load profile")
            }
        }
    }
    
    fun logout() {
        authRepository.logout()
    }
    
    fun uploadAvatar(uri: Uri) {
        viewModelScope.launch {
            val currentUserId = authRepository.getCurrentUserId() ?: return@launch
            _state.value = ProfileState.Loading
            val result = imageRepository.uploadImage(uri, "avatars")
            
            result.onSuccess { downloadUrl ->
                try {
                    ServiceLocator.firestore.collection("users").document(currentUserId)
                        .update("profileImageUrl", downloadUrl).await()
                    
                    // Reload profile
                    loadUserProfile()
                } catch (e: Exception) {
                    _state.value = ProfileState.Error("Failed to update profile image")
                }
            }.onFailure {
                _state.value = ProfileState.Error("Failed to upload image")
            }
        }
    }

    fun uploadCoverImage(uri: Uri) {
        viewModelScope.launch {
            val currentUserId = authRepository.getCurrentUserId() ?: return@launch
            _state.value = ProfileState.Loading
            val result = imageRepository.uploadImage(uri, "covers")
            
            result.onSuccess { downloadUrl ->
                try {
                    ServiceLocator.firestore.collection("users").document(currentUserId)
                        .update("coverImageUrl", downloadUrl).await()
                    
                    loadUserProfile()
                } catch (e: Exception) {
                    _state.value = ProfileState.Error("Failed to update cover image")
                }
            }.onFailure {
                _state.value = ProfileState.Error("Failed to upload cover image")
            }
        }
    }

    fun deleteAccount(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val currentUserId = authRepository.getCurrentUserId()
            if (currentUserId == null) {
                onError("User not logged in")
                return@launch
            }
            try {
                // 1. Delete all posts owned by user
                val postsResult = postRepository.getPostsByOwnerId(currentUserId)
                if (postsResult.isSuccess) {
                    val posts = postsResult.getOrNull() ?: emptyList()
                    for (post in posts) {
                        postRepository.deletePost(post.id)
                    }
                }
                
                chatRepository.deleteChatsForUser(currentUserId)
                
                val userSnapshot = ServiceLocator.firestore.collection("users").document(currentUserId).get().await()
                val userToEdit = userSnapshot.toObject(User::class.java)
                if (userToEdit != null) {
                    if (userToEdit.profileImageUrl.isNotEmpty()) {
                        imageRepository.deleteImage(userToEdit.profileImageUrl)
                    }
                    if (userToEdit.coverImageUrl.isNotEmpty()) {
                        imageRepository.deleteImage(userToEdit.coverImageUrl)
                    }
                }
                
                ServiceLocator.firestore.collection("users").document(currentUserId).delete().await()
                
                val authResult = authRepository.deleteAccount()
                if (authResult.isSuccess) {
                    onSuccess()
                } else {
                    authRepository.logout()
                    onSuccess()
                }
            } catch (e: Exception) {
                onError(e.message ?: "Failed to delete account")
            }
        }
    }

    fun updateFullProfile(name: String, bio: String, phone: String, neighborhood: String) {
        viewModelScope.launch {
            val currentUserId = authRepository.getCurrentUserId() ?: return@launch
            try {
                ServiceLocator.firestore.collection("users").document(currentUserId)
                    .update(
                        mapOf(
                            "name" to name,
                            "bio" to bio,
                            "phone" to phone,
                            "neighborhood" to neighborhood
                        )
                    ).await()
                loadUserProfile()
            } catch (e: Exception) {
                _state.value = ProfileState.Error("Failed to update profile")
            }
        }
    }
}

class ProfileViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}



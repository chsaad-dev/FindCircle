package com.example.findcircle.ui.addpost

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.findcircle.data.repository.ImageRepository
import com.example.findcircle.data.repository.PostRepository
import com.example.findcircle.di.ServiceLocator
import com.example.findcircle.domain.model.Post
import com.example.findcircle.domain.model.PostStatus
import com.example.findcircle.domain.model.PostType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AddPostState {
    object Idle : AddPostState()
    object Loading : AddPostState()
    object Success : AddPostState()
    data class Error(val message: String) : AddPostState()
}

class AddPostViewModel(
    private val postRepository: PostRepository = ServiceLocator.postRepository,
    private val imageRepository: ImageRepository = ServiceLocator.imageRepository
) : ViewModel() {

    private val _state = MutableStateFlow<AddPostState>(AddPostState.Idle)
    val state: StateFlow<AddPostState> = _state.asStateFlow()

    fun createPost(
        title: String,
        description: String,
        category: String,
        type: PostType,
        latitude: Double,
        longitude: Double,
        imageUri: Uri?
    ) {
        if (title.isBlank() || description.isBlank()) {
            _state.value = AddPostState.Error("Title and description are required")
            return
        }

        _state.value = AddPostState.Loading

        viewModelScope.launch {
            try {
                // Get the current user info
                val currentUser = ServiceLocator.auth.currentUser
                if (currentUser == null) {
                    _state.value = AddPostState.Error("User not logged in")
                    return@launch
                }

                var imageUrl = ""
                if (imageUri != null) {
                    val result = imageRepository.uploadImage(imageUri, "posts")
                    if (result.isSuccess) {
                        imageUrl = result.getOrNull() ?: ""
                    } else {
                        _state.value = AddPostState.Error("Image upload failed")
                        return@launch
                    }
                }

                val post = Post(
                    ownerId = currentUser.uid,
                    ownerName = currentUser.displayName ?: currentUser.email ?: "Unknown User",
                    title = title,
                    description = description,
                    category = category,
                    type = type,
                    status = PostStatus.OPEN,
                    imageUrl = imageUrl,
                    latitude = latitude,
                    longitude = longitude
                )

                val createResult = postRepository.createPost(post)
                if (createResult.isSuccess) {
                    _state.value = AddPostState.Success
                } else {
                    _state.value = AddPostState.Error(
                        createResult.exceptionOrNull()?.message ?: "Failed to create post"
                    )
                }
            } catch (e: Exception) {
                _state.value = AddPostState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }
    
    fun resetState() {
        _state.value = AddPostState.Idle
    }
}

class AddPostViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddPostViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddPostViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

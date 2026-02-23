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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import android.content.Context
import android.util.Log

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

    private val _tags = MutableStateFlow<List<String>>(emptyList())
    val tags: StateFlow<List<String>> = _tags.asStateFlow()

    fun removeTag(tag: String) {
        _tags.value = _tags.value - tag
    }

    fun analyzeImage(context: Context, uri: Uri) {
        try {
            val image = InputImage.fromFilePath(context, uri)
            val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
            
            labeler.process(image)
                .addOnSuccessListener { labels ->
                    val highConfidenceTags = labels
                        .filter { it.confidence > 0.7f }
                        .map { it.text }
                        .take(5) // Max 5 tags

                    // Add new unique tags
                    val currentTags = _tags.value
                    val merged = (currentTags + highConfidenceTags).distinct()
                    _tags.value = merged
                }
                .addOnFailureListener { e ->
                    Log.e("AddPostViewModel", "ML Kit Error: \${e.localizedMessage}")
                }
        } catch (e: Exception) {
            Log.e("AddPostViewModel", "Image Processing Error: \${e.localizedMessage}")
        }
    }

    fun createPost(
        title: String,
        description: String,
        category: String,
        type: PostType,
        latitude: Double,
        longitude: Double,
        imageUri: Uri?,
        dateReported: Long,
        tags: List<String>
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
                    longitude = longitude,
                    dateReported = dateReported,
                    tags = tags
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
        _tags.value = emptyList()
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

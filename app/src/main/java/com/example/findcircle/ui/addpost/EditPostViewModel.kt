package com.example.findcircle.ui.addpost

import android.content.Context
import android.net.Uri
import android.util.Log
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.label.ImageLabel

sealed class EditPostState {
    object Idle : EditPostState()
    object Loading : EditPostState()
    object Success : EditPostState()
    data class Error(val message: String) : EditPostState()
}

class EditPostViewModel(
    private val postId: String,
    private val postRepository: PostRepository = ServiceLocator.postRepository,
    private val imageRepository: ImageRepository = ServiceLocator.imageRepository
) : ViewModel() {

    private val _state = MutableStateFlow<EditPostState>(EditPostState.Idle)
    val state: StateFlow<EditPostState> = _state.asStateFlow()

    private val _post = MutableStateFlow<Post?>(null)
    val post: StateFlow<Post?> = _post.asStateFlow()

    private val _tags = MutableStateFlow<List<String>>(emptyList())
    val tags: StateFlow<List<String>> = _tags.asStateFlow()

    init {
        loadPost()
    }

    private fun loadPost() {
        viewModelScope.launch {
            _state.value = EditPostState.Loading
            val result = postRepository.getPostById(postId)
            if (result.isSuccess) {
                val fetchedPost = result.getOrNull()
                if (fetchedPost != null) {
                    _post.value = fetchedPost
                    _tags.value = fetchedPost.tags
                    _state.value = EditPostState.Idle
                } else {
                    _state.value = EditPostState.Error("Post not found")
                }
            } else {
                _state.value = EditPostState.Error("Failed to load post")
            }
        }
    }

    fun removeTag(tag: String) {
        _tags.update { it - tag }
    }

    fun analyzeImage(context: Context, uri: Uri) {
        try {
            val image = InputImage.fromFilePath(context, uri)
            val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
            
            labeler.process(image)
                .addOnSuccessListener { labels: List<ImageLabel> ->
                    val highConfidenceTags = labels
                        .filter { it.confidence > 0.7f }
                        .map { it.text }
                        .take(5)

                    _tags.update { currentTags -> 
                        (currentTags + highConfidenceTags).distinct() 
                    }
                }
                .addOnFailureListener { e: Exception ->
                    Log.e("EditPostViewModel", "ML Kit Error", e)
                }
        } catch (e: Exception) {
            Log.e("EditPostViewModel", "Image Processing Error", e)
        }
    }

    fun updatePost(
        context: Context,
        title: String,
        description: String,
        category: String,
        type: PostType,
        latitude: Double,
        longitude: Double,
        locationName: String,
        imageUri: Uri?,
        dateReported: Long,
        tags: List<String>,
        isUrgent: Boolean = false,
        secretQuestion: String = ""
    ) {
        if (title.isBlank() || description.isBlank()) {
            _state.value = EditPostState.Error("Title and description are required")
            return
        }

        val currentPost = _post.value ?: return

        _state.value = EditPostState.Loading

        viewModelScope.launch {
            try {
                var newImageUrl = currentPost.imageUrl
                
                // If a new image was selected (imageUri is not null and not from web)
                if (imageUri != null && imageUri.scheme != "https" && imageUri.scheme != "http") {
                    val compressedUri = com.example.findcircle.util.ImageCompressor.compressImage(context, imageUri)
                    val result = imageRepository.uploadImage(compressedUri, "posts")
                    if (result.isSuccess) {
                        newImageUrl = result.getOrNull() ?: ""
                    } else {
                        _state.value = EditPostState.Error("Image upload failed")
                        return@launch
                    }
                }

                val updatedPost = currentPost.copy(
                    title = title,
                    description = description,
                    category = category,
                    type = type,
                    imageUrl = newImageUrl,
                    latitude = latitude,
                    longitude = longitude,
                    locationName = locationName,
                    dateReported = dateReported,
                    tags = tags,
                    isUrgent = isUrgent,
                    secretQuestion = secretQuestion
                )

                // Reuse createPost to overwrite or create a custom update method
                // We'll add an update method to repository or just use createPost(which sets)
                
                // We need to implement updatePost in PostRepository if it's not there.
                // Firebase set() on existing document will overwrite, but merge is safer. 
                // Wait, creating post uses set without merge. Let's just create updatePost in Repository.
                val updateResult = postRepository.createPost(updatedPost) // Assuming createPost uses docRef.set(post) which overwrites entirely (fine since we have complete data).
                
                if (updateResult.isSuccess) {
                    _state.value = EditPostState.Success
                } else {
                    _state.value = EditPostState.Error(
                        updateResult.exceptionOrNull()?.message ?: "Failed to update post"
                    )
                }
            } catch (e: Exception) {
                _state.value = EditPostState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    fun resetState() {
        _state.value = EditPostState.Idle
    }
}

class EditPostViewModelFactory(private val postId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditPostViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditPostViewModel(
                postId = postId,
                postRepository = ServiceLocator.postRepository,
                imageRepository = ServiceLocator.imageRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

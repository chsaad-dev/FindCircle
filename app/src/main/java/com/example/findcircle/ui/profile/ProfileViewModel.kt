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

sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(val user: User?) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

class ProfileViewModel(
    private val authRepository: AuthRepository = ServiceLocator.authRepository,
    private val imageRepository: ImageRepository = ServiceLocator.imageRepository
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
                // In a perfect world we would fetch this from Firestore using getCurrentUserId()
                // and a method in AuthRepository: fetchUser(uid)
                // For now, since AuthRepository handles auth, we'll get basic info and fake the rest if needed,
                // or assume User is available.
                
                // Let's implement a quick fetch from Firestore
                val currentUserId = authRepository.getCurrentUserId()
                if (currentUserId != null) {
                    val snapshot = ServiceLocator.firestore.collection("users").document(currentUserId).get().await()
                    val user = snapshot.toObject(User::class.java)
                    _state.value = ProfileState.Success(user)
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

    fun updateProfile(name: String, neighborhood: String) {
        viewModelScope.launch {
            val currentUserId = authRepository.getCurrentUserId() ?: return@launch
            try {
                ServiceLocator.firestore.collection("users").document(currentUserId)
                    .update(
                        mapOf(
                            "name" to name,
                            "neighborhood" to neighborhood
                        )
                    ).await()
                loadUserProfile()
            } catch (e: Exception) {
                // Ignore or handle
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



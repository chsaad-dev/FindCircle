package com.example.findcircle.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.findcircle.data.repository.AuthRepository
import com.example.findcircle.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(
    private val authRepository: AuthRepository = ServiceLocator.authRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<AuthState>(AuthState.Idle)
    val loginState: StateFlow<AuthState> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow<AuthState>(AuthState.Idle)
    val registerState: StateFlow<AuthState> = _registerState.asStateFlow()
    
    val isUserLoggedIn: Boolean
        get() = authRepository.getCurrentUserId() != null

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _loginState.value = AuthState.Error("Email and password cannot be empty")
            return
        }
        
        _loginState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authRepository.login(email, pass)
            result.onSuccess {
                _loginState.value = AuthState.Success
            }.onFailure { e ->
                _loginState.value = AuthState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        _loginState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(idToken)
            result.onSuccess {
                _loginState.value = AuthState.Success
            }.onFailure { e ->
                _loginState.value = AuthState.Error(e.message ?: "Google Sign In failed")
            }
        }
    }

    fun register(name: String, email: String, pass: String, neighborhood: String) {
        if (name.isBlank() || email.isBlank() || pass.isBlank()) {
            _registerState.value = AuthState.Error("Please fill in all required fields")
            return
        }
        
        _registerState.value = AuthState.Loading
        viewModelScope.launch {
            val result = authRepository.register(name, email, pass, neighborhood)
            result.onSuccess {
                _registerState.value = AuthState.Success
            }.onFailure { e ->
                _registerState.value = AuthState.Error(e.message ?: "Registration failed")
            }
        }
    }
    
    fun resetStates() {
        _loginState.value = AuthState.Idle
        _registerState.value = AuthState.Idle
    }
}

// Simple Factory for manual DI
class AuthViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

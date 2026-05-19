package com.example.stylemate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.stylemate.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    data class AuthUiState(
        val isLoading: Boolean = true,
        val isLoggedIn: Boolean = false,
        val errorMessage: String? = null
    )

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.isLoggedInFlow.collect { loggedIn ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoggedIn = loggedIn
                )
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = repository.login(email, password)
            _uiState.value = if (result.isSuccess) {
                _uiState.value.copy(isLoading = false, isLoggedIn = true, errorMessage = null)
            } else {
                _uiState.value.copy(
                    isLoading = false,
                    isLoggedIn = false,
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repository.logout()
            _uiState.value = _uiState.value.copy(isLoading = false, isLoggedIn = false)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

class AuthViewModelFactory(
    private val repository: AuthRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}


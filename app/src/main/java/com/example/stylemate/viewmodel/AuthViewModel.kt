package com.example.stylemate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.stylemate.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * MVVM ViewModel cho màn hình Login/SignUp.
 *
 * ViewModel KHÔNG phụ thuộc vào Android Context.
 * Tất cả strings được render ở tầng Composable thông qua stringResource().
 *
 * [LoginUiState] dùng sealed interface để ép kiểu an toàn.
 */
class AuthViewModel(
    private val repository: AuthRepository
) : ViewModel() {

    // ── Sealed Interface UI State ─────────────────────────────────
    sealed interface LoginUiState {
        /** Đang kiểm tra session */
        data object Loading : LoginUiState

        /** Form đăng nhập/đăng ký */
        data class Form(
            val email: String = "",
            val password: String = "",
            val confirmPassword: String = "",
            val isRegisterMode: Boolean = false,
            val passwordVisible: Boolean = false,
            val isLoading: Boolean = false,
            val errorMessage: String? = null,
            val validationError: String? = null
        ) : LoginUiState {
            val isPasswordMismatch: Boolean
                get() = isRegisterMode && confirmPassword.isNotBlank() && confirmPassword != password

            val isFormValid: Boolean
                get() = email.isNotBlank() && password.isNotBlank() &&
                        (!isRegisterMode || confirmPassword.isNotBlank()) &&
                        !isPasswordMismatch
        }

        /** Đã đăng nhập thành công */
        data object LoggedIn : LoginUiState
    }

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Loading)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        // Kiểm tra session tồn tại khi khởi tạo
        viewModelScope.launch {
            repository.isLoggedInFlow.collect { loggedIn ->
                _uiState.value = if (loggedIn) {
                    LoginUiState.LoggedIn
                } else {
                    LoginUiState.Form()
                }
            }
        }

        // Lắng nghe session expired
        viewModelScope.launch {
            repository.sessionExpiredFlow.collect { expired ->
                if (expired) {
                    _uiState.value = LoginUiState.Form(
                        errorMessage = "session_expired"
                    )
                    repository.clearSessionExpired()
                }
            }
        }
    }

    // ── Form Actions ─────────────────────────────────────────────

    fun updateEmail(email: String) {
        val current = _uiState.value as? LoginUiState.Form ?: return
        _uiState.value = current.copy(email = email, errorMessage = null, validationError = null)
    }

    fun updatePassword(password: String) {
        val current = _uiState.value as? LoginUiState.Form ?: return
        _uiState.value = current.copy(password = password, errorMessage = null, validationError = null)
    }

    fun updateConfirmPassword(confirmPassword: String) {
        val current = _uiState.value as? LoginUiState.Form ?: return
        val validationError = if (current.isRegisterMode && confirmPassword != current.password) {
            "password_mismatch"
        } else null
        _uiState.value = current.copy(
            confirmPassword = confirmPassword,
            validationError = validationError
        )
    }

    fun togglePasswordVisibility() {
        val current = _uiState.value as? LoginUiState.Form ?: return
        _uiState.value = current.copy(passwordVisible = !current.passwordVisible)
    }

    fun toggleRegisterMode() {
        val current = _uiState.value as? LoginUiState.Form ?: return
        _uiState.value = current.copy(
            isRegisterMode = !current.isRegisterMode,
            errorMessage = null,
            validationError = null,
            confirmPassword = ""
        )
    }

    fun clearError() {
        val current = _uiState.value as? LoginUiState.Form ?: return
        _uiState.value = current.copy(errorMessage = null, validationError = null)
    }

    fun submit() {
        val current = _uiState.value as? LoginUiState.Form ?: return
        if (!current.isFormValid) return

        if (current.isRegisterMode) {
            if (current.isPasswordMismatch) {
                _uiState.value = current.copy(validationError = "password_mismatch")
                return
            }
            register(current.email, current.password)
        } else {
            login(current.email, current.password)
        }
    }

    private fun login(email: String, password: String) {
        val current = _uiState.value as? LoginUiState.Form ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isLoading = true, errorMessage = null)
            val result = repository.login(email, password)
            _uiState.value = if (result.isSuccess) {
                LoginUiState.LoggedIn
            } else {
                current.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    private fun register(email: String, password: String) {
        val current = _uiState.value as? LoginUiState.Form ?: return
        viewModelScope.launch {
            _uiState.value = current.copy(isLoading = true, errorMessage = null)
            val result = repository.register(email, password)
            _uiState.value = if (result.isSuccess) {
                LoginUiState.LoggedIn
            } else {
                current.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Form(isLoading = true)
            repository.logout()
            _uiState.value = LoginUiState.Form()
        }
    }

    @Suppress("UNCHECKED_CAST")
    class AuthViewModelFactory(
        private val repository: AuthRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                return AuthViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
package com.example.stylemate.repository

import com.example.stylemate.data.auth.AuthStorage
import com.example.stylemate.data.auth.AuthValidator
import com.example.stylemate.network.AuthLoginData
import com.example.stylemate.network.AuthResponse
import com.example.stylemate.network.LoginRequest
import com.example.stylemate.network.RefreshRequest
import com.example.stylemate.network.StylemateApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AuthRepository(
    private val apiService: StylemateApiService,
    private val authStorage: AuthStorage
) {

    val isLoggedInFlow: Flow<Boolean> = authStorage.accessTokenFlow
        .map { !it.isNullOrBlank() }

    suspend fun login(email: String, password: String): Result<AuthLoginData> {
        if (!AuthValidator.isValidEmail(email)) {
            return Result.failure(IllegalArgumentException("Email không hợp lệ"))
        }
        if (!AuthValidator.isValidPassword(password)) {
            return Result.failure(IllegalArgumentException("Mật khẩu tối thiểu 6 ký tự"))
        }

        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.login(LoginRequest(email.trim(), password))
                if (response.isSuccessful) {
                    val body = response.body()
                    val data = body?.data
                    if (body != null && data != null) {
                        authStorage.saveSession(
                            accessToken = data.accessToken,
                            refreshToken = data.refreshToken,
                            userId = data.user.id,
                            email = data.user.email
                        )
                        Result.success(data)
                    } else {
                        Result.failure(IllegalStateException("Response rỗng"))
                    }
                } else {
                    Result.failure(IllegalStateException("Đăng nhập thất bại: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun refreshAccessToken(refreshToken: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.refresh(RefreshRequest(refreshToken))
                if (response.isSuccessful) {
                    val token = response.body()?.data?.accessToken
                    if (!token.isNullOrBlank()) {
                        authStorage.updateAccessToken(token)
                        Result.success(token)
                    } else {
                        Result.failure(IllegalStateException("Token mới không hợp lệ"))
                    }
                } else {
                    Result.failure(IllegalStateException("Refresh token thất bại: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun logout(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.logout()
            } catch (_: Exception) {
                // Ignore network errors; still clear local session.
            }
            authStorage.clearSession()
            Result.success(Unit)
        }
    }
}


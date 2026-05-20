package com.example.stylemate.repository

import com.example.stylemate.data.auth.AuthStorage
import com.example.stylemate.data.auth.AuthValidator
import com.example.stylemate.network.AuthLoginData
import com.example.stylemate.network.LoginRequest
import com.example.stylemate.network.RefreshRequest
import com.example.stylemate.network.RegisterRequest
import com.example.stylemate.network.StylemateApiService
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.io.IOException

class AuthRepository(
    private val apiService: StylemateApiService,
    private val authStorage: AuthStorage
) {

    val isLoggedInFlow: Flow<Boolean> = authStorage.accessTokenFlow
        .map { !it.isNullOrBlank() }

    val sessionExpiredFlow: Flow<Boolean> = authStorage.sessionExpiredFlow

    private fun parseErrorMessage(errorBody: okhttp3.ResponseBody?): String? {
        val raw = errorBody?.string() ?: return null
        return try {
            val json = JsonParser.parseString(raw).asJsonObject
            json.get("message")?.asString
        } catch (_: Exception) {
            null
        }
    }

    private fun mapNetworkError(exception: Exception): String? {
        return when (exception) {
            is SocketTimeoutException -> "Kết nối bị timeout. Vui lòng thử lại."
            is UnknownHostException, is ConnectException -> "Không thể kết nối đến máy chủ. Kiểm tra mạng hoặc địa chỉ server."
            is IOException -> "Lỗi mạng. Vui lòng kiểm tra kết nối và thử lại."
            else -> null
        }
    }

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
                    val message = parseErrorMessage(response.errorBody())
                        ?: "Đăng nhập thất bại: ${response.code()}"
                    Result.failure(IllegalStateException(message))
                }
            } catch (e: Exception) {
                val message = mapNetworkError(e) ?: (e.message ?: "Đăng nhập thất bại")
                Result.failure(IllegalStateException(message))
            }
        }
    }

    suspend fun register(email: String, password: String): Result<AuthLoginData> {
        if (!AuthValidator.isValidEmail(email)) {
            return Result.failure(IllegalArgumentException("Email không hợp lệ"))
        }
        if (!AuthValidator.isValidPassword(password)) {
            return Result.failure(IllegalArgumentException("Mật khẩu tối thiểu 6 ký tự"))
        }

        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.register(RegisterRequest(email.trim(), password))
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
                    val message = parseErrorMessage(response.errorBody())
                        ?: "Đăng ký thất bại: ${response.code()}"
                    Result.failure(IllegalStateException(message))
                }
            } catch (e: Exception) {
                val message = mapNetworkError(e) ?: (e.message ?: "Đăng ký thất bại")
                Result.failure(IllegalStateException(message))
            }
        }
    }

    suspend fun clearSessionExpired() {
        authStorage.setSessionExpired(false)
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

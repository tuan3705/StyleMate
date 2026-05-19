package com.example.stylemate.network

import com.google.gson.annotations.SerializedName

/**
 * 🔐 Auth DTOs
 */

data class AuthUserDto(
    @SerializedName("id")
    val id: String,
    val email: String
)

data class AuthLoginData(
    val user: AuthUserDto,
    val accessToken: String,
    val refreshToken: String,
    val isNewUser: Boolean
)

data class AuthResponse<T>(
    val success: Boolean,
    val message: String?,
    val data: T
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class RefreshRequest(
    val refreshToken: String
)

data class RefreshData(
    val accessToken: String
)

data class SimpleMessageResponse(
    val success: Boolean,
    val message: String?
)

data class RegisterRequest(
    val email: String,
    val password: String
)

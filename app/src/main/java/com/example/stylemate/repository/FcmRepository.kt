package com.example.stylemate.repository

import com.example.stylemate.data.auth.AuthStorage
import com.example.stylemate.data.notification.FcmTokenStore
import com.example.stylemate.network.FcmTokenRequest
import com.example.stylemate.network.StylemateApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import android.util.Log

class FcmRepository(
    private val apiService: StylemateApiService,
    private val authStorage: AuthStorage,
    private val tokenStore: FcmTokenStore
) {

    companion object {
        private const val TAG = "FcmRepository"
    }

    suspend fun cacheFcmToken(token: String) = withContext(Dispatchers.IO) {
        if (token.isNotBlank()) {
            tokenStore.savePendingToken(token)
        }
    }

    suspend fun syncFcmToken(
        token: String? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val resolvedToken = token ?: tokenStore.pendingTokenFlow.firstOrNull()
        if (resolvedToken.isNullOrBlank()) {
            Log.d(TAG, "No FCM token to sync")
            return@withContext Result.success(Unit)
        }

        val userId = authStorage.userIdFlow.firstOrNull()
        if (userId.isNullOrBlank()) {
            Log.w(TAG, "Missing userId; caching token for later sync")
            tokenStore.savePendingToken(resolvedToken)
            return@withContext Result.failure(IllegalStateException("Thiếu userId để lưu FCM token"))
        }

        return@withContext try {
            val response = apiService.saveFcmToken(
                FcmTokenRequest(
                    userId = userId,
                    fcmToken = resolvedToken,
                    latitude = latitude,
                    longitude = longitude
                )
            )
            if (response.isSuccessful) {
                tokenStore.clearPendingToken()
                Log.d(TAG, "Synced FCM token for userId=${'$'}userId")
                Result.success(Unit)
            } else {
                tokenStore.savePendingToken(resolvedToken)
                Log.w(TAG, "Failed to sync FCM token: ${'$'}{response.code()}")
                Result.failure(IllegalStateException("Không thể lưu FCM token: ${'$'}{response.code()}"))
            }
        } catch (e: Exception) {
            tokenStore.savePendingToken(resolvedToken)
            Log.e(TAG, "Error syncing FCM token: ${'$'}{e.message}", e)
            Result.failure(e)
        }
    }
}

package com.example.stylemate.notification

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import android.util.Log

private const val TAG = "FcmTokenFetcher"

suspend fun fetchFcmToken(): String? {
    var lastToken: String? = null
    repeat(3) { attempt ->
        lastToken = suspendCancellableCoroutine { cont ->
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    Log.d(TAG, "Fetched FCM token (len=${'$'}{token.length}) on attempt ${'$'}{attempt + 1}")
                    cont.resume(token)
                }
                .addOnFailureListener { ex ->
                    Log.w(TAG, "Failed to fetch FCM token on attempt ${'$'}{attempt + 1}: ${'$'}{ex.message}")
                    cont.resume(null)
                }
        }
        if (!lastToken.isNullOrBlank()) {
            return lastToken
        }
        if (attempt < 2) {
            delay(1000)
        }
    }
    Log.w(TAG, "FCM token is still null after retries")
    return lastToken
}

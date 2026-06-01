package com.example.stylemate.data.auth

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking

object AuthTokenProvider {

    @Volatile
    private var storage: AuthStorage? = null

    fun init(authStorage: AuthStorage) {
        storage = authStorage
    }

    fun accessTokenBlocking(): String? {
        val currentStorage = storage ?: return null
        return runBlocking {
            currentStorage.accessTokenFlow
                .firstOrNull()
        }
    }

    fun refreshTokenBlocking(): String? {
        val currentStorage = storage ?: return null
        return runBlocking {
            currentStorage.refreshTokenFlow
                .firstOrNull()
        }
    }

    fun updateAccessTokenBlocking(accessToken: String) {
        val currentStorage = storage ?: return
        runBlocking {
            currentStorage.updateAccessToken(accessToken)
        }
    }

    fun setSessionExpiredBlocking(expired: Boolean) {
        val currentStorage = storage ?: return
        runBlocking {
            currentStorage.setSessionExpired(expired)
        }
    }

    fun clearSessionBlocking() {
        val currentStorage = storage ?: return
        runBlocking {
            currentStorage.clearSession()
        }
    }
}

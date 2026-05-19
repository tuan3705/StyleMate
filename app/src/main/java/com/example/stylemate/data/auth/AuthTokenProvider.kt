package com.example.stylemate.data.auth

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
}


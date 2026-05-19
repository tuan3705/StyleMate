package com.example.stylemate.data.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val AUTH_DATASTORE_NAME = "auth_prefs"

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(
    name = AUTH_DATASTORE_NAME
)

class AuthStorage(private val context: Context) {

    private object Keys {
        val accessToken = stringPreferencesKey("access_token")
        val refreshToken = stringPreferencesKey("refresh_token")
        val userId = stringPreferencesKey("user_id")
        val email = stringPreferencesKey("user_email")
        val sessionExpired = booleanPreferencesKey("session_expired")
    }

    val accessTokenFlow: Flow<String?> = context.authDataStore.data.map { prefs ->
        prefs[Keys.accessToken]
    }

    val refreshTokenFlow: Flow<String?> = context.authDataStore.data.map { prefs ->
        prefs[Keys.refreshToken]
    }

    val userEmailFlow: Flow<String?> = context.authDataStore.data.map { prefs ->
        prefs[Keys.email]
    }

    val sessionExpiredFlow: Flow<Boolean> = context.authDataStore.data.map { prefs ->
        prefs[Keys.sessionExpired] ?: false
    }

    suspend fun saveSession(
        accessToken: String,
        refreshToken: String,
        userId: String,
        email: String
    ) {
        context.authDataStore.edit { prefs ->
            prefs[Keys.accessToken] = accessToken
            prefs[Keys.refreshToken] = refreshToken
            prefs[Keys.userId] = userId
            prefs[Keys.email] = email
            prefs[Keys.sessionExpired] = false
        }
    }

    suspend fun updateAccessToken(accessToken: String) {
        context.authDataStore.edit { prefs ->
            prefs[Keys.accessToken] = accessToken
        }
    }

    suspend fun setSessionExpired(expired: Boolean) {
        context.authDataStore.edit { prefs ->
            prefs[Keys.sessionExpired] = expired
        }
    }

    suspend fun clearSession() {
        context.authDataStore.edit { prefs ->
            prefs.remove(Keys.accessToken)
            prefs.remove(Keys.refreshToken)
            prefs.remove(Keys.userId)
            prefs.remove(Keys.email)
            prefs[Keys.sessionExpired] = false
        }
    }
}

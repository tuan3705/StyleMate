package com.example.stylemate.data.notification

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val NOTIFICATION_DATASTORE_NAME = "notification_prefs"

private val Context.notificationDataStore: DataStore<Preferences> by preferencesDataStore(
    name = NOTIFICATION_DATASTORE_NAME
)

class FcmTokenStore(private val context: Context) {

    private object Keys {
        val pendingFcmToken = stringPreferencesKey("pending_fcm_token")
    }

    val pendingTokenFlow: Flow<String?> = context.notificationDataStore.data.map { prefs ->
        prefs[Keys.pendingFcmToken]
    }

    suspend fun savePendingToken(token: String) {
        context.notificationDataStore.edit { prefs ->
            prefs[Keys.pendingFcmToken] = token
        }
    }

    suspend fun clearPendingToken() {
        context.notificationDataStore.edit { prefs ->
            prefs.remove(Keys.pendingFcmToken)
        }
    }
}


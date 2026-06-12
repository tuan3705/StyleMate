package com.example.stylemate

import android.app.Application
import android.util.Log
import com.example.stylemate.data.auth.AuthStorage
import com.example.stylemate.data.auth.AuthTokenProvider
import com.example.stylemate.data.notification.FcmTokenStore
import com.example.stylemate.network.RetrofitClient
import com.example.stylemate.repository.AuthRepository
import com.example.stylemate.repository.FcmRepository
import com.example.stylemate.notification.AppForegroundTracker
import com.example.stylemate.notification.fetchFcmToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import com.google.firebase.FirebaseApp
import androidx.lifecycle.ProcessLifecycleOwner

class StyleMateApp : Application() {

    lateinit var authStorage: AuthStorage
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var fcmRepository: FcmRepository
        private set

    override fun onCreate() {
        super.onCreate()
        Log.d("StyleMateApp", "onCreate called")
        val firebaseApp = runCatching {
            FirebaseApp.initializeApp(this)
        }.onFailure { error ->
            Log.e("StyleMateApp", "FirebaseApp initialization failed", error)
        }.getOrNull()
        Log.d("StyleMateApp", "FirebaseApp initialized=${firebaseApp != null}")
        authStorage = AuthStorage(this)
        AuthTokenProvider.init(authStorage)
        authRepository = AuthRepository(RetrofitClient.stylemateApiService, authStorage)
        fcmRepository = FcmRepository(
            RetrofitClient.stylemateApiService,
            authStorage,
            FcmTokenStore(this)
        )

        ProcessLifecycleOwner.get().lifecycle.addObserver(AppForegroundTracker)

        if (firebaseApp != null) {
            CoroutineScope(Dispatchers.IO).launch {
                repeat(5) { attempt ->
                    val token = runCatching { fetchFcmToken() }
                        .onFailure { error -> Log.w("StyleMateApp", "FCM token fetch failed", error) }
                        .getOrNull()
                    if (!token.isNullOrBlank()) {
                        fcmRepository.cacheFcmToken(token)
                        fcmRepository.syncFcmToken(token)
                        return@launch
                    }
                    if (attempt < 4) {
                        delay(2000)
                    }
                }
            }
        } else {
            Log.w("StyleMateApp", "Skipping FCM token fetch because Firebase is unavailable")
        }

        CoroutineScope(Dispatchers.IO).launch {
            authStorage.accessTokenFlow.collectLatest { accessToken ->
                if (!accessToken.isNullOrBlank()) {
                    fcmRepository.syncFcmToken()
                }
            }
        }
    }
}

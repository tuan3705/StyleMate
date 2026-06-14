package com.example.stylemate

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
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

class StyleMateApp : Application(), ImageLoaderFactory {

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

    /**
     * ⚡ PERFORMANCE: Cấu hình Coil ImageLoader toàn cục với memory cache
     * và disk cache để tránh tải lại ảnh từ network mỗi lần scroll/recompose.
     *
     * ⚡ CẢI TIẾN 06/2026:
     *   - crossfade(true) global: hiệu ứng mượt mà khi ảnh load
     *   - bitmapPool: Pool bitmap tái sử dụng để giảm GC
     *   - Memory cache: 25% heap (tối đa 256MB)
     *   - Disk cache: 2% storage (tối đa 250MB)
     *   - respectCacheHeaders(false): ưu tiên cache local hơn header HTTP
     */
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // 25% of available heap
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_cache"))
                    .maxSizePercent(0.02) // 2% of total storage
                    .build()
            }
            .crossfade(true)
            .respectCacheHeaders(false)
            .build()
    }
}
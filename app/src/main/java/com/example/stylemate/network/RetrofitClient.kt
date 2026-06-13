package com.example.stylemate.network

import com.example.stylemate.BuildConfig
import com.example.stylemate.data.auth.AuthTokenProvider
import com.example.stylemate.data.auth.AuthRefreshInterceptor
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * ═══════════════════════════════════════════════════════════════
 * 🏗️ RETROFIT CLIENT — Singleton quản lý Retrofit Instance
 * ═══════════════════════════════════════════════════════════════
 *
 * ⚡ Base URL đọc từ BuildConfig (sinh từ local.properties)
 *    → Muốn đổi IP, chỉ cần sửa local.properties, KHÔNG cần sửa code!
 *
 * 🌤️ Thời tiết gọi qua Backend proxy (/api/weather/forecast)
 *    → Không cần WEATHER_API_KEY ở Android nữa!
 *
 * Quản lý 1 Retrofit instance duy nhất cho Backend Node.js.
 *
 * Android Emulator dùng 10.0.2.2 để truy cập localhost của máy host.
 * ───────────────────────────────────────────────────────────────
 */
object RetrofitClient {

    // ═════════════════════════════════════════════════════════════
    // 🎯 Cấu hình Backend — Đọc từ BuildConfig (BuildConfig sinh từ local.properties)
    // ═════════════════════════════════════════════════════════════

    /**
     * Base URL của Backend Node.js.
     * Giá trị được lấy từ BuildConfig.STYLEMATE_BASE_URL
     * → BuildConfig được Gradle sinh dựa trên local.properties
     *
     * Cách đổi:
     *   1. Mở file local.properties ở thư mục gốc dự án
     *   2. Sửa dòng STYLEMATE_BASE_URL=http://IP_MỚI:3000/
     *   3. Build lại app (Build → Rebuild Project)
     *
     * ⚠️ KHÔNG bao giờ sửa trực tiếp trong file này nữa!
     */
    @JvmField
    val STYLEMATE_BASE_URL: String = BuildConfig.STYLEMATE_BASE_URL

    /**
     * Timeout mặc định cho tất cả request (30 giây).
     */
    private const val TIMEOUT_SECONDS = 30L

    // ═════════════════════════════════════════════════════════════
    // 🛡️ OkHttp Client
    // ═════════════════════════════════════════════════════════════

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.HEADERS
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    /**
     * ⚡ FIX: Dùng `header()` thay vì `addHeader()` để tránh trùng lặp Authorization headers
     * khi AuthRefreshInterceptor retry request.
     *
     * Thứ tự interceptor:
     * 1. refreshInterceptor (chạy đầu tiên để bao bọc cả authInterceptor)
     * 2. authInterceptor (thêm token)
     * 3. loggingInterceptor
     *
     * Lý do: refreshInterceptor cần bắt response ở ngoài cùng. Khi retry,
     * nó tạo request mới với token mới → authInterceptor sẽ không ghi đè
     * vì dùng `header()` thay vì `addHeader()`.
     */
    private val refreshInterceptor = AuthRefreshInterceptor(STYLEMATE_BASE_URL)

    private val authInterceptor = Interceptor { chain ->
        val token = AuthTokenProvider.accessTokenBlocking()
        val request = if (!token.isNullOrBlank()) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            // ⚡ refreshInterceptor phải ở TRƯỚC authInterceptor để bắt response
            .addInterceptor(refreshInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    // ═════════════════════════════════════════════════════════════
    // 🏗️ Retrofit Instance cho Stylemate Backend
    // ═════════════════════════════════════════════════════════════
    private val stylemateRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(STYLEMATE_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Service chính cho toàn bộ API của Stylemate Backend.
     * Dùng trong tất cả Repository (kể cả weather).
     */
    val stylemateApiService: StylemateApiService by lazy {
        stylemateRetrofit.create(StylemateApiService::class.java)
    }
}

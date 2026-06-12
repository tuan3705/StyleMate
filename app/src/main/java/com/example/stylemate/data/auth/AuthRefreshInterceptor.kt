package com.example.stylemate.data.auth

import com.google.gson.JsonParser
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class AuthRefreshInterceptor(
    private val baseUrl: String
) : Interceptor {

    // ── Lock đồng bộ hóa refresh token ─────────────────────────
    private val refreshLock = ReentrantLock()

    // Cache access token đã được refresh để tránh gọi refresh nhiều lần
    @Volatile
    private var refreshedAccessToken: String? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (response.code != 401) {
            return response
        }

        // Không retry nếu đã retry rồi
        if (request.header(RETRY_HEADER) == RETRY_HEADER_VALUE) {
            return response
        }

        // Không retry nếu chính request refresh
        val requestPath = request.url.encodedPath
        if (requestPath.endsWith("/api/auth/refresh")) {
            return response
        }

        // Đồng bộ hóa: chỉ 1 thread được refresh token
        return refreshLock.withLock {
            // Kiểm tra nếu thread khác đã refresh thành công
            val cachedToken = refreshedAccessToken
            if (cachedToken != null) {
                // Dùng token đã refresh, gửi lại request
                response.close()
                return retryWithToken(chain, request, cachedToken)
            }

            // Thực hiện refresh
            val refreshToken = AuthTokenProvider.refreshTokenBlocking()
            if (refreshToken.isNullOrBlank()) {
                AuthTokenProvider.setSessionExpiredBlocking(true)
                AuthTokenProvider.clearSessionBlocking()
                return response
            }

            val newAccessToken = tryRefreshToken(refreshToken)
            if (newAccessToken.isNullOrBlank()) {
                AuthTokenProvider.setSessionExpiredBlocking(true)
                AuthTokenProvider.clearSessionBlocking()
                return response
            }

            // Cache token mới và cập nhật storage
            refreshedAccessToken = newAccessToken
            AuthTokenProvider.updateAccessTokenBlocking(newAccessToken)

            response.close()
            retryWithToken(chain, request, newAccessToken)
        }
    }

    private fun retryWithToken(chain: Interceptor.Chain, request: Request, token: String): Response {
        val newRequest = request.newBuilder()
            .header("Authorization", "Bearer $token")
            .header(RETRY_HEADER, RETRY_HEADER_VALUE)
            .build()
        return chain.proceed(newRequest)
    }

    private fun tryRefreshToken(refreshToken: String): String? {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = "{\"refreshToken\":\"$refreshToken\"}".toRequestBody(mediaType)
        val refreshRequest = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/api/auth/refresh")
            .post(body)
            .build()

        return try {
            val client = okhttp3.OkHttpClient()
            client.newCall(refreshRequest).execute().use { refreshResponse ->
                if (!refreshResponse.isSuccessful) return null
                val bodyString = refreshResponse.body?.string() ?: return null
                val json = JsonParser.parseString(bodyString).asJsonObject
                val data = json.getAsJsonObject("data") ?: return null
                data.get("accessToken")?.asString
            }
        } catch (_: Exception) {
            null
        }
    }

    private companion object {
        const val RETRY_HEADER = "X-Auth-Retry"
        const val RETRY_HEADER_VALUE = "1"
    }
}
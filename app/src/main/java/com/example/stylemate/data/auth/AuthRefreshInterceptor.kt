package com.example.stylemate.data.auth

import com.google.gson.JsonParser
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

class AuthRefreshInterceptor(
    private val baseUrl: String
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (response.code != 401) {
            return response
        }

        if (request.header(RETRY_HEADER) == RETRY_HEADER_VALUE) {
            return response
        }

        val requestPath = request.url.encodedPath
        if (requestPath.endsWith("/api/auth/refresh")) {
            return response
        }

        val refreshToken = AuthTokenProvider.refreshTokenBlocking()
        if (refreshToken.isNullOrBlank()) {
            return response
        }

        response.close()

        val refreshedToken = tryRefreshToken(refreshToken) ?: return response

        AuthTokenProvider.updateAccessTokenBlocking(refreshedToken)

        val newRequest = request.newBuilder()
            .header("Authorization", "Bearer $refreshedToken")
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


package com.example.stylemate.network

import android.content.Context
import android.util.Log
import com.example.stylemate.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

class RemoveBgClient(private val context: Context) {

    companion object {
        private const val TAG = "RemoveBgClient"
        private const val API_URL = "https://api.remove.bg/v1.0/removebg"
        private const val TIMEOUT_SECONDS = 60L
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    suspend fun removeBackground(imageFile: File): Pair<File?, String?> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.REMOVE_BG_API_KEY
        if (apiKey.isBlank()) {
            return@withContext null to "Chưa cấu hình REMOVE_BG_API_KEY. Sử dụng ảnh gốc."
        }
        if (!imageFile.exists()) {
            return@withContext null to "Không tìm thấy file ảnh để xoá nền."
        }

        try {
            val mimeType = when {
                imageFile.name.endsWith(".png", ignoreCase = true) -> "image/png"
                imageFile.name.endsWith(".gif", ignoreCase = true) -> "image/gif"
                imageFile.name.endsWith(".webp", ignoreCase = true) -> "image/webp"
                else -> "image/jpeg"
            }
            val requestBody = imageFile.asRequestBody(mimeType.toMediaTypeOrNull())
            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image_file", imageFile.name, requestBody)
                .addFormDataPart("size", "auto")
                .addFormDataPart("format", "png")
                .build()

            val request = Request.Builder()
                .url(API_URL)
                .addHeader("X-Api-Key", apiKey)
                .post(multipartBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string()
                    Log.w(TAG, "remove.bg lỗi: HTTP ${response.code} ${response.message} - $errBody")
                    return@withContext null to "remove.bg lỗi: HTTP ${response.code}"
                }

                val bytes = response.body?.bytes()
                if (bytes == null || bytes.isEmpty()) {
                    return@withContext null to "remove.bg trả về dữ liệu rỗng."
                }

                val outputFile = File(context.cacheDir, "nobg_${System.currentTimeMillis()}.png")
                outputFile.outputStream().use { it.write(bytes) }
                return@withContext outputFile to null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi gọi remove.bg: ${e.message}", e)
            return@withContext null to "Không thể xoá nền: ${e.message}"
        }
    }
}

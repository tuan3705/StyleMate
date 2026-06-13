package com.example.stylemate.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.stylemate.data.local.ImageStorage
import com.example.stylemate.network.RetrofitClient
import com.example.stylemate.network.StylemateApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.net.URL
import java.util.Locale
import org.json.JSONObject

class ImageProcessingRepository(
    private val apiService: StylemateApiService,
    private val context: Context
) {
    companion object {
        private const val TAG = "ImageProcessingRepository"
    }
    private suspend fun resolveInputFile(path: String, prefix: String): File {
        // Duong dan tuong doi cua backend (vd: /uploads/xxx.jpg) -> ghep BASE_URL thanh URL day du
        val normalized = if (path.startsWith("/uploads/")) {
            RetrofitClient.STYLEMATE_BASE_URL.trimEnd('/') + path
        } else {
            path
        }
        return when {
            normalized.startsWith("file://") -> File(normalized.removePrefix("file://"))
            normalized.startsWith("content://") -> {
                val uri = Uri.parse(normalized)
                ImageStorage.copyUriToInternalStorage(context, uri, prefix = prefix)
            }
            normalized.startsWith("http://") || normalized.startsWith("https://") -> {
                val extension = normalized.substringAfterLast('.', "")
                    .substringBefore('?')
                    .lowercase(Locale.ROOT)
                    .ifBlank { "jpg" }
                ImageStorage.saveStreamToInternalStorage(
                    context = context,
                    inputStream = URL(normalized).openStream(),
                    prefix = prefix,
                    extension = extension
                )
            }
            else -> File(normalized)
        }
    }

    suspend fun removeBackground(localPath: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val file = resolveInputFile(localPath, prefix = "remove_bg_input_")
            if (!file.exists()) {
                return@withContext Result.failure(IllegalArgumentException("Image does not exist"))
            }

            val extension = file.extension.lowercase(Locale.ROOT)
            val mimeType = when (extension) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "webp" -> "image/webp"
                "gif" -> "image/gif"
                "bmp" -> "image/bmp"
                else -> "image/*"
            }
            val requestBody = file.asRequestBody(mimeType.toMediaTypeOrNull())
            val multipartPart = MultipartBody.Part.createFormData("image", file.name, requestBody)

            val response = apiService.removeBackgroundImage(multipartPart)
            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string().orEmpty()
                val message = try {
                    val payload = JSONObject(errorBody)
                    payload.optString("message").ifBlank { "Background removal failed: ${response.code()}" }
                } catch (e: Exception) {
                    if (errorBody.isNotBlank()) errorBody else "Background removal failed: ${response.code()}"
                }
                return@withContext Result.failure(IllegalStateException(message))
            }

            val body = response.body() ?: return@withContext Result.failure(
                IllegalStateException("No image data received")
            )

            val outputFile = ImageStorage.saveStreamToInternalStorage(
                context = context,
                inputStream = body.byteStream(),
                prefix = "nobg_",
                extension = "png"
            )

            Result.success(outputFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fillWithAi(localPath: String): Result<com.example.stylemate.network.AiFillSuggestionDto> =
        withContext(Dispatchers.IO) {
            try {
                val file = resolveInputFile(localPath, prefix = "ai_fill_input_")
                if (!file.exists()) {
                    return@withContext Result.failure(IllegalArgumentException("Image does not exist"))
                }

                val extension = file.extension.lowercase(Locale.ROOT)
                val mimeType = when (extension) {
                    "jpg", "jpeg" -> "image/jpeg"
                    "png" -> "image/png"
                    "webp" -> "image/webp"
                    "gif" -> "image/gif"
                    "bmp" -> "image/bmp"
                    else -> "image/*"
                }
                val requestBody = file.asRequestBody(mimeType.toMediaTypeOrNull())
                val multipartPart = MultipartBody.Part.createFormData("image", file.name, requestBody)

                val response = apiService.aiFillFromImage(multipartPart)
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("AI tagging failed: ${response.code()}")
                    )
                }

                val body = response.body()
                    ?: return@withContext Result.failure(IllegalStateException("No data received"))
                if (!body.success) {
                    return@withContext Result.failure(IllegalStateException(body.message ?: "AI tagging lỗi"))
                }

                Log.d(TAG, "AI fill response: $body")

                Result.success(body.data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun autoTagging(localPath: String): Result<com.example.stylemate.network.AiAutoTaggingSuggestionDto> =
        withContext(Dispatchers.IO) {
            try {
                val file = resolveInputFile(localPath, prefix = "auto_tag_input_")
                if (!file.exists()) {
                    return@withContext Result.failure(IllegalArgumentException("Image does not exist"))
                }

                val extension = file.extension.lowercase(Locale.ROOT)
                val mimeType = when (extension) {
                    "jpg", "jpeg" -> "image/jpeg"
                    "png" -> "image/png"
                    "webp" -> "image/webp"
                    "gif" -> "image/gif"
                    "bmp" -> "image/bmp"
                    else -> "image/*"
                }
                val requestBody = file.asRequestBody(mimeType.toMediaTypeOrNull())
                val multipartPart = MultipartBody.Part.createFormData("image", file.name, requestBody)

                val response = apiService.autoTaggingFromImage(multipartPart)
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("Auto tagging failed: ${response.code()}")
                    )
                }

                val body = response.body()
                    ?: return@withContext Result.failure(IllegalStateException("No data received"))
                if (!body.success) {
                    return@withContext Result.failure(IllegalStateException(body.message ?: "Auto tagging lỗi"))
                }

                Result.success(body.data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}

package com.example.stylemate.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.stylemate.data.local.ImageStorage
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
        return when {
            path.startsWith("file://") -> File(path.removePrefix("file://"))
            path.startsWith("content://") -> {
                val uri = Uri.parse(path)
                ImageStorage.copyUriToInternalStorage(context, uri, prefix = prefix)
            }
            path.startsWith("http://") || path.startsWith("https://") -> {
                val extension = path.substringAfterLast('.', "")
                    .substringBefore('?')
                    .lowercase(Locale.ROOT)
                    .ifBlank { "jpg" }
                ImageStorage.saveStreamToInternalStorage(
                    context = context,
                    inputStream = URL(path).openStream(),
                    prefix = prefix,
                    extension = extension
                )
            }
            else -> File(path)
        }
    }

    suspend fun removeBackground(localPath: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val file = resolveInputFile(localPath, prefix = "remove_bg_input_")
            if (!file.exists()) {
                return@withContext Result.failure(IllegalArgumentException("Ảnh không tồn tại"))
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
                    payload.optString("message").ifBlank { "Tách nền thất bại: ${response.code()}" }
                } catch (e: Exception) {
                    if (errorBody.isNotBlank()) errorBody else "Tách nền thất bại: ${response.code()}"
                }
                return@withContext Result.failure(IllegalStateException(message))
            }

            val body = response.body() ?: return@withContext Result.failure(
                IllegalStateException("Không nhận được dữ liệu ảnh")
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
                    return@withContext Result.failure(IllegalArgumentException("Ảnh không tồn tại"))
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
                        IllegalStateException("AI tagging thất bại: ${response.code()}")
                    )
                }

                val body = response.body()
                    ?: return@withContext Result.failure(IllegalStateException("Không nhận được dữ liệu"))
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
                    return@withContext Result.failure(IllegalArgumentException("Ảnh không tồn tại"))
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
                        IllegalStateException("Auto tagging thất bại: ${response.code()}")
                    )
                }

                val body = response.body()
                    ?: return@withContext Result.failure(IllegalStateException("Không nhận được dữ liệu"))
                if (!body.success) {
                    return@withContext Result.failure(IllegalStateException(body.message ?: "Auto tagging lỗi"))
                }

                Result.success(body.data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}

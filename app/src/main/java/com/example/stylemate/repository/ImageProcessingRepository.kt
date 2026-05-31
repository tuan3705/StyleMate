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
import java.util.Locale

class ImageProcessingRepository(
    private val apiService: StylemateApiService,
    private val context: Context
) {
    companion object {
        private const val TAG = "ImageProcessingRepository"
    }
    suspend fun removeBackground(localPath: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val file = when {
                localPath.startsWith("file://") -> File(localPath.removePrefix("file://"))
                localPath.startsWith("content://") -> {
                    val uri = Uri.parse(localPath)
                    ImageStorage.copyUriToInternalStorage(context, uri, prefix = "remove_bg_input_")
                }
                else -> File(localPath)
            }

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
                return@withContext Result.failure(IllegalStateException("Tách nền thất bại: ${response.code()}"))
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
                val file = when {
                    localPath.startsWith("file://") -> File(localPath.removePrefix("file://"))
                    localPath.startsWith("content://") -> {
                        val uri = Uri.parse(localPath)
                        ImageStorage.copyUriToInternalStorage(context, uri, prefix = "ai_fill_input_")
                    }
                    else -> File(localPath)
                }

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
}

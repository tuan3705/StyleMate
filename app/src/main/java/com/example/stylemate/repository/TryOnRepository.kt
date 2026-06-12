package com.example.stylemate.repository

import android.content.Context
import android.net.Uri
import com.example.stylemate.data.models.JobStatus
import com.example.stylemate.data.models.ProcessingJob
import com.example.stylemate.network.RetrofitClient
import com.example.stylemate.network.StylemateApiService
import com.google.gson.Gson
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class TryOnRepository(private val context: Context) {

    private val api: StylemateApiService = RetrofitClient.stylemateApiService
    private val gson = Gson()

    /**
     * Start virtual try-on: upload body image + send selectedItemIds as JSON
     * Backend will use selectedItemIds to fetch cloth images from DB by ID.
     */
    suspend fun performTryOnWithItemIds(
        bodyImageUri: Uri,
        selectedItemIds: List<String>,
        onProgress: (ProcessingJob) -> Unit
    ): Result<String> {
        return try {
            // Upload body image as file
            val bodyPart = uriToMultipart(bodyImageUri, "bodyImage")

            // Send selectedItemIds as a JSON form field
            val itemIdsBody = gson.toJson(selectedItemIds)
                .toRequestBody("application/json".toMediaTypeOrNull())

            // Kickoff
            val kickoffResponse = api.kickoffTryOnWithItemIds(bodyPart, itemIdsBody)
            if (!kickoffResponse.isSuccessful) {
                return Result.failure(Exception("Failed to start try-on: ${kickoffResponse.code()}"))
            }

            val responseBody = kickoffResponse.body() ?: return Result.failure(Exception("Empty response"))
            val jobId = responseBody.jobId ?: return Result.failure(Exception("No jobId in response"))
            val estimatedSeconds = responseBody.estimatedSeconds ?: 15

            onProgress(ProcessingJob(
                jobId = jobId,
                userId = "",
                type = "virtual-tryon",
                status = JobStatus.QUEUED,
                progress = 0
            ))

            // Poll for result
            val maxAttempts = (estimatedSeconds * 2) + 15
            for (i in 0..maxAttempts) {
                delay(1500)

                val statusResponse = api.getTryOnStatus(jobId)
                if (!statusResponse.isSuccessful) continue

                val statusBody = statusResponse.body()
                if (statusBody == null || statusBody.status == null) continue

                val currentStatus = parseStatus(statusBody.status)
                val currentProgress = statusBody.progress ?: 0

                onProgress(ProcessingJob(
                    jobId = jobId,
                    userId = "",
                    type = "virtual-tryon",
                    status = currentStatus,
                    progress = currentProgress
                ))

                when (currentStatus) {
                    JobStatus.COMPLETED -> {
                        val resultResponse = api.getTryOnResult(jobId)
                        if (resultResponse.isSuccessful) {
                            val resultBody = resultResponse.body()
                            val imageUrl = resultBody?.generatedImageUrl
                            if (imageUrl != null) {
                                val fullUrl = RetrofitClient.STYLEMATE_BASE_URL.trimEnd('/') + imageUrl
                                return Result.success(fullUrl)
                            }
                        }
                        return Result.failure(Exception("Completed but no image URL"))
                    }
                    JobStatus.FAILED -> {
                        return Result.failure(Exception(statusBody.message ?: "Try-on failed"))
                    }
                    else -> { /* keep polling */ }
                }
            }

            Result.failure(Exception("Timeout waiting for try-on result"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun uriToMultipart(uri: Uri, fieldName: String): MultipartBody.Part {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open $uri")
        val file = File(context.cacheDir, "tryon_${System.currentTimeMillis()}_${fieldName}.jpg")
        FileOutputStream(file).use { output ->
            inputStream.copyTo(output)
        }
        inputStream.close()

        val requestBody = file.readBytes().toRequestBody("image/jpeg".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(fieldName, file.name, requestBody)
    }

    private fun parseStatus(statusStr: String?): JobStatus {
        return when (statusStr?.lowercase()) {
            "queued" -> JobStatus.QUEUED
            "processing" -> JobStatus.IN_PROGRESS
            "completed" -> JobStatus.COMPLETED
            "failed" -> JobStatus.FAILED
            "cancelled" -> JobStatus.CANCELLED
            else -> JobStatus.FAILED
        }
    }
}

data class KickoffResponse(
    val success: Boolean?,
    val jobId: String?,
    val status: String?,
    val estimatedSeconds: Int?
)

data class StatusResponse(
    val success: Boolean?,
    val jobId: String?,
    val status: String?,
    val progress: Int?,
    val message: String?
)

data class ResultResponse(
    val success: Boolean?,
    val jobId: String?,
    val status: String?,
    val generatedImageUrl: String?,
    val message: String?
)
package com.example.stylemate.repository

import android.content.Context
import android.util.Log
import com.example.stylemate.model.ClothingItemEntity
import com.example.stylemate.network.ClothingItemDto
import com.example.stylemate.network.RetrofitClient
import com.example.stylemate.network.StylemateApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

/**
 * 🏪 CLOTHING REPOSITORY
 * 
 * Xử lý dữ liệu trang phục thông qua API Backend Node.js.
 */
class ClothingRepository(
    private val apiService: StylemateApiService,
    private val context: Context
) {

    companion object {
        private const val TAG = "ClothingRepository"

        private val BASE_URL: String by lazy {
            RetrofitClient.STYLEMATE_BASE_URL.trimEnd('/')
        }

        private fun isLocalFilePath(path: String): Boolean {
            return path.startsWith("/data/") ||
                   path.startsWith("/storage/") ||
                   path.startsWith("/sdcard/") ||
                   path.startsWith("/mnt/") ||
                   path.startsWith("file://") ||
                   path.startsWith("content://")
        }

        private fun toFullUrl(path: String): String {
            if (path.isBlank()) return path
            if (path.startsWith("http://") || path.startsWith("https://")) return path
            if (path.startsWith("/uploads/")) return "$BASE_URL$path"
            return path
        }

        fun ClothingItemDto.toEntity() = ClothingItemEntity(
            id = this.id,
            imageOriginal = this.imageOriginal,
            imageNoBg = this.imageNoBg,
            category = this.category,
            color = this.color,
            name = this.name,
            season = this.season,
            occasion = this.occasion,
            brand = this.brand,
            purchaseDate = this.purchaseDate,
            price = this.price,
            canvasPosX = this.canvasPosX,
            canvasPosY = this.canvasPosY,
            createdAt = this.createdAt
        )

        fun ClothingItemEntity.toDto() = ClothingItemDto(
            id = this.id,
            imageOriginal = this.imageOriginal,
            imageNoBg = this.imageNoBg,
            category = this.category,
            color = this.color,
            name = this.name,
            season = this.season,
            occasion = this.occasion,
            brand = this.brand,
            purchaseDate = this.purchaseDate,
            price = this.price,
            canvasPosX = this.canvasPosX,
            canvasPosY = this.canvasPosY,
            createdAt = this.createdAt
        )
    }

    fun getAllItems(): Flow<List<ClothingItemEntity>> = flow {
        try {
            val response = apiService.getAllClothes()
            if (response.isSuccessful) {
                val body = response.body()
                emit(body?.data?.map { it.toEntity() } ?: emptyList())
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ getAllItems() lỗi: ${e.message}")
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    fun getItemsByCategory(category: String): Flow<List<ClothingItemEntity>> = flow {
        try {
            val response = apiService.getAllClothes(category = category)
            if (response.isSuccessful) {
                val body = response.body()
                emit(body?.data?.map { it.toEntity() } ?: emptyList())
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ getItemsByCategory() lỗi: ${e.message}")
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    fun getItemCountByCategory(category: String): Flow<Int> = flow {
        try {
            val response = apiService.getAllClothes(category = category)
            if (response.isSuccessful) {
                val body = response.body()
                val count = body?.count ?: (body?.data?.size ?: 0)
                emit(count)
            } else {
                emit(0)
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ getItemCountByCategory() lỗi: ${e.message}")
            emit(0)
        }
    }.flowOn(Dispatchers.IO)

    fun getTotalItemCount(): Flow<Int> = flow {
        try {
            val response = apiService.getAllClothes()
            if (response.isSuccessful) {
                val body = response.body()
                val count = body?.count ?: (body?.data?.size ?: 0)
                emit(count)
            } else {
                emit(0)
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ getTotalItemCount() lỗi: ${e.message}")
            emit(0)
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getItemById(itemId: String): ClothingItemEntity? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getClothingItemById(itemId)
            if (response.isSuccessful) {
                response.body()?.data?.toEntity()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ getItemById() lỗi: ${e.message}")
            null
        }
    }

    suspend fun uploadImageToServer(localPath: String): String = withContext(Dispatchers.IO) {
        if (!isLocalFilePath(localPath)) {
            return@withContext toFullUrl(localPath)
        }

        try {
            val file = when {
                localPath.startsWith("file://") -> File(localPath.removePrefix("file://"))
                localPath.startsWith("content://") -> {
                    val uri = android.net.Uri.parse(localPath)
                    val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    tempFile
                }
                else -> File(localPath)
            }

            if (!file.exists()) return@withContext toFullUrl(localPath)

            val mimeType = "image/jpeg"
            val requestBody = file.asRequestBody(mimeType.toMediaTypeOrNull())
            val multipartPart = MultipartBody.Part.createFormData("image", file.name, requestBody)

            val response = apiService.uploadImage(multipartPart)
            if (response.isSuccessful) {
                val body = response.body()
                val serverPath = body?.url ?: ""
                if (serverPath.isNotBlank()) {
                    return@withContext "$BASE_URL$serverPath"
                }
            }
            toFullUrl(localPath)
        } catch (e: Exception) {
            toFullUrl(localPath)
        }
    }

    suspend fun insertItem(item: ClothingItemEntity) = withContext(Dispatchers.IO) {
        try {
            val uploadedOriginal = uploadImageToServer(item.imageOriginal)
            val uploadedNoBg = if (item.imageNoBg.isNotBlank()) {
                uploadImageToServer(item.imageNoBg)
            } else {
                ""
            }
            val dto = item.toDto().copy(
                imageOriginal = uploadedOriginal,
                imageNoBg = uploadedNoBg
            )
            apiService.createClothingItem(dto)
        } catch (e: Exception) {
            Log.e(TAG, "❌ insertItem() lỗi: ${e.message}")
        }
    }

    suspend fun updateItem(item: ClothingItemEntity) = withContext(Dispatchers.IO) {
        try {
            val uploadedOriginal = uploadImageToServer(item.imageOriginal)
            val uploadedNoBg = if (item.imageNoBg.isNotBlank()) {
                uploadImageToServer(item.imageNoBg)
            } else {
                ""
            }
            val updateMap = mapOf<String, Any>(
                "imageOriginal" to uploadedOriginal,
                "imageNoBg" to uploadedNoBg,
                "category" to item.category,
                "color" to item.color,
                "name" to item.name,
                "season" to item.season,
                "occasion" to item.occasion,
                "brand" to item.brand,
                "purchaseDate" to item.purchaseDate,
                "price" to item.price
            )
            apiService.updateClothingItem(item.id, updateMap)
        } catch (e: Exception) {
            Log.e(TAG, "❌ updateItem() lỗi: ${e.message}")
        }
    }

    suspend fun deleteItem(item: ClothingItemEntity) = withContext(Dispatchers.IO) {
        try {
            apiService.deleteClothingItem(item.id)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ deleteItem() lỗi: ${e.message}")
        }
    }

    suspend fun updateItemCanvasPosition(itemId: String, posX: Float, posY: Float) = withContext(Dispatchers.IO) {
        try {
            apiService.updateClothingItem(itemId, mapOf("canvasPosX" to posX, "canvasPosY" to posY))
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ updateItemCanvasPosition() lỗi: ${e.message}")
        }
    }
}

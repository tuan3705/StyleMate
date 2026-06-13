package com.example.stylemate.repository

import android.content.Context
import android.util.Log
import com.example.stylemate.model.Categories
import com.example.stylemate.model.ClothingItemEntity
import com.example.stylemate.network.ClothingItemDto
import com.example.stylemate.network.RetrofitClient
import com.example.stylemate.network.StylemateApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

/**
 * 🏪 CLOTHING REPOSITORY
 * 
 * ⚡ CẢI TIẾN HIỆU NĂNG (06/2026):
 *   - Cache trong memory 60s cho getAllItems() → tránh gọi API liên tục
 *   - getItemCountByCategory() và getTotalItemCount() tính local từ cache
 *     → KHÔNG gọi API riêng cho từng category (tránh N request)
 *   - uploadImageToServer: tối ưu tránh copy file content:// không cần thiết
 */
class ClothingRepository(
    private val apiService: StylemateApiService,
    private val context: Context
) {

    companion object {
        private const val TAG = "ClothingRepository"
        private const val CACHE_TTL_MS = 60_000L // 60 giây

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

    // ═════════════════════════════════════════════════════════════
    // 🗃️ In-Memory Cache (thread-safe)
    // ═════════════════════════════════════════════════════════════

    private val cacheMutex = Mutex()
    private var cachedItems: List<ClothingItemEntity>? = null
    private var cacheTimestamp: Long = 0L

    /**
     * Lấy danh sách items từ cache hoặc API.
     * Cache có TTL 60s, tự động refresh khi hết hạn.
     */
    private suspend fun getCachedItems(): List<ClothingItemEntity> = cacheMutex.withLock {
        val now = System.currentTimeMillis()
        if (cachedItems != null && (now - cacheTimestamp) < CACHE_TTL_MS) {
            Log.d(TAG, "📦 Dùng cache (${cachedItems!!.size} items, age=${now - cacheTimestamp}ms)")
            return@withLock cachedItems!!
        }
        try {
            Log.d(TAG, "🌐 Gọi API getAllClothes()...")
            val response = apiService.getAllClothes()
            if (response.isSuccessful) {
                val items = response.body()?.data?.map { it.toEntity() } ?: emptyList()
                cachedItems = items
                cacheTimestamp = now
                items
            } else {
                // Nếu API lỗi nhưng có cache cũ → trả cache cũ để app không crash
                cachedItems ?: emptyList()
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ API getAllClothes() exception: ${e.message}")
            // Trả cache cũ nếu có, nếu không thì empty list (không crash)
            cachedItems ?: emptyList()
        }
    }

    /**
     * Xoá cache để lần gọi tiếp theo sẽ fetch mới.
     */
    fun invalidateCache() {
        Log.d(TAG, "🗑️ Xoá cache")
        cachedItems = null
        cacheTimestamp = 0L
    }

    // ═════════════════════════════════════════════════════════════
    // 📋 Public API — Flow-based
    // ═════════════════════════════════════════════════════════════

    /**
     * Lấy tất cả items (dùng cache).
     */
    fun getAllItems(): Flow<List<ClothingItemEntity>> = flow {
        emit(getCachedItems())
    }.flowOn(Dispatchers.IO)

    /**
     * Lấy items theo category (filter từ cache, KHÔNG gọi API riêng).
     */
    fun getItemsByCategory(category: String): Flow<List<ClothingItemEntity>> = flow {
        if (category == Categories.ALL) {
            emit(getCachedItems())
        } else {
            emit(getCachedItems().filter { it.category == category })
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Đếm items theo category (tính từ cache, KHÔNG gọi API riêng).
     * ⚡ QUAN TRỌNG: Trước đây mỗi category gọi 1 API → giờ chỉ dùng cache.
     */
    fun getItemCountByCategory(category: String): Flow<Int> = flow {
        val allItems = getCachedItems()
        val count = if (category == Categories.ALL) {
            allItems.size
        } else {
            allItems.count { it.category == category }
        }
        emit(count)
    }.flowOn(Dispatchers.IO)

    /**
     * Tổng số items (tính từ cache).
     */
    fun getTotalItemCount(): Flow<Int> = flow {
        emit(getCachedItems().size)
    }.flowOn(Dispatchers.IO)

    /**
     * Lấy item theo ID (từ cache nếu có, không thì gọi API).
     */
    suspend fun getItemById(itemId: String): ClothingItemEntity? = withContext(Dispatchers.IO) {
        // Ưu tiên tìm trong cache
        val cached = cachedItems?.find { it.id == itemId }
        if (cached != null) return@withContext cached

        // Fallback: gọi API
        try {
            val response = apiService.getClothingItemById(itemId)
            if (response.isSuccessful) {
                response.body()?.data?.toEntity()
            } else null
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ getItemById() lỗi: ${e.message}")
            null
        }
    }

    // ═════════════════════════════════════════════════════════════
    // 📤 Upload & Ghi (Write)
    // ═════════════════════════════════════════════════════════════

    suspend fun uploadImageToServer(localPath: String): String = withContext(Dispatchers.IO) {
        if (!isLocalFilePath(localPath)) {
            return@withContext toFullUrl(localPath)
        }

        try {
            val file = when {
                localPath.startsWith("file://") -> File(localPath.removePrefix("file://"))
                localPath.startsWith("content://") -> {
                    val tempFile = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
                    context.contentResolver.openInputStream(android.net.Uri.parse(localPath))?.use { input ->
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
                    // ⚡ Chỉ lưu relative path (/uploads/xxx.jpg), KHÔNG prepend BASE_URL.
                    // Client sẽ ghép BASE_URL khi render (xem ImageRequestUtils.kt).
                    // Tránh lưu IP cứng (vd: 10.0.2.2) → chết trên máy thật.
                    return@withContext serverPath
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
            val uploadedNoBg = when {
                item.imageNoBg.isBlank() -> ""
                item.imageNoBg == item.imageOriginal -> uploadedOriginal
                else -> uploadImageToServer(item.imageNoBg)
            }
            val dto = item.toDto().copy(
                imageOriginal = uploadedOriginal,
                imageNoBg = uploadedNoBg
            )
            apiService.createClothingItem(dto)
            invalidateCache()
        } catch (e: Exception) {
            Log.e(TAG, "❌ insertItem() lỗi: ${e.message}")
        }
    }

    suspend fun updateItem(item: ClothingItemEntity) = withContext(Dispatchers.IO) {
        try {
            val uploadedOriginal = uploadImageToServer(item.imageOriginal)
            val uploadedNoBg = when {
                item.imageNoBg.isBlank() -> ""
                item.imageNoBg == item.imageOriginal -> uploadedOriginal
                else -> uploadImageToServer(item.imageNoBg)
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
            invalidateCache()
        } catch (e: Exception) {
            Log.e(TAG, "❌ updateItem() lỗi: ${e.message}")
        }
    }

    suspend fun deleteItem(item: ClothingItemEntity) = withContext(Dispatchers.IO) {
        try {
            apiService.deleteClothingItem(item.id)
            invalidateCache()
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
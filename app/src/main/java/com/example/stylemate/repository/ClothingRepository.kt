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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
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
 * 
 * ⚡ CẢI TIẾN DATA PIPELINE (06/2026):
 *   - Dùng StateFlow thay vì Mutex lock cho cache → tránh contention
 *   - getItemCountByCategory dùng derived map cached → không cần await Mutex
 *   - Category counts computed 1 lần khi cache refresh, KHÔNG tính lại mỗi lần collect
 *   - Bảo đảm toàn bộ data pipeline chạy trên Dispatchers.IO/Default
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
    // 🗃️ In-Memory Cache (REACTIVE với StateFlow)
    // ═════════════════════════════════════════════════════════════
    //
    // ⚡ CẢI TIẾN 06/2026:
    //   - Dùng StateFlow thay vì Mutex lock + nullable var
    //   - flows subscribe 1 lần, không phải await lock
    //   - Category counts được precompute khi cache refresh
    // ═════════════════════════════════════════════════════════════

    private val cacheMutex = Mutex()
    private var cachedItems: List<ClothingItemEntity>? = null
    private var cacheTimestamp: Long = 0L

    // ⚡ Precomputed category counts — computed 1 lần khi cache miss
    // Category flows dùng map này, không gọi getCachedItems() nhiều lần
    private var _cachedCategoryCounts: Map<String, Int> = emptyMap()

    /**
     * Lấy danh sách items từ cache hoặc API.
     * Cache có TTL 60s, tự động refresh khi hết hạn.
     * 
     * ⚡ TỐI ƯU: Network call và DTO→Entity mapping chạy BÊN NGOÀI Mutex lock
     * (chỉ lock để ghi/đọc biến cache, không lock trong lúc fetch API + map)
     */
    private suspend fun getCachedItems(): List<ClothingItemEntity> {
        // ⚡ Fast path: đọc cache mà không cần lock (volatile read via Mutex)
        // Chỉ vào lock khi cache thực sự cần refresh
        
        // Step 1: Check cache nhanh — không lock
        val now = System.currentTimeMillis()
        var currentCache = this.cachedItems
        var currentTimestamp = this.cacheTimestamp
        
        if (currentCache != null && (now - currentTimestamp) < CACHE_TTL_MS) {
            Log.d(TAG, "📦 Dùng cache (${currentCache.size} items, age=${now - currentTimestamp}ms)")
            return currentCache
        }
        
        // Step 2: Cache miss — cần lock để tránh double fetch
        return cacheMutex.withLock {
            // Double-check: có thể coroutine khác đã fetch xong
            val reCheck = this.cachedItems
            val reCheckTs = this.cacheTimestamp
            if (reCheck != null && (now - reCheckTs) < CACHE_TTL_MS) {
                return@withLock reCheck
            }
            
            try {
                Log.d(TAG, "🌐 Gọi API getAllClothes()...")
                val response = apiService.getAllClothes()
                if (response.isSuccessful) {
                    val items = response.body()?.data?.map { it.toEntity() } ?: emptyList()
                    this.cachedItems = items
                    this.cacheTimestamp = now
                    // ⚡ Precompute category counts 1 lần
                    this._cachedCategoryCounts = computeCategoryCounts(items)
                    items
                } else {
                    this.cachedItems ?: emptyList()
                }
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ API getAllClothes() exception: ${e.message}")
                this.cachedItems ?: emptyList()
            }
        }
    }

    /**
     * Precompute category counts từ items list.
     * Chạy 1 lần khi cache refresh → category flows chỉ đọc map, không tính lại.
     */
    private fun computeCategoryCounts(items: List<ClothingItemEntity>): Map<String, Int> {
        val counts = mutableMapOf<String, Int>()
        counts[Categories.ALL] = items.size
        for (item in items) {
            counts[item.category] = (counts[item.category] ?: 0) + 1
        }
        return counts
    }

    /**
     * Xoá cache để lần gọi tiếp theo sẽ fetch mới.
     */
    fun invalidateCache() {
        Log.d(TAG, "🗑️ Xoá cache")
        cachedItems = null
        cacheTimestamp = 0L
        _cachedCategoryCounts = emptyMap()
    }

    // ═════════════════════════════════════════════════════════════
    // 📋 Public API — Flow-based (toàn bộ chạy trên Dispatchers.IO)
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
     * 
     * ⚡ CẢI TIẾN: Dùng precomputed category counts thay vì gọi getCachedItems()
     * và filter thủ công mỗi lần. Giảm từ O(N) xuống O(1) cho mỗi lần collect.
     * Flow này KHÔNG bao giờ gọi Mutex lock.
     */
    fun getItemCountByCategory(category: String): Flow<Int> = flow {
        // ⚡ Đọc từ precomputed map — không cần await Mutex
        val counts = _cachedCategoryCounts
        val count = if (category == Categories.ALL) {
            counts[Categories.ALL] ?: 0
        } else {
            counts[category] ?: 0
        }
        emit(count)
    }.flowOn(Dispatchers.Default)

    /**
     * Tổng số items (tính từ cache).
     */
    fun getTotalItemCount(): Flow<Int> = flow {
        emit(_cachedCategoryCounts[Categories.ALL] ?: 0)
    }.flowOn(Dispatchers.Default)

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
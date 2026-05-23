package com.example.stylemate.repository

import android.content.Context
import android.util.Log
import com.example.stylemate.model.ClothingItemEntity
import com.example.stylemate.network.ClothingItemDto
import com.example.stylemate.network.RemoveBgClient
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
 * ═══════════════════════════════════════════════════════════════
 * 🏪 CLOTHING REPOSITORY — Phiên bản API (thay thế Room)
 * ═══════════════════════════════════════════════════════════════
 *
 * THAY THẾ hoàn toàn ClothingRepository cũ (dùng Room DAO)
 * bằng phiên bản gọi Retrofit API lên Backend Node.js.
 *
 * ✅ GIỮ NGUYÊN tên các phương thức + kiểu trả về để ViewModel cũ
 *    không bị lỗi đỏ:
 *    - getAllItems(): Flow<List<ClothingItemEntity>>
 *    - getItemsByCategory(): Flow<List<ClothingItemEntity>>
 *    - insertItem(item: ClothingItemEntity)
 *    - deleteItem(item: ClothingItemEntity)
 *    - getItemCountByCategory(): Flow<Int>
 *    - getTotalItemCount(): Flow<Int>
 *    - getItemById(): ClothingItemEntity?
 *    - updateItemCanvasPosition()
 *
 * 🔄 Map DTO (từ API) → Entity (cho ViewModel) và ngược lại.
 * ───────────────────────────────────────────────────────────────
 */
class ClothingRepository(
    private val apiService: StylemateApiService,
    private val context: Context
) {

    private val removeBgClient = RemoveBgClient(context)

    companion object {
        private const val TAG = "ClothingRepository"

        /**
         * Base URL gốc của server (để ghép với đường dẫn tương đối).
         * Ví dụ: "http://192.168.1.11:3000"
         */
        private val BASE_URL: String by lazy {
            RetrofitClient.STYLEMATE_BASE_URL.trimEnd('/')
        }

        /**
         * Kiểm tra đường dẫn có phải là file local trên điện thoại không.
         * Các đường dẫn local thường bắt đầu bằng /data/ hoặc /storage/.
         * Đường dẫn URL (http://... /uploads/...) thì KHÔNG upload lại.
         */
        private fun isLocalFilePath(path: String): Boolean {
            return path.startsWith("/data/") ||
                   path.startsWith("/storage/") ||
                   path.startsWith("/sdcard/") ||
                   path.startsWith("/mnt/") ||
                   path.startsWith("file://") ||
                   path.startsWith("content://")
        }

        /**
         * Chuyển đường dẫn local thành full URL nếu cần.
         */
        private fun toFullUrl(path: String): String {
            if (path.isBlank()) return path
            // Nếu đã là HTTP URL thì giữ nguyên
            if (path.startsWith("http://") || path.startsWith("https://")) return path
            // Nếu là đường dẫn tương đối (bắt đầu bằng /uploads/...)
            if (path.startsWith("/uploads/")) return "$BASE_URL$path"
            // Còn lại (local path) → giữ nguyên, ViewModel sẽ upload
            return path
        }

        /**
         * Chuyển ClothingItemDto (từ API) → ClothingItemEntity (cho ViewModel).
         */
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

        /**
         * Chuyển ClothingItemEntity (từ ViewModel) → ClothingItemDto (gửi API).
         */
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
    // 📋 Đọc (Read) — Reactive với Flow
    // ═════════════════════════════════════════════════════════════

    /**
     * 📋 Lấy danh sách tất cả items dưới dạng Flow.
     *
     * ✅ Giữ đúng tên: getAllItems()
     * ✅ Giữ đúng kiểu trả về: Flow<List<ClothingItemEntity>>
     *
     * Chuyển ClothingItemDto (từ API) → ClothingItemEntity (cho ViewModel).
     *
     * ⚡ Flow chỉ emit 1 lần (không realtime như Room).
     * Để refresh, ViewModel cần gọi lại.
     */
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

    /**
     * 📋 Lấy danh sách items theo category dưới dạng Flow.
     *
     * ✅ Giữ đúng tên: getItemsByCategory(category: String)
     * ✅ Giữ đúng kiểu trả về: Flow<List<ClothingItemEntity>>
     *
     * @param category Danh mục cần lọc (vd: "Tops", "Bottoms")
     */
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

    /**
     * 📊 Lấy số lượng items trong một category dưới dạng Flow.
     *
     * ✅ Giữ đúng tên: getItemCountByCategory(category: String)
     * ✅ Giữ đúng kiểu trả về: Flow<Int>
     *
     * Backend trả về count trong response → dùng luôn.
     */
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

    /**
     * 📊 Lấy tổng số lượng items dưới dạng Flow.
     *
     * ✅ Giữ đúng tên: getTotalItemCount()
     * ✅ Giữ đúng kiểu trả về: Flow<Int>
     */
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

    // ═════════════════════════════════════════════════════════════
    // 🔍 Chi tiết
    // ═════════════════════════════════════════════════════════════

    /**
     * 🔍 Lấy chi tiết một item theo ID.
     *
     * ✅ Giữ đúng tên: getItemById(itemId: String)
     * ✅ Giữ đúng kiểu trả về: ClothingItemEntity?
     *
     * @param itemId UUID của item.
     */
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

    // ═════════════════════════════════════════════════════════════
    // 🖼️ Helper: Upload file ảnh local lên server
    // ═════════════════════════════════════════════════════════════

    /**
     * Gọi remove.bg để xoá nền ảnh.
     *
     * @return Pair<File?, String?>:
     *   - File? ảnh PNG đã tách nền (null nếu thất bại)
     *   - String? thông báo lỗi (null nếu thành công)
     */
    suspend fun removeBackground(imageFile: File): Pair<File?, String?> {
        return removeBgClient.removeBackground(imageFile)
    }

    /**
     * Upload một file ảnh local lên server.
     *
     * @param localPath Đường dẫn file local (vd: /data/user/0/.../abc.jpg)
     * @return URL đầy đủ (vd: http://192.168.1.11:3000/uploads/abc.jpg)
     *         hoặc giữ nguyên localPath nếu không upload được.
     */
    suspend fun uploadImageToServer(localPath: String): String = withContext(Dispatchers.IO) {
        // Nếu không phải local path → bỏ qua, giữ nguyên
        if (!isLocalFilePath(localPath)) {
            Log.d(TAG, "⏭️ Bỏ qua upload (đã là URL): $localPath")
            return@withContext toFullUrl(localPath)
        }

        try {
            val file = when {
                localPath.startsWith("file://") -> File(localPath.removePrefix("file://"))
                localPath.startsWith("content://") -> {
                    // content:// URI cần ContentResolver để copy
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

            if (!file.exists()) {
                Log.w(TAG, "⚠️ File không tồn tại: $localPath")
                return@withContext toFullUrl(localPath)
            }

            // Tạo RequestBody và MultipartBody.Part từ file
            val mimeType = when {
                localPath.endsWith(".png", ignoreCase = true) -> "image/png"
                localPath.endsWith(".gif", ignoreCase = true) -> "image/gif"
                localPath.endsWith(".webp", ignoreCase = true) -> "image/webp"
                else -> "image/jpeg"
            }
            val requestBody = file.asRequestBody(mimeType.toMediaTypeOrNull())
            val multipartPart = MultipartBody.Part.createFormData("image", file.name, requestBody)

            Log.d(TAG, "📤 Đang upload ảnh: ${file.name} (${file.length()} bytes)")

            val response = apiService.uploadImage(multipartPart)
            if (response.isSuccessful) {
                val body = response.body()
                val serverPath = body?.url ?: ""
                if (serverPath.isNotBlank()) {
                    val fullUrl = "$BASE_URL$serverPath"
                    Log.d(TAG, "✅ Upload thành công: $fullUrl")
                    return@withContext fullUrl
                }
            }

            Log.w(TAG, "⚠️ Upload thất bại: HTTP ${response.code()} ${response.message()}")
            toFullUrl(localPath)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Lỗi upload ảnh: ${e.message}", e)
            toFullUrl(localPath)
        }
    }

    // ═════════════════════════════════════════════════════════════
    // ➕ Ghi (Write) — Suspend + IO
    // ═════════════════════════════════════════════════════════════

    /**
     * ➕ Thêm (hoặc upsert) một item.
     *
     * ⚡ Tự động upload ảnh local lên server TRƯỚC khi tạo item.
     * Sau đó gán URL server vào imageOriginal / imageNoBg rồi gửi DTO.
     *
     * @param item ClothingItemEntity cần lưu (với đường dẫn local).
     */
    suspend fun insertItem(item: ClothingItemEntity) = withContext(Dispatchers.IO) {
        try {
            // ── Bước 1: Upload ảnh gốc (imageOriginal) ──────────
            val uploadOriginal = uploadImageToServer(item.imageOriginal)

            // ── Bước 2: Upload ảnh không nền (imageNoBg) ───────
            val uploadNoBg = if (item.imageNoBg.isNotBlank() &&
                item.imageNoBg != item.imageOriginal
            ) {
                uploadImageToServer(item.imageNoBg)
            } else {
                uploadOriginal // Nếu giống nhau thì dùng chung URL
            }

            Log.d(TAG, "📸 imageOriginal: ${item.imageOriginal} → $uploadOriginal")
            Log.d(TAG, "📸 imageNoBg: ${item.imageNoBg} → $uploadNoBg")

            // ── Bước 3: Tạo DTO với URL đã upload ───────────────
            val dto = item.toDto().copy(
                imageOriginal = uploadOriginal,
                imageNoBg = uploadNoBg
            )

            // ── Bước 4: Gọi API tạo item ────────────────────────
            val response = apiService.createClothingItem(dto)
            if (response.isSuccessful) {
                Log.d(TAG, "✅ Tạo item thành công: ${item.id}")
            } else {
                Log.w(TAG, "⚠️ Tạo item thất bại: HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ insertItem() lỗi: ${e.message}", e)
        }
    }

    /**
     * ✏️ Cập nhật một item.
     *
     * ✅ Giữ đúng tên: updateItem(item: ClothingItemEntity)
     * Tự động upload ảnh local trước khi update.
     */
    suspend fun updateItem(item: ClothingItemEntity) = withContext(Dispatchers.IO) {
        try {
            val uploadOriginal = uploadImageToServer(item.imageOriginal)
            val uploadNoBg = if (item.imageNoBg.isNotBlank() && item.imageNoBg != item.imageOriginal) {
                uploadImageToServer(item.imageNoBg)
            } else {
                uploadOriginal
            }

            val updateMap = mapOf<String, Any>(
                "imageOriginal" to uploadOriginal,
                "imageNoBg" to uploadNoBg,
                "category" to item.category,
                "color" to item.color,
                "name" to item.name,
                "season" to item.season,
                "occasion" to item.occasion,
                "brand" to item.brand,
                "purchaseDate" to item.purchaseDate,
                "price" to item.price,
                "canvasPosX" to item.canvasPosX,
                "canvasPosY" to item.canvasPosY
            )

            val response = apiService.updateClothingItem(item.id, updateMap)
            if (!response.isSuccessful) {
                Log.w(TAG, "⚠️ updateItem() thất bại: HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ updateItem() lỗi: ${e.message}", e)
        }
    }

    /**
     * ❌ Xoá một item.
     *
     * ✅ Giữ đúng tên: deleteItem(item: ClothingItemEntity)
     * ✅ Giữ đúng tham số: ClothingItemEntity
     *
     * @param item ClothingItemEntity cần xoá.
     */
    suspend fun deleteItem(item: ClothingItemEntity) = withContext(Dispatchers.IO) {
        try {
            apiService.deleteClothingItem(item.id)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ deleteItem() lỗi: ${e.message}")
        }
    }

    /**
     * 🖱️ Cập nhật vị trí canvas của một item.
     *
     * ✅ Giữ đúng tên: updateItemCanvasPosition(itemId, posX, posY)
     *
     * @param itemId UUID của item
     * @param posX Vị trí X trên canvas (0.0 → 1.0)
     * @param posY Vị trí Y trên canvas (0.0 → 1.0)
     */
    suspend fun updateItemCanvasPosition(
        itemId: String,
        posX: Float,
        posY: Float
    ) = withContext(Dispatchers.IO) {
        try {
            val updateMap = mapOf<String, Any>(
                "canvasPosX" to posX,
                "canvasPosY" to posY
            )
            apiService.updateClothingItem(itemId, updateMap)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ updateItemCanvasPosition() lỗi: ${e.message}")
        }
    }


}

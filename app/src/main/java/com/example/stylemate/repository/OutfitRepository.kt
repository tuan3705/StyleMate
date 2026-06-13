package com.example.stylemate.repository

import android.util.Log
import com.example.stylemate.model.ClothingItemEntity
import com.example.stylemate.model.OutfitClothingCrossRef
import com.example.stylemate.model.OutfitEntity
import com.example.stylemate.model.OutfitItemWithPosition
import com.example.stylemate.model.OutfitWithClothingItems
import com.example.stylemate.network.ClothingItemDto
import com.example.stylemate.network.OutfitDto
import com.example.stylemate.network.StylemateApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * ═══════════════════════════════════════════════════════════════
 * 🏪 OUTFIT REPOSITORY — Phiên bản API (thay thế Room)
 * ═══════════════════════════════════════════════════════════════
 *
 * ⚡ CẢI TIẾN HIỆU NĂNG (06/2026):
 *   - Thêm ObjectCache dùng chung để cache danh sách clothes 60s
 *   - getAllOutfitsWithItems() chỉ gọi getAllClothes() 1 lần duy nhất
 *   - getOutfitItemsWithPosition() dùng cache thay vì gọi API riêng
 *   - Tránh N+1 API call khi hiển thị nhiều Outfit Card
 *
 * 🔄 Backend trả về OutfitDto (items lồng sẵn trong clothingItems).
 *    Repository map DTO → Entity (OutfitWithClothingItems, OutfitItemWithPosition...)
 *    để giữ nguyên kiểu trả về cho ViewModel.
 * ───────────────────────────────────────────────────────────────
 */
class OutfitRepository(private val apiService: StylemateApiService) {

    companion object {
        private const val TAG = "OutfitRepository"
        private const val CACHE_TTL_MS = 60_000L // 60 giây

        /**
         * Chuyển OutfitDto (từ API) thành OutfitWithClothingItems (cho ViewModel).
         */
        fun OutfitDto.toOutfitWithClothingItems(): OutfitWithClothingItems {
            val outfitEntity = OutfitEntity(
                id = this.id,
                name = this.name,
                createdAt = this.createdAt
            )
            val clothingItems = this.clothingItems.map { ref ->
                ClothingItemEntity(
                    id = ref.clothingItemId,
                    imageOriginal = "",
                    imageNoBg = "",
                    category = "",
                    color = "",
                    name = "",
                    season = "",
                    occasion = "",
                    brand = "",
                    purchaseDate = 0L,
                    price = 0.0,
                    canvasPosX = ref.posX,
                    canvasPosY = ref.posY,
                    createdAt = 0L
                )
            }
            return OutfitWithClothingItems(
                outfit = outfitEntity,
                clothingItems = clothingItems
            )
        }
    }

    // ═════════════════════════════════════════════════════════════
    // 🗃️ Shared Object Cache (thread-safe)
    // ═════════════════════════════════════════════════════════════

    private val clothesCacheMutex = Mutex()
    private var cachedAllClothes: Map<String, ClothingItemEntity>? = null
    private var clothesCacheTimestamp: Long = 0L

    /**
     * Lấy danh sách clothes đã cache (60s TTL).
     * Dùng map<String, ClothingItemEntity> để lookup nhanh theo ID.
     */
    private suspend fun getCachedClothesMap(): Map<String, ClothingItemEntity> = clothesCacheMutex.withLock {
        val now = System.currentTimeMillis()
        if (cachedAllClothes != null && (now - clothesCacheTimestamp) < CACHE_TTL_MS) {
            return@withLock cachedAllClothes!!
        }
        try {
            Log.d(TAG, "📦 Cache clothes miss, gọi API getAllClothes()...")
            val response = apiService.getAllClothes()
            val clothesMap: Map<String, ClothingItemEntity> = if (response.isSuccessful) {
                val items = response.body()?.data ?: emptyList()
                items.associate { dto ->
                    dto.id to ClothingItemEntity(
                        id = dto.id,
                        imageOriginal = dto.imageOriginal,
                        imageNoBg = dto.imageNoBg,
                        category = dto.category,
                        color = dto.color,
                        name = dto.name,
                        season = dto.season,
                        occasion = dto.occasion,
                        brand = dto.brand,
                        purchaseDate = dto.purchaseDate,
                        price = dto.price,
                        canvasPosX = dto.canvasPosX,
                        canvasPosY = dto.canvasPosY,
                        createdAt = dto.createdAt
                    )
                }
            } else {
                cachedAllClothes ?: emptyMap()
            }
            cachedAllClothes = clothesMap
            clothesCacheTimestamp = now
            clothesMap
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ API getAllClothes() exception: ${e.message}")
            cachedAllClothes ?: emptyMap()
        }
    }

    /**
     * Xoá cache để lần gọi tiếp theo fetch mới.
     */
    fun invalidateClothesCache() {
        cachedAllClothes = null
        clothesCacheTimestamp = 0L
    }

    /**
     * Map OutfitDto thành OutfitWithClothingItems với đầy đủ thông tin item.
     * Dùng cache clothes để tránh gọi API riêng cho từng outfit.
     */
    private suspend fun OutfitDto.toFullOutfitWithItems(clothesMap: Map<String, ClothingItemEntity>): OutfitWithClothingItems {
        val outfitEntity = OutfitEntity(
            id = this.id,
            name = this.name,
            createdAt = this.createdAt
        )
        val clothingItems = this.clothingItems.mapNotNull { ref ->
            val fullItem = clothesMap[ref.clothingItemId]
            if (fullItem != null) {
                ClothingItemEntity(
                    id = fullItem.id,
                    imageOriginal = fullItem.imageOriginal,
                    imageNoBg = fullItem.imageNoBg,
                    category = fullItem.category,
                    color = fullItem.color,
                    name = fullItem.name,
                    season = fullItem.season,
                    occasion = fullItem.occasion,
                    brand = fullItem.brand,
                    purchaseDate = fullItem.purchaseDate,
                    price = fullItem.price,
                    canvasPosX = ref.posX,
                    canvasPosY = ref.posY,
                    canvasScale = ref.scale,
                    createdAt = fullItem.createdAt
                )
            } else null
        }
        return OutfitWithClothingItems(
            outfit = outfitEntity,
            clothingItems = clothingItems
        )
    }

    /**
     * Map OutfitDto thành OutfitItemWithPosition (kèm vị trí).
     */
    private suspend fun OutfitDto.toOutfitItemsWithPosition(clothesMap: Map<String, ClothingItemEntity>): List<OutfitItemWithPosition> {
        return this.clothingItems.mapNotNull { ref ->
            val fullItem = clothesMap[ref.clothingItemId]
            if (fullItem != null) {
                OutfitItemWithPosition(
                    item = ClothingItemEntity(
                        id = fullItem.id,
                        imageOriginal = fullItem.imageOriginal,
                        imageNoBg = fullItem.imageNoBg,
                        category = fullItem.category,
                        color = fullItem.color,
                        name = fullItem.name,
                        season = fullItem.season,
                        occasion = fullItem.occasion,
                        brand = fullItem.brand,
                        purchaseDate = fullItem.purchaseDate,
                        price = fullItem.price,
                        canvasPosX = ref.posX,
                        canvasPosY = ref.posY,
                        createdAt = fullItem.createdAt
                    ),
                    posX = ref.posX,
                    posY = ref.posY,
                    scale = ref.scale
                )
            } else null
        }
    }

    // ═════════════════════════════════════════════════════════════
    // 📋 Đọc (Read) — Reactive với Flow
    // ═════════════════════════════════════════════════════════════

    /**
     * Lấy danh sách tất cả Outfit (kèm ClothingItem đầy đủ) dưới dạng Flow.
     * 
     * ⚡ Tối ưu: Chỉ gọi API getAllClothes() 1 lần dùng chung cho tất cả outfits.
     * Dùng cache 60s để tránh gọi lại khi chuyển tab.
     */
    fun getAllOutfitsWithItems(nameQuery: String? = null): Flow<List<OutfitWithClothingItems>> = flow {
        try {
            val outfitsResponse = apiService.getAllOutfits(
                name = nameQuery?.trim()?.takeIf { it.isNotBlank() }
            )

            if (outfitsResponse.isSuccessful) {
                val outfits = outfitsResponse.body()?.data ?: emptyList()
                if (outfits.isEmpty()) {
                    emit(emptyList())
                    return@flow
                }

                // ⚡ Chỉ gọi getAllClothes() 1 lần (dùng cache 60s)
                val clothesMap = getCachedClothesMap()
                val result = outfits.map { outfitDto ->
                    outfitDto.toFullOutfitWithItems(clothesMap)
                }
                emit(result)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            Log.w(TAG, "getAllOutfitsWithItems() loi: ${e.message}")
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Lấy chi tiết một Outfit theo ID.
     */
    suspend fun getOutfitWithItemsById(outfitId: String): OutfitWithClothingItems? =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getOutfitById(outfitId)
                if (response.isSuccessful) {
                    val outfitDto = response.body()?.data ?: return@withContext null
                    val clothesMap = getCachedClothesMap()
                    outfitDto.toFullOutfitWithItems(clothesMap)
                } else null
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ getOutfitWithItemsById() lỗi: ${e.message}")
                null
            }
        }

    /**
     * Lấy danh sách Outfit có chứa một ClothingItem cụ thể.
     */
    fun getOutfitsContainingItem(clothingItemId: String): Flow<List<OutfitWithClothingItems>> =
        flow {
            try {
                val response = apiService.getOutfitsContainingItem(clothingItemId)
                if (response.isSuccessful) {
                    val body = response.body()?.data ?: emptyList()
                    val clothesMap = getCachedClothesMap()
                    emit(body.map { it.toFullOutfitWithItems(clothesMap) })
                } else {
                    emit(emptyList())
                }
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ getOutfitsContainingItem() lỗi: ${e.message}")
                emit(emptyList())
            }
        }.flowOn(Dispatchers.IO)

    /**
     * Lấy danh sách items trong outfit kèm vị trí đã lưu.
     * 
     * ⚡ Tối ưu: Dùng cache clothes thay vì gọi lại API getAllClothes().
     */
    suspend fun getOutfitItemsWithPosition(outfitId: String): List<OutfitItemWithPosition> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getOutfitById(outfitId)
                if (response.isSuccessful) {
                    val outfitDto = response.body()?.data ?: return@withContext emptyList()
                    val clothesMap = getCachedClothesMap()
                    outfitDto.toOutfitItemsWithPosition(clothesMap)
                } else emptyList()
            } catch (e: Exception) {
                Log.w(TAG, "getOutfitItemsWithPosition() loi: ${e.message}")
                emptyList()
            }
        }

    // ═════════════════════════════════════════════════════════════
    // ➕ Ghi (Write) — Suspend + IO
    // ═════════════════════════════════════════════════════════════

    suspend fun insertOutfit(outfit: OutfitEntity) = withContext(Dispatchers.IO) {
        try {
            val dto = OutfitDto(
                id = outfit.id,
                name = outfit.name,
                clothingItems = emptyList(),
                createdAt = outfit.createdAt
            )
            apiService.createOutfit(dto)
            invalidateClothesCache()
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ insertOutfit() lỗi: ${e.message}")
        }
    }

    suspend fun insertOutfitClothingCrossRefs(crossRefs: List<OutfitClothingCrossRef>) =
        withContext(Dispatchers.IO) {
            try {
                val outfitId = crossRefs.firstOrNull()?.outfitId ?: return@withContext
                val itemsPayload = crossRefs.map { ref ->
                    mapOf(
                        "clothingItemId" to ref.clothingItemId,
                        "posX" to ref.posX,
                        "posY" to ref.posY,
                        "scale" to ref.scale
                    )
                }
                val updateMap = mapOf<String, Any>("clothingItems" to itemsPayload)
                apiService.updateOutfit(outfitId, updateMap)
                invalidateClothesCache()
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ insertOutfitClothingCrossRefs() lỗi: ${e.message}")
            }
        }

    suspend fun clearOutfitItems(outfitId: String) = withContext(Dispatchers.IO) {
        try {
            val updateMap = mapOf<String, Any>("clothingItems" to emptyList<Map<String, Any>>())
            apiService.updateOutfit(outfitId, updateMap)
            invalidateClothesCache()
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ clearOutfitItems() lỗi: ${e.message}")
        }
    }

    suspend fun deleteOutfit(outfit: OutfitEntity) = withContext(Dispatchers.IO) {
        try {
            apiService.deleteOutfit(outfit.id)
            invalidateClothesCache()
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ deleteOutfit() lỗi: ${e.message}")
        }
    }
}
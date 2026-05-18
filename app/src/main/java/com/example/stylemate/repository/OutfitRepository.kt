package com.example.stylemate.repository

import android.util.Log
import com.example.stylemate.model.ClothingItemEntity
import com.example.stylemate.model.OutfitClothingCrossRef
import com.example.stylemate.model.OutfitEntity
import com.example.stylemate.model.OutfitItemWithPosition
import com.example.stylemate.model.OutfitWithClothingItems
import com.example.stylemate.network.OutfitClothingItemRefDto
import com.example.stylemate.network.OutfitDto
import com.example.stylemate.network.StylemateApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * ═══════════════════════════════════════════════════════════════
 * 🏪 OUTFIT REPOSITORY — Phiên bản API (thay thế Room)
 * ═══════════════════════════════════════════════════════════════
 *
 * THAY THẾ hoàn toàn OutfitRepository cũ (dùng Room DAO)
 * bằng phiên bản gọi Retrofit API lên Backend Node.js.
 *
 * ✅ GIỮ NGUYÊN tên các phương thức + kiểu trả về để ViewModel cũ
 *    không bị lỗi đỏ:
 *    - getAllOutfitsWithItems(): Flow<List<OutfitWithClothingItems>>
 *    - getOutfitWithItemsById(): OutfitWithClothingItems?
 *    - getOutfitsContainingItem(): Flow<List<OutfitWithClothingItems>>
 *    - getOutfitItemsWithPosition(): List<OutfitItemWithPosition>
 *    - insertOutfit(), insertOutfitClothingCrossRefs(), clearOutfitItems()
 *    - deleteOutfit()
 *
 * 🔄 Backend trả về OutfitDto (items lồng sẵn trong clothingItems).
 *    Repository map DTO → Entity (OutfitWithClothingItems, OutfitItemWithPosition...)
 *    để giữ nguyên kiểu trả về cho ViewModel.
 * ───────────────────────────────────────────────────────────────
 */
class OutfitRepository(private val apiService: StylemateApiService) {

    companion object {
        private const val TAG = "OutfitRepository"

        /**
         * Chuyển OutfitDto (từ API) thành OutfitWithClothingItems (cho ViewModel).
         *
         * Backend trả về clothingItems là mảng { clothingItemId, posX, posY }
         * KHÔNG có ClothingItemDto đầy đủ.
         *
         * Nếu cần populate, ViewModel nên gọi riêng API Clothes.
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
    // 📋 Đọc (Read) — Reactive với Flow
    // ═════════════════════════════════════════════════════════════

    /**
     * Lấy danh sach tat ca Outfit (kem ClothingItem ben trong) duoi dang Flow.
     *
     * Giai phap:
     *   1. Goi GET /api/outfits -> lay outfit + itemReferences
     *   2. Goi GET /api/clothes    -> lay danh sach ClothingItem day du
     *   3. Map itemReferences sang ClothingItemEntity (co image, name, category...)
     *
     * Nho vay anh, ten, category duoc hien thi dung trong Outfit card.
     */
    fun getAllOutfitsWithItems(): Flow<List<OutfitWithClothingItems>> = flow {
        try {
            // ── Bước 1: Lấy danh sách Outfit (không populate) ──
            val outfitsResponse = apiService.getAllOutfits()

            // ── Bước 2: Lấy danh sách ClothingItem đầy đủ ────
            val clothesResponse = apiService.getAllClothes()

            if (outfitsResponse.isSuccessful && clothesResponse.isSuccessful) {
                val outfits = outfitsResponse.body()?.data ?: emptyList()
                val allClothes = clothesResponse.body()?.data ?: emptyList()

                // Map clothingItemId -> ClothingItemDto
                val clothesMap = allClothes.associateBy { it.id }

                val result = outfits.map { outfitDto ->
                    val clothingItems = outfitDto.clothingItems.mapNotNull { ref ->
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
                                createdAt = fullItem.createdAt
                            )
                        } else null
                    }

                    OutfitWithClothingItems(
                        outfit = OutfitEntity(
                            id = outfitDto.id,
                            name = outfitDto.name,
                            createdAt = outfitDto.createdAt
                        ),
                        clothingItems = clothingItems
                    )
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
     * 📋 Lấy chi tiết một Outfit theo ID.
     *
     * ✅ Giữ đúng tên: getOutfitWithItemsById(outfitId: String)
     * ✅ Giữ đúng kiểu trả về: OutfitWithClothingItems?
     *
     * @param outfitId UUID của outfit.
     */
    suspend fun getOutfitWithItemsById(outfitId: String): OutfitWithClothingItems? =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getOutfitById(outfitId)
                if (response.isSuccessful) {
                    response.body()?.data?.toOutfitWithClothingItems()
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ getOutfitWithItemsById() lỗi: ${e.message}")
                null
            }
        }

    /**
     * 📋 Lấy danh sách Outfit có chứa một ClothingItem cụ thể.
     *
     * ✅ Giữ đúng tên: getOutfitsContainingItem(clothingItemId: String)
     * ✅ Giữ đúng kiểu trả về: Flow<List<OutfitWithClothingItems>>
     *
     * @param clothingItemId UUID của ClothingItem.
     */
    fun getOutfitsContainingItem(clothingItemId: String): Flow<List<OutfitWithClothingItems>> =
        flow {
            try {
                val response = apiService.getOutfitsContainingItem(clothingItemId)
                if (response.isSuccessful) {
                    val body = response.body()
                    emit(body?.data?.map { it.toOutfitWithClothingItems() } ?: emptyList())
                } else {
                    emit(emptyList())
                }
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ getOutfitsContainingItem() lỗi: ${e.message}")
                emit(emptyList())
            }
        }.flowOn(Dispatchers.IO)

    /**
     * 📋 Lấy danh sách items trong outfit kèm vị trí đã lưu.
     *
     * ✅ Giữ đúng tên: getOutfitItemsWithPosition(outfitId: String)
     * ✅ Giữ đúng kiểu trả về: List<OutfitItemWithPosition>
     *
     * @param outfitId UUID của outfit.
     */
    suspend fun getOutfitItemsWithPosition(outfitId: String): List<OutfitItemWithPosition> =
        withContext(Dispatchers.IO) {
            try {
                // ── Bước 1: Lấy outfit (chứa references) ────
                val response = apiService.getOutfitById(outfitId)

                // ── Bước 2: Lấy full clothing items ────────
                val clothesResponse = apiService.getAllClothes()

                if (response.isSuccessful && clothesResponse.isSuccessful) {
                    val outfitDto = response.body()?.data
                    val allClothes = clothesResponse.body()?.data ?: emptyList()
                    val clothesMap = allClothes.associateBy { it.id }

                    outfitDto?.let { dto ->
                        dto.clothingItems.mapNotNull { ref ->
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
                                    posY = ref.posY
                                )
                            } else null
                        }
                    } ?: emptyList()
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                Log.w(TAG, "getOutfitItemsWithPosition() loi: ${e.message}")
                emptyList()
            }
        }

    // ═════════════════════════════════════════════════════════════
    // ➕ Ghi (Write) — Suspend + IO
    // ═════════════════════════════════════════════════════════════

    /**
     * ➕ Tạo một Outfit mới.
     *
     * ✅ Giữ đúng tên: insertOutfit(outfit: OutfitEntity)
     *
     * Chuyển Entity → DTO → gọi API POST.
     *
     * @param outfit OutfitEntity cần lưu.
     */
    suspend fun insertOutfit(outfit: OutfitEntity) = withContext(Dispatchers.IO) {
        try {
            val dto = OutfitDto(
                id = outfit.id,
                name = outfit.name,
                clothingItems = emptyList(),
                createdAt = outfit.createdAt
            )
            apiService.createOutfit(dto)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ insertOutfit() lỗi: ${e.message}")
        }
    }

    /**
     * 🔗 Liên kết danh sách ClothingItem vào một Outfit.
     *
     * ✅ Giữ đúng tên: insertOutfitClothingCrossRefs(crossRefs: List<OutfitClothingCrossRef>)
     * ✅ Giữ đúng tham số (1 param) để ViewModel cũ gọi được.
     *
     * Backend KHÔNG có bảng CrossRef. Phương thức này update outfit
     * với danh sách clothingItems mới qua PUT.
     *
     * @param crossRefs Danh sách OutfitClothingCrossRef.
     */
    suspend fun insertOutfitClothingCrossRefs(crossRefs: List<OutfitClothingCrossRef>) =
        withContext(Dispatchers.IO) {
            try {
                val outfitId = crossRefs.firstOrNull()?.outfitId ?: return@withContext
                val itemsPayload = crossRefs.map { ref ->
                    mapOf(
                        "clothingItemId" to ref.clothingItemId,
                        "posX" to ref.posX,
                        "posY" to ref.posY
                    )
                }
                val updateMap = mapOf<String, Any>("clothingItems" to itemsPayload)
                apiService.updateOutfit(outfitId, updateMap)
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ insertOutfitClothingCrossRefs() lỗi: ${e.message}")
            }
        }

    /**
     * 🗑️ Xoá tất cả liên kết ClothingItem của một Outfit.
     *
     * ✅ Giữ đúng tên: clearOutfitItems(outfitId: String)
     *
     * @param outfitId UUID của outfit cần clear items.
     */
    suspend fun clearOutfitItems(outfitId: String) = withContext(Dispatchers.IO) {
        try {
            val updateMap = mapOf<String, Any>("clothingItems" to emptyList<Map<String, Any>>())
            apiService.updateOutfit(outfitId, updateMap)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ clearOutfitItems() lỗi: ${e.message}")
        }
    }

    /**
     * ❌ Xoá một Outfit.
     *
     * ✅ Giữ đúng tên: deleteOutfit(outfit: OutfitEntity)
     *
     * ⚠️ Backend tự động CASCADE xoá CalendarEvent liên quan.
     *
     * @param outfit OutfitEntity cần xoá.
     */
    suspend fun deleteOutfit(outfit: OutfitEntity) = withContext(Dispatchers.IO) {
        try {
            apiService.deleteOutfit(outfit.id)
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ deleteOutfit() lỗi: ${e.message}")
        }
    }


}

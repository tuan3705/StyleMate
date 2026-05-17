package com.example.stylemate.repository

import com.example.stylemate.model.OutfitClothingCrossRef
import com.example.stylemate.model.OutfitDao
import com.example.stylemate.model.OutfitEntity
import com.example.stylemate.model.OutfitWithClothingItems
import com.example.stylemate.model.OutfitItemWithPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * 🏪 OutfitRepository — Repository Pattern cho module Outfit.
 *
 * Lớp trung gian duy nhất giữa [OutfitViewModel] và [OutfitDao].
 * - ViewModel KHÔNG bao giờ gọi trực tiếp DAO, mà luôn qua Repository.
 * - Tất cả các thao tác ghi (suspend) đều được chuyển qua [Dispatchers.IO].
 * - Flow từ DAO được giữ nguyên để giữ tính reactive (UI tự cập nhật).
 */
class OutfitRepository(private val outfitDao: OutfitDao) {

    // ─────────────────────────────────────────────────────────────
    // 📋 Đọc (Read) — Reactive với Flow
    // ─────────────────────────────────────────────────────────────

    /**
     * 📋 Lấy danh sách tất cả Outfit (kèm ClothingItem bên trong) dưới dạng Flow.
     *
     * ⚡ Reactive: Khi có bất kỳ thay đổi nào trong outfits, cross_ref, hoặc
     * clothing_items, Flow tự động emit dữ liệu mới — UI cập nhật tức thì.
     */
    fun getAllOutfitsWithItems(): Flow<List<OutfitWithClothingItems>> {
        return outfitDao.getAllOutfitsWithItems()
    }

    /**
     * 📋 Lấy chi tiết một Outfit theo ID.
     *
     * @param outfitId UUID của outfit.
     */
    suspend fun getOutfitWithItemsById(outfitId: String): OutfitWithClothingItems? =
        withContext(Dispatchers.IO) {
            outfitDao.getOutfitWithItemsById(outfitId)
        }

    /**
     * 📋 Lấy danh sách Outfit có chứa một ClothingItem cụ thể.
     *
     * @param clothingItemId UUID của ClothingItem.
     */
    fun getOutfitsContainingItem(clothingItemId: String): Flow<List<OutfitWithClothingItems>> {
        return outfitDao.getOutfitsContainingItem(clothingItemId)
    }

    suspend fun getOutfitItemsWithPosition(outfitId: String): List<OutfitItemWithPosition> =
        withContext(Dispatchers.IO) {
            outfitDao.getOutfitItemsWithPosition(outfitId)
        }

    // ─────────────────────────────────────────────────────────────
    // ➕ Ghi (Write) — Suspend + IO
    // ─────────────────────────────────────────────────────────────

    /**
     * ➕ Tạo một Outfit mới.
     *
     * @param outfit Entity cần lưu.
     */
    suspend fun insertOutfit(outfit: OutfitEntity) = withContext(Dispatchers.IO) {
        outfitDao.insertOutfit(outfit)
    }

    /**
     * 🔗 Liên kết danh sách ClothingItem vào một Outfit thông qua bảng CrossRef.
     * Thường được gọi sau [insertOutfit] để ghi các cặp quan hệ.
     *
     * @param crossRefs Danh sách các cặp (outfitId, clothingItemId).
     */
    suspend fun insertOutfitClothingCrossRefs(crossRefs: List<OutfitClothingCrossRef>) =
        withContext(Dispatchers.IO) {
            outfitDao.insertOutfitClothingCrossRefs(crossRefs)
        }

    /**
     * 🗑️ Xoá tất cả liên kết ClothingItem của một Outfit.
     * Dùng khi người dùng chỉnh sửa outfit (xoá cũ, thêm mới).
     *
     * @param outfitId UUID của outfit cần clear items.
     */
    suspend fun clearOutfitItems(outfitId: String) = withContext(Dispatchers.IO) {
        outfitDao.clearOutfitItems(outfitId)
    }

    /**
     * ❌ Xoá một Outfit khỏi database.
     * Nhờ CASCADE, các CrossRef liên quan cũng tự động bị xoá.
     *
     * @param outfit Entity cần xoá.
     */
    suspend fun deleteOutfit(outfit: OutfitEntity) = withContext(Dispatchers.IO) {
        outfitDao.deleteOutfit(outfit)
    }
}

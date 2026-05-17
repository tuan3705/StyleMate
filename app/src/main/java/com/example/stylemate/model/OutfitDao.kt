package com.example.stylemate.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Delete
import com.example.stylemate.model.OutfitItemWithPosition
import kotlinx.coroutines.flow.Flow

/**
 * 🗄️ OutfitDao — Data Access Object cho module Outfit (Phối đồ).
 *
 * Sử dụng [Transaction] để đảm bảo tính toàn vẹn dữ liệu khi thao tác
 * trên nhiều bảng cùng lúc (vd: tạo Outfit + insert CrossRef cùng lúc).
 * Sử dụng [Flow] để UI observe danh sách outfits một cách reactive.
 */
@Dao
interface OutfitDao {

    // ─────────────────────────────────────────────────────────────
    // 📋 Truy vấn danh sách Outfit kèm ClothingItem
    // ─────────────────────────────────────────────────────────────

    /**
     * Lấy toàn bộ danh sách Outfit, mỗi Outfit kèm danh sách ClothingItem bên trong.
     *
     * 🏗️ Room sẽ chạy truy vấn N+1 nhưng được tối ưu:
     *   - 1 query SELECT * FROM outfits
     *   - 1 query SELECT * FROM outfit_clothing_cross_ref
     *   - 1 query SELECT * FROM clothing_items WHERE id IN (...)
     * Sau đó Room tự động gom nhóm (map) kết quả vào [OutfitWithClothingItems].
     *
     * ⚡ Reactive: Khi có bất kỳ thay đổi nào trong outfits, cross_ref, hoặc clothing_items,
     * Flow tự động emit lại danh sách mới — UI cập nhật tức thì.
     */
    @Transaction
    @Query("SELECT * FROM outfits ORDER BY createdAt DESC")
    fun getAllOutfitsWithItems(): Flow<List<OutfitWithClothingItems>>

    /**
     * Lấy chi tiết một Outfit theo ID (kèm danh sách ClothingItem).
     * Dùng khi cần hiển thị/edit một outfit cụ thể.
     *
     * @param outfitId UUID của outfit cần lấy.
     * @return OutfitWithClothingItems hoặc null nếu không tìm thấy.
     */
    @Transaction
    @Query("SELECT * FROM outfits WHERE id = :outfitId")
    suspend fun getOutfitWithItemsById(outfitId: String): OutfitWithClothingItems?

    // ─────────────────────────────────────────────────────────────
    // ➕ Tạo Outfit mới + thêm Items vào Outfit
    // ─────────────────────────────────────────────────────────────

    /**
     * Tạo một Outfit mới.
     * Dùng [OnConflictStrategy.REPLACE] để hỗ trợ upsert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutfit(outfit: OutfitEntity)

    /**
     * Thêm một hoặc nhiều ClothingItem vào Outfit thông qua bảng CrossRef.
     *
     * 📝 Đây là bước quan trọng: sau khi tạo Outfit (insertOutfit), ta gọi tiếp
     * hàm này để ghi các cặp (outfitId, clothingItemId) vào bảng trung gian.
     *
     * ⚠️ Các hàm này nên được wrap trong cùng một @Transaction để đảm bảo
     * atomic — nếu insert CrossRef thất bại, Outfit cũng không được tạo.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE) // Upsert để cập nhật vị trí
    suspend fun insertOutfitClothingCrossRefs(crossRefs: List<OutfitClothingCrossRef>)

    /**
     * Xoá tất cả liên kết của một Outfit với các ClothingItem.
     * Dùng khi người dùng muốn thay đổi danh sách items trong outfit.
     */
    @Query("DELETE FROM outfit_clothing_cross_ref WHERE outfitId = :outfitId")
    suspend fun clearOutfitItems(outfitId: String)

    /**
     * Lấy danh sách items trong outfit kèm vị trí đã lưu.
     */
    @Query("""
        SELECT c.*, x.posX AS posX, x.posY AS posY
        FROM clothing_items c
        INNER JOIN outfit_clothing_cross_ref x
        ON c.id = x.clothingItemId
        WHERE x.outfitId = :outfitId
        ORDER BY c.createdAt DESC
    """)
    suspend fun getOutfitItemsWithPosition(outfitId: String): List<OutfitItemWithPosition>

    // ─────────────────────────────────────────────────────────────
    // ❌ Xoá Outfit
    // ─────────────────────────────────────────────────────────────

    /**
     * Xoá một Outfit khỏi database.
     *
     * 🧹 Nhờ FOREIGN KEY với onDelete = CASCADE, tất cả các dòng CrossRef
     * liên quan đến outfit này cũng tự động bị xoá — không cần xoá tay.
     */
    @Delete
    suspend fun deleteOutfit(outfit: OutfitEntity)

    // ─────────────────────────────────────────────────────────────
    // 🔧 Tiện ích mở rộng
    // ─────────────────────────────────────────────────────────────

    /**
     * Lấy danh sách các Outfit có chứa một ClothingItem cụ thể.
     * Dùng cho tính năng "Item này đang ở trong outfit nào?"
     *
     * @param clothingItemId UUID của ClothingItem cần tra.
     */
    @Transaction
    @Query("""
        SELECT * FROM outfits 
        WHERE id IN (
            SELECT outfitId FROM outfit_clothing_cross_ref 
            WHERE clothingItemId = :clothingItemId
        )
        ORDER BY createdAt DESC
    """)
    fun getOutfitsContainingItem(clothingItemId: String): Flow<List<OutfitWithClothingItems>>
}

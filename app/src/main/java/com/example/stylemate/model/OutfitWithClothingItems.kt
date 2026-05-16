package com.example.stylemate.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/**
 * 📦 OutfitWithClothingItems — Data class dùng để truy vấn một Outfit
 * kèm toàn bộ danh sách [ClothingItemEntity] bên trong nó.
 *
 * 🧠 Giải thích cơ chế @Relation + @Junction của Room:
 * ----------------------------------------------------------------------------
 * Đây là cách Room thực hiện truy vấn N-N một cách "thần kỳ" (magic) mà
 * không cần viết JOIN thủ công:
 *
 *   @Relation(
 *       parentColumn = "id",              // Khoá chính của bảng "cha" (OutfitEntity)
 *       entityColumn = "id",              // Khoá chính của bảng "con" (ClothingItemEntity)
 *       associateBy = Junction(           // Bảng trung gian cầu nối (KHÔNG có @ prefix trong Kotlin)
 *           value = OutfitClothingCrossRef::class,
 *           parentColumn = "outfitId",    // Cột trong bảng trung gian trỏ tới bảng cha
 *           entityColumn = "clothingItemId" // Cột trong bảng trung gian trỏ tới bảng con
 *       )
 *   )
 *
 * 📐 Quy trình Room chạy ngầm:
 *   1. SELECT * FROM outfits WHERE id = :outfitId
 *   2. SELECT clothing_items.* FROM clothing_items
 *      INNER JOIN outfit_clothing_cross_ref
 *      ON clothing_items.id = outfit_clothing_cross_ref.clothingItemId
 *      WHERE outfit_clothing_cross_ref.outfitId = :outfitId
 *   3. Room tự động gán List<ClothingItemEntity> vào trường [clothingItems]
 *      của data class này.
 *
 * 🎯 Kết quả: Ta có một đối tượng Outfit chứa sẵn danh sách ClothingItem —
 *    rất tiện để UI render (vd: hiển thị các item trong một outfit dạng lưới).
 * ----------------------------------------------------------------------------
 *
 * @property outfit Thông tin của Outfit (id, name, createdAt).
 * @property clothingItems Danh sách các ClothingItem thuộc outfit này.
 */
data class OutfitWithClothingItems(
    @Embedded
    val outfit: OutfitEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = OutfitClothingCrossRef::class,
            parentColumn = "outfitId",
            entityColumn = "clothingItemId"
        )
    )
    val clothingItems: List<ClothingItemEntity>
)

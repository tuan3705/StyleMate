package com.example.stylemate.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * 🔗 OutfitClothingCrossRef — Bảng trung gian (Junction Table) cho quan hệ N-N
 * giữa [OutfitEntity] và [ClothingItemEntity].
 *
 * 🧠 Giải thích cơ chế @Junction của Room:
 * ----------------------------------------------------------------------------
 * Trong cơ sở dữ liệu quan hệ, để biểu diễn mối quan hệ N-N (nhiều-nhiều),
 * ta cần một bảng trung gian (junction table) lưu cặp khoá ngoại (foreign keys)
 * từ cả hai bảng chính.
 *
 * Bảng này chính là "cầu nối" và không mang dữ liệu nghiệp vụ:
 *   outfits(id) ──< outfit_clothing >── clothing_items(id)
 *
 * Khi Room truy vấn với @Relation kết hợp @Junction:
 *   1. Room SELECT từ bảng chính (outfits).
 *   2. Room JOIN với bảng trung gian (outfit_clothing) qua outfitId.
 *   3. Room JOIN tiếp với bảng phụ (clothing_items) qua clothingItemId.
 *   4. Room tự động gom nhóm kết quả — mỗi Outfit sẽ kèm một List<ClothingItemEntity>.
 *
 * 🏗️ Cấu trúc bảng:
 *   - outfitId: Khoá ngoại tham chiếu tới [OutfitEntity.id].
 *   - clothingItemId: Khoá ngoại tham chiếu tới [ClothingItemEntity.id].
 *   - Kết hợp (outfitId, clothingItemId) là composite primary key.
 *
 * ⚠️ @Entity ở đây không có @PrimaryKey riêng lẻ mà dùng primaryKeys =
 *    arrayOf("outfitId", "clothingItemId") để đảm bảo không trùng lặp cặp.
 * ----------------------------------------------------------------------------
 */
@Entity(
    tableName = "outfit_clothing_cross_ref",
    primaryKeys = ["outfitId", "clothingItemId"],
    foreignKeys = [
        ForeignKey(
            entity = OutfitEntity::class,
            parentColumns = ["id"],
            childColumns = ["outfitId"],
            onDelete = ForeignKey.CASCADE
            // Khi xoá Outfit, tự động xoá hết các dòng liên quan trong bảng này
        ),
        ForeignKey(
            entity = ClothingItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["clothingItemId"],
            onDelete = ForeignKey.CASCADE
            // Khi xoá ClothingItem, tự động xoá khỏi các Outfit chứa nó
        )
    ],
    indices = [
        Index(value = ["clothingItemId"]) // Tăng tốc truy vấn ngược: item này thuộc outfit nào?
    ]
)
data class OutfitClothingCrossRef(
    val outfitId: String,
    val clothingItemId: String
)

package com.example.stylemate.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 👔 OutfitEntity — Entity Room cho module Phối đồ (Outfit).
 *
 * Một Outfit (bộ đồ) gồm nhiều ClothingItem (quần, áo, giày, ...).
 * Quan hệ giữa Outfit và ClothingItem là N-N (Many-to-Many):
 *   - Một Outfit chứa nhiều ClothingItem.
 *   - Một ClothingItem có thể thuộc nhiều Outfit khác nhau.
 *
 * @property id Mã định danh duy nhất (UUID string).
 * @property name Tên bộ đồ do người dùng đặt (vd: "Đi chơi cuối tuần").
 * @property createdAt Timestamp (epoch millis) lưu thời điểm tạo outfit — dùng cho sắp xếp.
 */
@Entity(tableName = "outfits")
data class OutfitEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

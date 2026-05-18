package com.example.stylemate.model

/**
 * 👔 OutfitEntity — Data class đại diện cho một bộ đồ (Outfit).
 *
 * Trước đây là Entity Room, nay là POJO thuần.
 *
 * Một Outfit gồm nhiều ClothingItem (quan hệ N-N).
 *
 * @property id Mã định danh duy nhất (UUID string).
 * @property name Tên bộ đồ do người dùng đặt.
 * @property createdAt Timestamp (epoch millis) thời điểm tạo.
 */
data class OutfitEntity(
    val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

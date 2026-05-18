package com.example.stylemate.model

/**
 * 🔗 OutfitClothingCrossRef — Liên kết giữa Outfit và ClothingItem.
 *
 * Trước đây là bảng trung gian (Junction Table) của Room,
 * nay là POJO thuần dùng để map dữ liệu qua API.
 *
 * @property outfitId UUID của Outfit.
 * @property clothingItemId UUID của ClothingItem.
 * @property posX Vị trí X của item trong canvas outfit (0.0 → 1.0).
 * @property posY Vị trí Y của item trong canvas outfit (0.0 → 1.0).
 */
data class OutfitClothingCrossRef(
    val outfitId: String,
    val clothingItemId: String,
    val posX: Float = 0.5f,
    val posY: Float = 0.5f
)

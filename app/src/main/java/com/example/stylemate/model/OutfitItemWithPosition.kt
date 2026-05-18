package com.example.stylemate.model

/**
 * 📦 OutfitItemWithPosition — ClothingItem trong Outfit kèm vị trí.
 *
 * Trước đây dùng @Embedded + @ColumnInfo của Room,
 * nay là POJO thuần.
 *
 * @property item ClothingItem entity.
 * @property posX Vị trí X (0.0 → 1.0) trên canvas.
 * @property posY Vị trí Y (0.0 → 1.0) trên canvas.
 */
data class OutfitItemWithPosition(
    val item: ClothingItemEntity,
    val posX: Float,
    val posY: Float
)

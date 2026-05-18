package com.example.stylemate.model

/**
 * 📦 OutfitWithClothingItems — Outfit kèm danh sách ClothingItem bên trong.
 *
 * Trước đây dùng @Embedded + @Relation của Room,
 * nay là POJO thuần dùng chung cho ViewModel.
 *
 * @property outfit Thông tin của Outfit (id, name, createdAt).
 * @property clothingItems Danh sách ClothingItem thuộc outfit này.
 */
data class OutfitWithClothingItems(
    val outfit: OutfitEntity,
    val clothingItems: List<ClothingItemEntity>
)

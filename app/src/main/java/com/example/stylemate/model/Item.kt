package com.example.stylemate.model

/**
 * 📦 Item — Data class đại diện cho một món quần áo (legacy, giữ lại cho tương thích).
 *
 * Trước đây là Room Entity, nay là POJO thuần.
 * Dùng bởi các màn hình cũ hoặc tính năng chưa migrate.
 */
data class Item(
    val id: Int = 0,
    val name: String,
    val category: String,
    val imageUri: String?,
    val color: String,
    val season: String,
    val occasion: String,
    val brand: String,
    val purchaseDate: String,
    val price: Double
)

object Categories {
    const val ALL = "All"
    const val TOPS = "Tops"
    const val BOTTOMS = "Bottoms"
    const val DRESSES = "Dresses"
    const val FOOTWEAR = "Footwear"
    const val BAGS = "Bags"
    const val ACCESSORIES = "Accessories"
    const val JEWELRY = "Jewelry"

    val list = listOf(TOPS, BOTTOMS, DRESSES, FOOTWEAR, BAGS, ACCESSORIES, JEWELRY)
}

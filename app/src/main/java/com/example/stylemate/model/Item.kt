package com.example.stylemate.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class Item(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val category: String,
    val color: String,
    val season: String,
    val occasion: String,
    val brand: String,
    val purchaseDate: String,
    val price: Double,
    val imageUrl: String = "" // Added for future use
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

    val list = listOf(ALL, TOPS, BOTTOMS, DRESSES, FOOTWEAR, BAGS, ACCESSORIES, JEWELRY)
}

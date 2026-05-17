package com.example.stylemate.model

import androidx.room.ColumnInfo
import androidx.room.Embedded

data class OutfitItemWithPosition(
    @Embedded
    val item: ClothingItemEntity,
    @ColumnInfo(name = "posX")
    val posX: Float,
    @ColumnInfo(name = "posY")
    val posY: Float
)

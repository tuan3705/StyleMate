package com.example.stylemate.model

@kotlinx.serialization.Serializable
data class ClothingItem(
    val id: String,
    val name: String,
    val category: String,
    val color: String,
    val imageUrl: String,
    val tags: List<String> = emptyList()
)

@kotlinx.serialization.Serializable
data class Outfit(
    val id: String,
    val items: List<ClothingItem> = emptyList(),
    val occasion: String,
    val rating: Int = 0
)

@kotlinx.serialization.Serializable
data class User(
    val id: String,
    val name: String,
    val stylePreference: String
)

@kotlinx.serialization.Serializable
data class ChatMessage(
    val sender: String,
    val content: String,
    val timestamp: Long
)

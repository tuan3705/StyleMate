package com.example.stylemate.data.models

/**
 * Data contracts derived from AI_Stylist_Features.md
 */

data class SuggestedOutfit(
    val id: String,
    val items: List<String>, // List of item IDs
    val imageUrls: List<String>,
    val reason: String,
    val confidence: Double,
    val actions: List<String> = listOf("tryon", "save", "buy")
)

data class ChatResponse(
    val message: String,
    val suggestedOutfits: List<SuggestedOutfit>,
    val followups: List<String>
)

data class ProcessingJob(
    val jobId: String,
    val userId: String,
    val type: String, // 'virtual-tryon', 'item-extraction', 'color-analysis'
    val status: JobStatus,
    val progress: Int, // 0-100
    val resultUrls: List<String> = emptyList(),
    val error: String? = null,
    val expiresAt: Long? = null
)

enum class JobStatus {
    QUEUED, IN_PROGRESS, COMPLETED, FAILED, CANCELLED
}

data class ColorPalette(
    val season: String,
    val palette: List<String>, // Hex codes
    val avoid: List<String>, // Hex codes
    val confidence: Double,
    val description: String
)

data class StyleAssessmentResult(
    val score: Double,
    val message: String,
    val recommendations: List<Recommendation>,
    val relatedItemIds: List<String>,
    val followups: List<String>
)

data class Recommendation(
    val text: String,
    val itemSuggestions: List<String> // Item IDs
)

data class ClosetItem(
    val id: String,
    val category: String,
    val imageUrl: String,
    val tags: List<String> = emptyList(),
    val confidence: Double = 1.0
)

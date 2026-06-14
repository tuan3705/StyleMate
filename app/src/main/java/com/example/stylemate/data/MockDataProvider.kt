package com.example.stylemate.data

import com.example.stylemate.data.models.ColorPalette
import com.example.stylemate.data.models.StyleAssessmentResult
import com.example.stylemate.data.models.Recommendation

/**
 * Central provider for mock/seed data used during development.
 * ViewModels should NOT contain any hardcoded display strings.
 * Instead, they should call [MockDataProvider] for test data or
 * [androidx.compose.ui.res.stringResource] in Compose for UI strings.
 */
object MockDataProvider {

    // ── Color Analysis (FindColorsViewModel) ──────────────────────

    object ColorAnalysis {
        val SPRING_WARM = ColorPalette(
            season = "Spring Warm",
            palette = listOf("#FAD9C1", "#FFDD88", "#99E0A7"),
            avoid = listOf("#4B5563", "#0A0A0A"),
            confidence = 0.85,
            description = "color_analysis_description_spring_warm"
        )

        val SUMMER_COOL = ColorPalette(
            season = "Summer Cool",
            palette = listOf("#E8D5F5", "#C4E0F9", "#B8D8BE"),
            avoid = listOf("#111111", "#FF6600"),
            confidence = 0.82,
            description = "color_analysis_description_summer_cool"
        )

        val AUTUMN_WARM = ColorPalette(
            season = "Autumn Warm",
            palette = listOf("#F4C28D", "#C9A96E", "#A8B87A"),
            avoid = listOf("#1A1A2E", "#FF00FF"),
            confidence = 0.88,
            description = "color_analysis_description_autumn_warm"
        )

        val WINTER_COOL = ColorPalette(
            season = "Winter Cool",
            palette = listOf("#D4E6F1", "#F9E79F", "#ABEBC6"),
            avoid = listOf("#2C3E50", "#7F8C8D"),
            confidence = 0.86,
            description = "color_analysis_description_winter_cool"
        )

        val DEFAULT_MOCK = SPRING_WARM
    }

    // ── Style Assessment (StyleAssessViewModel) ───────────────────

    object StyleAssessment {
        val FORMAL_SUCCESS = StyleAssessmentResult(
            score = 8.5,
            message = "style_assess_message_formal_success",
            recommendations = listOf(
                Recommendation("style_assess_recommend_tie", listOf("item_7")),
                Recommendation("style_assess_recommend_belt", listOf("item_8"))
            ),
            relatedItemIds = listOf("item_7", "item_8"),
            followups = listOf("style_assess_followup_accessories", "style_assess_followup_shoes")
        )

        val CASUAL_SUCCESS = StyleAssessmentResult(
            score = 7.2,
            message = "style_assess_message_casual_success",
            recommendations = listOf(
                Recommendation("style_assess_recommend_sneakers", listOf("item_10"))
            ),
            relatedItemIds = listOf("item_10"),
            followups = listOf("style_assess_followup_layers", "style_assess_followup_colors")
        )

        val DEFAULT_MOCK = FORMAL_SUCCESS
    }
}
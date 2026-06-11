package com.example.stylemate.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Material 3 Shapes
 * Tuân thủ đúng spec của Material Design 3:
 * https://m3.material.io/styles/shape/shape-scale-tokens
 *
 * - extraSmall:   4dp  (for small components like chips, switches)
 * - small:        8dp  (for cards, buttons, text fields)
 * - medium:       12dp (for elevated cards, dialogs)
 * - large:        16dp (for modal bottom sheets, side sheets)
 * - extraLarge:   28dp (for FAB, large components)
 */
val StyleMateShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
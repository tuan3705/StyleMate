package com.example.stylemate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.stylemate.data.models.SuggestedOutfit
import androidx.compose.ui.tooling.preview.Preview

/**
 * OutfitSuggestionCard displays a structured outfit suggestion with items and action buttons.
 * 
 * @param outfit The SuggestedOutfit data object.
 * @param onAction Callback for actions like 'tryon', 'save', 'buy'.
 */
@Composable
fun OutfitSuggestionCard(
    outfit: SuggestedOutfit,
    onAction: (String, SuggestedOutfit) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Reason / LLM Summary
            Text(
                text = outfit.reason,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Items Grid
            Box(modifier = Modifier.height(180.dp)) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(outfit.imageUrls) { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = "Outfit item",
                            modifier = Modifier
                                .height(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // Confidence Badge
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Confidence: ${(outfit.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                outfit.actions.forEach { action ->
                    Button(
                        onClick = { onAction(action, outfit) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text(text = action.uppercase(), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewOutfitSuggestionCard() {
    val sampleOutfit = SuggestedOutfit(
        id = "o1",
        items = listOf("i1", "i2"),
        imageUrls = listOf("https://via.placeholder.com/150", "https://via.placeholder.com/150"),
        reason = "Perfect for a rainy day at the office.",
        confidence = 0.92
    )
    OutfitSuggestionCard(outfit = sampleOutfit, onAction = { _, _ -> })
}

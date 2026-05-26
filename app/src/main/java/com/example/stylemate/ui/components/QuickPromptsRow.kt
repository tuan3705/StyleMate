package com.example.stylemate.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview

/**
 * QuickPromptsRow displays a horizontal list of chip-based follow-up prompts.
 * 
 * @param prompts List of strings to display as clickable chips.
 * @param onPromptClick Callback when a prompt is selected.
 */
@Composable
fun QuickPromptsRow(
    prompts: List<String>,
    onPromptClick: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(prompts) { prompt ->
            SuggestionChip(
                onClick = { onPromptClick(prompt) },
                label = {
                    Text(text = prompt, style = MaterialTheme.typography.labelMedium)
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewQuickPromptsRow() {
    QuickPromptsRow(
        prompts = listOf("Make it formal", "Try different colors", "Show accessories"),
        onPromptClick = {}
    )
}

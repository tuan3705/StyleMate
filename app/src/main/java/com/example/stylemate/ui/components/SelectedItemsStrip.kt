package com.example.stylemate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.stylemate.data.models.ClosetItem
import androidx.compose.ui.tooling.preview.Preview

/**
 * SelectedItemsStrip shows a horizontal list of selected items, usually at the bottom of a selection screen.
 * 
 * @param items List of selected ClosetItem objects.
 * @param onRemove Callback to remove an item from selection.
 */
@Composable
fun SelectedItemsStrip(
    items: List<ClosetItem>,
    onRemove: (ClosetItem) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .height(80.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items) { item ->
                    Box(modifier = Modifier.size(60.dp)) {
                        AsyncImage(
                            model = item.imageUrl,
                            contentDescription = "Selected item",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { onRemove(item) },
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = 8.dp, y = (-8).dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            
            Text(
                text = "${items.size} items",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSelectedItemsStrip() {
    val sampleItems = listOf(
        ClosetItem("1", "shirt", "https://via.placeholder.com/60"),
        ClosetItem("2", "pants", "https://via.placeholder.com/60")
    )
    SelectedItemsStrip(items = sampleItems, onRemove = {})
}

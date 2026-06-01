package com.example.stylemate.ui.screens.style_assess

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.stylemate.data.models.ClosetItem
import com.example.stylemate.ui.components.SelectedItemsStrip
import com.example.stylemate.ui.components.StylistButton
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyleAssessScreen(
    viewModel: StyleAssessViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val selectedItems by viewModel.selectedItems.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    // Mock closet data
    val mockCloset = remember {
        listOf(
            ClosetItem("1", "shirt", "https://via.placeholder.com/150"),
            ClosetItem("2", "pants", "https://via.placeholder.com/150"),
            ClosetItem("3", "jacket", "https://via.placeholder.com/150"),
            ClosetItem("4", "shoes", "https://via.placeholder.com/150")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Style Assessment") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (selectedItems.isNotEmpty() && uiState is StyleAssessUiState.Idle) {
                Column {
                    SelectedItemsStrip(
                        items = selectedItems,
                        onRemove = { viewModel.removeItem(it) }
                    )
                    StylistButton(
                        text = "Assess Style",
                        onClick = { viewModel.runAssessment() },
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (val state = uiState) {
                is StyleAssessUiState.Idle -> {
                    ClosetGrid(
                        items = mockCloset,
                        selectedItems = selectedItems,
                        onItemToggle = { viewModel.toggleItemSelection(it) }
                    )
                }
                is StyleAssessUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is StyleAssessUiState.Success -> {
                    AssessmentResultView(
                        result = state.result,
                        onReset = { viewModel.reset() }
                    )
                }
                is StyleAssessUiState.Error -> {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun ClosetGrid(
    items: List<ClosetItem>,
    selectedItems: List<ClosetItem>,
    onItemToggle: (ClosetItem) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { item ->
            val isSelected = selectedItems.any { it.id == item.id }
            Card(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clickable { onItemToggle(item) },
                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.category,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
fun AssessmentResultView(
    result: com.example.stylemate.data.models.StyleAssessmentResult,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Your Style Score",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = result.score.toString(),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = result.message,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        
        StylistButton(text = "Try Again", onClick = onReset)
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewStyleAssessScreen() {
    StyleAssessScreen()
}

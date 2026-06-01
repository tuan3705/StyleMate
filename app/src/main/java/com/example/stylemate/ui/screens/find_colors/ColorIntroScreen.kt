package com.example.stylemate.ui.screens.find_colors

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stylemate.ui.components.StylistButton
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorIntroScreen(
    viewModel: FindColorsViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Find My Colors") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (val state = uiState) {
                is ColorUiState.Intro -> {
                    Text("Discover your seasonal color palette with AI analysis.")
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Steps:", style = MaterialTheme.typography.titleMedium)
                    BulletPoint("1. Remove accessories")
                    BulletPoint("2. No heavy makeup")
                    BulletPoint("3. Find good natural light")
                    
                    Spacer(modifier = Modifier.weight(1f))
                    StylistButton(text = "Start Analysis", onClick = { viewModel.startCapture() })
                }
                is ColorUiState.Capture -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Camera Preview", color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    StylistButton(text = "Capture & Analyze", onClick = { viewModel.analyze() })
                }
                is ColorUiState.Analyzing -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    Text("AI is analyzing your skin tone...")
                }
                is ColorUiState.Result -> {
                    ColorResultView(palette = state.palette, onReset = { viewModel.reset() })
                }
            }
        }
    }
}

@Composable
fun BulletPoint(text: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("• ", style = MaterialTheme.typography.bodyLarge)
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun ColorResultView(palette: com.example.stylemate.data.models.ColorPalette, onReset: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Your Season:", style = MaterialTheme.typography.titleLarge)
        Text(
            text = palette.season,
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Recommended Palette:")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(palette.palette) { hex ->
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(hex)))
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text(palette.description)
        
        Spacer(modifier = Modifier.height(24.dp))
        StylistButton(text = "Save Profile", onClick = {})
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
            Text("Retake")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewColorIntroScreen() {
    ColorIntroScreen()
}

package com.example.stylemate.ui.screens.ai_stylist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.stylemate.network.RetrofitClient
import com.example.stylemate.repository.ClothingRepository
import com.example.stylemate.viewmodel.ClothingViewModel
import com.example.stylemate.viewmodel.ClothingViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIClosetSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val apiService = RetrofitClient.stylemateApiService
    val repository = remember { ClothingRepository(apiService, context) }
    val clothingVM: ClothingViewModel = viewModel(factory = ClothingViewModelFactory(repository))
    
    val items by clothingVM.filteredItems.collectAsStateWithLifecycle()
    val selectedItems = remember { mutableStateListOf<String>() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Tủ đồ để gợi ý", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 4.dp, shadowElevation = 8.dp) {
                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                ) {
                    Text("Hoàn tất", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Virtual "Selection" Card matching Screenshot 4
            Card(
                modifier = Modifier.size(160.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7FB))
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(items.take(4)) { item ->
                            AsyncImage(
                                model = item.imageNoBg.ifBlank { item.imageOriginal },
                                contentDescription = null,
                                modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(8.dp)).background(Color.White),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                    // Overlay check and "5+" like Screenshot
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color.Black, CircleShape)
                            .align(Alignment.TopStart),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(text = "Tất cả món đồ", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
            Text(text = "${items.size}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

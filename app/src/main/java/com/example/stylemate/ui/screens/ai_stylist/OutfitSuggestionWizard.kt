package com.example.stylemate.ui.screens.ai_stylist

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import com.example.stylemate.model.Categories
import com.example.stylemate.model.ClothingItemEntity
import com.example.stylemate.network.RetrofitClient
import com.example.stylemate.repository.ClothingRepository
import com.example.stylemate.viewmodel.ClothingViewModel
import com.example.stylemate.viewmodel.ClothingViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutfitSuggestionWizard(
    onBack: () -> Unit,
    onFinish: (List<String>, String, String, String) -> Unit
) {
    var currentStep by remember { mutableIntStateOf(1) }
    val selectedItemIds = remember { mutableStateListOf<String>() }
    var selectedOccasion by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf("") }
    var selectedTheme by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$currentStep/3", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 1) currentStep-- else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (currentStep) {
                1 -> ItemSelectionStep(
                    selectedIds = selectedItemIds,
                    onToggleId = { id ->
                        if (selectedItemIds.contains(id)) selectedItemIds.remove(id)
                        else selectedItemIds.add(id)
                    },
                    onNext = { currentStep = 2 }
                )
                2 -> OccasionSelectionStep(
                    selectedOccasion = selectedOccasion,
                    onSelect = { selectedOccasion = it },
                    onNext = { currentStep = 3 }
                )
                3 -> StyleRefinementStep(
                    selectedOccasion = selectedOccasion,
                    selectedStyle = selectedStyle,
                    selectedTheme = selectedTheme,
                    onStyleSelect = { selectedStyle = it },
                    onThemeSelect = { selectedTheme = it },
                    onNext = { 
                        onFinish(selectedItemIds.toList(), selectedOccasion, selectedStyle, selectedTheme)
                    }
                )
            }
        }
    }
}

@Composable
fun ItemSelectionStep(
    selectedIds: List<String>,
    onToggleId: (String) -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    val apiService = RetrofitClient.stylemateApiService
    val repository = remember { ClothingRepository(apiService, context) }
    val viewModel: ClothingViewModel = viewModel(factory = ClothingViewModelFactory(repository))
    
    val items by viewModel.filteredItems.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    
    val allCategories = listOf(Categories.ALL) + Categories.list

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Chọn các món đồ bạn muốn được tạo kiểu",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        ScrollableTabRow(
            selectedTabIndex = allCategories.indexOf(selectedCategory).let { if (it == -1) 0 else it },
            edgePadding = 16.dp,
            divider = {},
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            allCategories.forEach { category ->
                Tab(
                    selected = selectedCategory == category,
                    onClick = { viewModel.selectCategory(category) },
                    text = { Text(category) }
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items) { item ->
                    val isSelected = selectedIds.contains(item.id)
                    ClosetItemCard(
                        item = item,
                        isSelected = isSelected,
                        onToggle = { onToggleId(item.id) }
                    )
                }
            }
        }

        // Bottom Actions
        Surface(tonalElevation = 2.dp, shadowElevation = 4.dp) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                Button(
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                ) {
                    Text("Tiếp theo", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onNext,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Bỏ qua", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun ClosetItemCard(
    item: ClothingItemEntity,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val baseUrl = RetrofitClient.STYLEMATE_BASE_URL.trimEnd('/')
    val imageUrl = if (item.imageOriginal.startsWith("http")) item.imageOriginal 
                   else "$baseUrl${item.imageOriginal}"

    Column(
        modifier = Modifier
            .clickable { onToggle() }
            .padding(4.dp)
    ) {
        Box(modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF5F7FB))) {
            AsyncImage(
                model = imageUrl,
                contentDescription = item.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .size(20.dp)
                    .border(1.dp, if (isSelected) Color.Black else Color.LightGray, CircleShape)
                    .background(if (isSelected) Color.Black else Color.Transparent, CircleShape)
                    .align(Alignment.TopStart),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.name.ifBlank { "Món đồ" },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
        Text(
            text = "17/5/2026", // Mock date as per UI
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OccasionSelectionStep(
    selectedOccasion: String,
    onSelect: (String) -> Unit,
    onNext: () -> Unit
) {
    val occasions = listOf(
        "Hàng ngày", "Trường học", "Làm việc", "Du lịch", "Bữa tiệc", "Hẹn hò",
        "Đám cưới", "Mua sắm", "Đi dạo", "Tập thể dục", "Nhà thờ", "Tụ họp",
        "Ăn ngoài", "Phỏng vấn", "Buổi hòa nhạc", "Tang lễ"
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Chọn Dịp",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(24.dp))

        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            occasions.forEach { occasion ->
                val isSelected = selectedOccasion == occasion
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(occasion) },
                    label = { Text(occasion) },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFE3F2FD),
                        selectedLabelColor = Color(0xFF1976D2)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = Color.LightGray,
                        selectedBorderColor = Color(0xFF1976D2),
                        enabled = true,
                        selected = isSelected
                    )
                )
            }
        }

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedOccasion.isNotEmpty()) Color.Black else Color.LightGray
            ),
            enabled = selectedOccasion.isNotEmpty()
        ) {
            Text("Nhận gợi ý", fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StyleRefinementStep(
    selectedOccasion: String,
    selectedStyle: String,
    selectedTheme: String,
    onStyleSelect: (String) -> Unit,
    onThemeSelect: (String) -> Unit,
    onNext: () -> Unit
) {
    val styles = listOf(
        "Không có", "Thường ngày", "Cổ điển", "Đường phố", "Hiện đại", "Tối giản",
        "Nữ tính", "Bohemian", "Công sở thoải mái", "Bán trang trọng", "Trang trọng", "Dự tiệc"
    )
    val themes = listOf(
        "Không có", "Ở nhà/Thư giãn", "Đồ mặc ở nhà", "Quán cà phê/Tụ tập", "Triển lãm", "Phim"
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Chọn Dịp",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Selected Occasion Chip
        Surface(
            color = Color(0xFFE3F2FD),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1976D2))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(selectedOccasion, color = Color(0xFF1976D2))
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF1976D2))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("Phong cách", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            styles.forEach { style ->
                FilterChip(
                    selected = selectedStyle == style,
                    onClick = { onStyleSelect(style) },
                    label = { Text(style) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("Chủ đề", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            themes.forEach { theme ->
                FilterChip(
                    selected = selectedTheme == theme,
                    onClick = { onThemeSelect(theme) },
                    label = { Text(theme) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
        ) {
            Text("Nhận gợi ý", fontWeight = FontWeight.Bold)
        }
    }
}

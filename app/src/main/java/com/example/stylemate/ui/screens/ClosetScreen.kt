package com.example.stylemate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stylemate.model.AppDatabase
import com.example.stylemate.model.Categories
import com.example.stylemate.model.ClothingItemEntity
import com.example.stylemate.repository.ClothingRepository
import com.example.stylemate.viewmodel.ClothingViewModel
import com.example.stylemate.viewmodel.ClothingViewModelFactory
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClosetScreen() {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val repository = ClothingRepository(database.clothingDao())

    // 🔧 Khởi tạo ClothingViewModel với Factory pattern
    val viewModel: ClothingViewModel = viewModel(
        factory = ClothingViewModelFactory(repository)
    )

    // 🔷 Collect StateFlow với lifecycle-aware collector
    // collectAsStateWithLifecycle() tự động dừng collect khi lifecycle không active
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val allCategories = listOf(Categories.ALL) + Categories.list

    // ── Hiển thị Snackbar khi có lỗi ─────────────────────────────
    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        floatingActionButton = {
            AddItemFab(onClick = { showBottomSheet = true })
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Closet",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { /* Filter action */ }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Filter Buttons
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allCategories) { category ->
                    // 📊 Collect số lượng items riêng cho từng category
                    val count by viewModel.getItemCountByCategory(category)
                        .collectAsState(initial = 0)
                    CategoryChip(
                        category = category,
                        count = count,
                        isSelected = selectedCategory == category,
                        onClick = { viewModel.selectCategory(category) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Content Area: Grid Items ──────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    // Hiển thị loading spinner nếu đang xử lý
                    isLoading -> {
                        CircularProgressIndicator()
                    }
                    // Hiển thị empty state nếu không có item
                    items.isEmpty() -> {
                        Text(
                            text = "No items in ${selectedCategory.takeIf { it != Categories.ALL } ?: "your closet"}\nTap + to add your first item!",
                            color = Color.Gray,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    // 📱 Hiển thị lưới items (2 cột, dạng thumbnail)
                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(items, key = { it.id }) { clothingItem ->
                                ClothingItemCard(
                                    item = clothingItem,
                                    onDelete = { viewModel.deleteClothingItem(clothingItem) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Bottom Sheet: Thêm item mới ─────────────────────────────
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showBottomSheet = false
            },
            sheetState = sheetState
        ) {
            NewClothingItemSheet(
                viewModel = viewModel,
                onItemAdded = {
                    // Đóng sheet — ViewModel xử lý bất đồng bộ phía sau
                    scope.launch {
                        sheetState.hide()
                        showBottomSheet = false
                    }
                }
            )
        }
    }
}

/**
 * 🃏 ClothingItemCard — Card hiển thị một clothing item dạng thumbnail trong grid.
 *
 * @param item ClothingItemEntity cần hiển thị.
 * @param onDelete Callback khi người dùng nhấn nút xoá.
 */
@Composable
fun ClothingItemCard(
    item: ClothingItemEntity,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // ── Ảnh thumbnail (placeholder màu theo category) ──────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(getCategoryColor(item.category).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Icon đại diện cho loại quần áo
                    Text(
                        text = getCategoryIcon(item.category),
                        fontSize = 32.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    // Tên file ảnh (rút gọn)
                    Text(
                        text = item.imageOriginal.substringAfterLast("/").take(15),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // ── Thông tin phía dưới card ──────────────────────────
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = item.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.color,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // ── Nút Xoá (góc trên bên phải) ──────────────────────
            IconButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete item",
                    tint = Color.White,
                    modifier = Modifier
                        .background(Color.Red.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .padding(4.dp)
                )
            }
        }
    }
}

// ── Helper: Màu nền theo category ────────────────────────────────
private fun getCategoryColor(category: String): Color = when (category) {
    "Tops" -> Color(0xFF42A5F5)      // Xanh dương
    "Bottoms" -> Color(0xFF66BB6A)    // Xanh lá
    "Dresses" -> Color(0xFFEC407A)    // Hồng
    "Footwear" -> Color(0xFF8D6E63)   // Nâu
    "Bags" -> Color(0xFFAB47BC)       // Tím
    "Accessories" -> Color(0xFFFFA726) // Cam
    "Jewelry" -> Color(0xFFD4E157)     // Vàng chanh
    else -> Color(0xFFBDBDBD)          // Xám
}

// ── Helper: Icon emoji theo category ─────────────────────────────
private fun getCategoryIcon(category: String): String = when (category) {
    "Tops" -> "👕"
    "Bottoms" -> "👖"
    "Dresses" -> "👗"
    "Footwear" -> "👟"
    "Bags" -> "👜"
    "Accessories" -> "⌚"
    "Jewelry" -> "💍"
    else -> "🧥"
}

// ── Helper: Danh sách categories cho bottom sheet ────────────────
private val sheetCategories = listOf("Tops", "Bottoms", "Dresses", "Footwear", "Bags", "Accessories", "Jewelry")


// ─────────────────────────────────────────────────────────────────
// 📝 NewClothingItemSheet — Bottom sheet để thêm item mới
// Sử dụng ClothingViewModel thay vì callback truyền thống
// ─────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewClothingItemSheet(
    viewModel: ClothingViewModel,
    onItemAdded: () -> Unit
) {
    // ── Local state ─────────────────────────────────────────────
    var category by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var expandedMenu by remember { mutableStateOf(false) }

    // Collect loading state từ ViewModel
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Text(
            "Quick Add Item",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // ── Category (Dropdown) ────────────────────────────────
        Text("Category", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        ExposedDropdownMenuBox(
            expanded = expandedMenu,
            onExpandedChange = { expandedMenu = !expandedMenu }
        ) {
            OutlinedTextField(
                value = category,
                onValueChange = {},
                readOnly = true,
                label = { Text("Select category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMenu) },
                modifier = Modifier.fillMaxWidth().menuAnchor(
                    type = MenuAnchorType.PrimaryNotEditable,
                    enabled = true
                )
            )
            ExposedDropdownMenu(
                expanded = expandedMenu,
                onDismissRequest = { expandedMenu = false }
            ) {
                sheetCategories.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat) },
                        onClick = {
                            category = cat
                            expandedMenu = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Color ──────────────────────────────────────────────
        Text("Color", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = color,
            onValueChange = { color = it },
            label = { Text("Color (e.g. Red)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        // ── Nút Add ────────────────────────────────────────────
        Button(
            onClick = {
                if (category.isBlank() || color.isBlank()) return@Button
                // ⚡ Tạo file mock và gọi ViewModel.addClothingItem()
                val mockFile = java.io.File("/tmp/quick_add_${System.currentTimeMillis()}.jpg")
                viewModel.addClothingItem(
                    imageFile = mockFile,
                    category = category,
                    color = color
                )
                // Đóng sheet sau khi gọi
                onItemAdded()
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            enabled = !isLoading && category.isNotBlank() && color.isNotBlank()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text("Adding...")
            } else {
                Text("Add Item")
            }
        }
    }
}

@Composable
fun CategoryChip(
    category: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Color(0xFFFFD54F) else Color(0xFFF5F5F5), // Yellowish or light gray
        contentColor = if (isSelected) Color.Black else Color.Gray
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = category,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 16.sp
            )
            // Show count for all categories in the filter row
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = count.toString(),
                fontSize = 12.sp,
                color = if (isSelected) Color.Black.copy(alpha = 0.7f) else Color.Gray
            )
        }
    }
}

@Composable
fun AddItemFab(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add Item"
        )
    }
}

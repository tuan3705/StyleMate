package com.example.stylemate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.layout.ContentScale
import android.net.Uri
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.stylemate.model.AppDatabase
import com.example.stylemate.model.Categories
import com.example.stylemate.model.ClothingItemEntity
import com.example.stylemate.model.OutfitWithClothingItems
import com.example.stylemate.repository.ClothingRepository
import com.example.stylemate.repository.OutfitRepository
import com.example.stylemate.ui.common.ImagePickerSection
import com.example.stylemate.ui.common.rememberImagePickerState
import com.example.stylemate.viewmodel.ClothingViewModel
import com.example.stylemate.viewmodel.ClothingViewModelFactory
import com.example.stylemate.viewmodel.OutfitViewModel
import com.example.stylemate.viewmodel.OutfitViewModelFactory
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ═════════════════════════════════════════════════════════════════
// 📱 ClosetScreen — Màn hình Tủ đồ (đã tích hợp Phối đồ)
// ═════════════════════════════════════════════════════════════════
//
// 🧩 Cấu trúc TabRow gồm 2 tab:
//   Tab 1: "Món đồ" — danh sách ClothingItem + filter (code gốc)
//   Tab 2: "Bộ đồ"  — danh sách Outfit đã lưu + nút tạo mới
//
// 🎯 UX Logic:
//   - FAB thay đổi hành vi theo tab đang chọn
//   - Tab "Món đồ":  FAB → mở ModalBottomSheet Quick Add item (NewClothingItemSheet)
//   - Tab "Bộ đồ":   FAB → mở ModalBottomSheet tạo outfit mới (CreateOutfitBottomSheetContent)
// ═════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClosetScreen() {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)

    // ── Khởi tạo Repositories & ViewModels ──────────────────────
    val clothingRepo = ClothingRepository(database.clothingDao())
    val clothingVM: ClothingViewModel = viewModel(
        factory = ClothingViewModelFactory(clothingRepo)
    )

    val outfitRepo = OutfitRepository(database.outfitDao())
    val outfitVM: OutfitViewModel = viewModel(
        factory = OutfitViewModelFactory(outfitRepo)
    )

    // ── Collect StateFlows (Items) ──────────────────────────────
    val selectedCategory by clothingVM.selectedCategory.collectAsStateWithLifecycle()
    val items by clothingVM.items.collectAsStateWithLifecycle()
    val isItemsLoading by clothingVM.isLoading.collectAsStateWithLifecycle()
    val errorMessage by clothingVM.errorMessage.collectAsStateWithLifecycle()

    // ── Collect StateFlows (Outfits) ────────────────────────────
    val outfits by outfitVM.outfits.collectAsStateWithLifecycle()
    val isOutfitLoading by outfitVM.isLoading.collectAsStateWithLifecycle()
    val outfitError by outfitVM.errorMessage.collectAsStateWithLifecycle()

    // ── Local states ────────────────────────────────────────────
    var selectedTab by remember { mutableIntStateOf(0) }  // 0 = Items, 1 = Outfits
    var showBottomSheet by remember { mutableStateOf(false) }    // Quick Add item
    var showCreateOutfitSheet by remember { mutableStateOf(false) } // Tạo outfit
    // Mỗi BottomSheet có sheetState riêng để vuốt mượt, không conflict
    val quickAddSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val createOutfitSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val allCategories = listOf(Categories.ALL) + Categories.list
    val tabs = listOf("Món đồ", "Bộ đồ")

    // ── Snackbar error ──────────────────────────────────────────
    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            clothingVM.clearError()
        }
    }
    LaunchedEffect(outfitError) {
        outfitError?.let { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            outfitVM.clearError()
        }
    }

    Scaffold(
        floatingActionButton = {
            // 🎯 FAB thay đổi hành vi theo tab
            FloatingActionButton(
                onClick = {
                    when (selectedTab) {
                        0 -> showBottomSheet = true          // Tab Items → Quick Add sheet
                        1 -> showCreateOutfitSheet = true    // Tab Outfits → Create Outfit sheet
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (selectedTab == 0) "Thêm món đồ" else "Tạo bộ đồ"
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ── Header ──────────────────────────────────────────
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

            // ── TabRow ──────────────────────────────────────────
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ═══════════════════════════════════════════════════════
            // TAB 0: MÓN ĐỒ (Giữ nguyên logic cũ)
            // ═══════════════════════════════════════════════════════
            if (selectedTab == 0) {
                ItemsTabContent(
                    clothingVM = clothingVM,
                    selectedCategory = selectedCategory,
                    items = items,
                    isLoading = isItemsLoading,
                    allCategories = allCategories
                )
            }
            // ═══════════════════════════════════════════════════════
            // TAB 1: BỘ ĐỒ
            // ═══════════════════════════════════════════════════════
            else {
                OutfitsTabContent(
                    outfits = outfits,
                    isLoading = isOutfitLoading,
                    onDeleteOutfit = { outfitVM.deleteOutfit(it) }
                )
            }
        }
    }

    // ── BOTTOM SHEET: QUICK ADD ITEM ──────────────────────────
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                scope.launch {
                    quickAddSheetState.hide()
                    showBottomSheet = false
                }
            },
            sheetState = quickAddSheetState
        ) {
            NewClothingItemSheet(
                viewModel = clothingVM,
                onItemAdded = {
                    scope.launch {
                        quickAddSheetState.hide()
                        showBottomSheet = false
                    }
                },
                onError = { message ->
                    scope.launch { snackbarHostState.showSnackbar(message) }
                }
            )
        }
    }

    // ── BOTTOM SHEET TẠO OUTFIT ───────────────────────────────
    if (showCreateOutfitSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                scope.launch {
                    createOutfitSheetState.hide()
                    showCreateOutfitSheet = false
                }
            },
            sheetState = createOutfitSheetState
        ) {
            CreateOutfitBottomSheetContent(
                outfitVM = outfitVM,
                clothingRepo = clothingRepo,
                onSaved = {
                    scope.launch {
                        createOutfitSheetState.hide()
                        showCreateOutfitSheet = false
                    }
                }
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// 🔷 TAB 0: ItemsTabContent (Giữ nguyên logic cũ)
// ═════════════════════════════════════════════════════════════════

@Composable
private fun ItemsTabContent(
    clothingVM: ClothingViewModel,
    selectedCategory: String,
    items: List<ClothingItemEntity>,
    isLoading: Boolean,
    allCategories: List<String>
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // ── Category Filter Buttons ──────────────────────────────
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(allCategories) { category ->
                val count by clothingVM.getItemCountByCategory(category)
                    .collectAsState(initial = 0)
                CategoryChip(
                    category = category,
                    count = count,
                    isSelected = selectedCategory == category,
                    onClick = { clothingVM.selectCategory(category) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Content: Items Grid ──────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> CircularProgressIndicator()
                items.isEmpty() -> {
                    Text(
                        text = "No items in ${selectedCategory.takeIf { it != Categories.ALL } ?: "your closet"}\nTap + to add your first item!",
                        color = Color.Gray,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }
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
                                onDelete = { clothingVM.deleteClothingItem(clothingItem) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// 🔷 TAB 1: OutfitsTabContent
// ═════════════════════════════════════════════════════════════════
//
// 🎯 UX Logic:
//   - Hiển thị danh sách Outfit đã lưu dưới dạng Card.
//   - Mỗi Card có: Tên outfit, ngày tạo, LazyRow items thumbnail, nút xoá.
//   - Nếu chưa có outfit nào → hiển thị empty state + hướng dẫn.
// ═════════════════════════════════════════════════════════════════

@Composable
private fun OutfitsTabContent(
    outfits: List<OutfitWithClothingItems>,
    isLoading: Boolean,
    onDeleteOutfit: (com.example.stylemate.model.OutfitEntity) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> CircularProgressIndicator()
            outfits.isEmpty() -> {
                // 📝 Empty state
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(text = "🧥", fontSize = 48.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Chưa có bộ đồ nào",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Nhấn + để tạo bộ đồ mới từ các món đồ trong tủ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(outfits, key = { it.outfit.id }) { outfitWithItems ->
                        SavedOutfitCard(
                            outfitWithItems = outfitWithItems,
                            onDelete = { onDeleteOutfit(outfitWithItems.outfit) }
                        )
                    }
                    // Spacer bottom cho FAB
                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// 🔷 SavedOutfitCard — Card hiển thị một bộ đồ đã lưu
// ═════════════════════════════════════════════════════════════════

@Composable
private fun SavedOutfitCard(
    outfitWithItems: OutfitWithClothingItems,
    onDelete: () -> Unit
) {
    val outfit = outfitWithItems.outfit
    val items = outfitWithItems.clothingItems
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val formattedDate = remember(outfit.createdAt) {
        dateFormat.format(Date(outfit.createdAt))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = getCategoryIcon(items.firstOrNull()?.category ?: "Other"),
                            fontSize = 20.sp
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = outfit.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "📅 $formattedDate · ${items.size} món",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Xoá bộ đồ",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            if (items.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Món đồ trong bộ:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items, key = { it.id }) { item ->
                        OutfitItemThumbnail(item = item)
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// 🔷 OutfitItemThumbnail — Chip nhỏ hiển thị item trong outfit
// ═════════════════════════════════════════════════════════════════

@Composable
private fun OutfitItemThumbnail(item: ClothingItemEntity) {
    val imageModel = rememberItemImageModel(item)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = getCategoryColor(item.category).copy(alpha = 0.15f),
        modifier = Modifier
            .width(100.dp)
            .height(90.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(getCategoryColor(item.category).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = getCategoryIcon(item.category), fontSize = 24.sp)
                if (imageModel != null) {
                    AsyncImage(
                        model = imageModel,
                        contentDescription = item.name.ifBlank { item.category },
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.name.ifBlank { item.category },
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// 🔷 CreateOutfitBottomSheetContent — Nội dung BottomSheet tạo Outfit
// ═════════════════════════════════════════════════════════════════
//
// 🧩 Gồm 3 phần:
//   1. Header + TextField tên bộ đồ
//   2. Grid chọn items (toggle) — có visual feedback
//   3. Nút Lưu (chỉ enable khi có item + tên)
//
// 🎯 UX Logic:
//   - Click vào item → thêm/xoá khỏi draft (toggle)
//   - Item đã chọn → viền xanh + icon check
//   - Lưu xong → đóng sheet + clear draft
// ═════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateOutfitBottomSheetContent(
    outfitVM: OutfitViewModel,
    clothingRepo: ClothingRepository,
    onSaved: () -> Unit
) {
    val allItems by clothingRepo.getAllItems()
        .collectAsState(initial = emptyList())

    val draftItems by outfitVM.draftOutfitItems.collectAsStateWithLifecycle()
    val isLoading by outfitVM.isLoading.collectAsStateWithLifecycle()
    val draftItemIds = draftItems.map { it.id }.toSet()

    var outfitName by remember { mutableStateOf("") }

    // Khi save thành công (draft empty, không loading) → đóng sheet
    val justSaved = remember { mutableStateOf(false) }
    LaunchedEffect(draftItems, isLoading) {
        if (justSaved.value && draftItems.isEmpty() && !isLoading) {
            onSaved()
        }
    }

    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header ──────────────────────────────────────────────
        Text(
            text = "Tạo Bộ Đồ Mới",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // ── Tên bộ đồ ───────────────────────────────────────────
        OutlinedTextField(
            value = outfitName,
            onValueChange = { outfitName = it },
            label = { Text("Tên bộ đồ") },
            placeholder = { Text("VD: Đi biển mùa hè") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            )
        )

        Spacer(Modifier.height(8.dp))

        // ── Badge hiển thị số lượng đã chọn ─────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Chọn món đồ",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            if (draftItems.isNotEmpty()) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "${draftItems.size}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        // ── Grid chọn items ─────────────────────────────────────
        if (allItems.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = "Tủ đồ đang trống. Hãy thêm quần áo trước nhé!",
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                items(allItems, key = { it.id }) { item ->
                    val isSelected = item.id in draftItemIds
                    SelectableItemCard(
                        item = item,
                        isSelected = isSelected,
                        onClick = {
                            if (isSelected) {
                                outfitVM.removeClothingItemFromDraft(item)
                            } else {
                                outfitVM.addClothingItemToDraft(item)
                            }
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Nút Lưu ─────────────────────────────────────────────
        Button(
            onClick = {
                outfitVM.saveOutfit(outfitName)
                justSaved.value = true
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            enabled = outfitName.isNotBlank() && draftItems.isNotEmpty() && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text("Đang lưu...")
            } else {
                Text("Lưu Bộ Đồ", fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ═════════════════════════════════════════════════════════════════
// 🔷 SelectableItemCard — Item trong grid có thể chọn/bỏ chọn
// ═════════════════════════════════════════════════════════════════
//
// 🎯 Visual feedback:
//   - Đã chọn: viền xanh 3dp + overlay icon ✅ góc trên
//   - Chưa chọn: viền trong suốt, background mờ theo category
// ═════════════════════════════════════════════════════════════════

@Composable
private fun SelectableItemCard(
    item: ClothingItemEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) Color(0xFF4CAF50) else Color.Transparent
    val borderWidth = if (isSelected) 3.dp else 0.dp
    val bgAlpha = if (isSelected) 0.3f else 0.15f
    val imageModel = rememberItemImageModel(item)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(getCategoryColor(item.category).copy(alpha = bgAlpha)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = getCategoryIcon(item.category), fontSize = 28.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = item.name.ifBlank { item.category },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Text(
                        text = item.color,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            if (imageModel != null) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = item.name.ifBlank { item.category },
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Đã chọn",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(24.dp)
                        .background(Color.White, CircleShape)
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = item.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                if (item.brand.isNotBlank()) {
                    Text(
                        text = item.brand,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// 🃏 ClothingItemCard — Card hiển thị một clothing item (GIỮ NGUYÊN)
// ═════════════════════════════════════════════════════════════════

@Composable
fun ClothingItemCard(
    item: ClothingItemEntity,
    onDelete: () -> Unit
) {
    val imageModel = rememberItemImageModel(item)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(getCategoryColor(item.category).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = getCategoryIcon(item.category), fontSize = 32.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = item.imageOriginal.substringAfterLast("/").take(15),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (imageModel != null) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = item.name.ifBlank { item.category },
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                if (item.name.isNotBlank()) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    if (item.brand.isNotBlank()) {
                        Text(
                            text = item.brand,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
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

@Composable
private fun rememberItemImageModel(item: ClothingItemEntity): ImageRequest? {
    val context = LocalContext.current
    return remember(item.imageOriginal, item.imageNoBg) {
        fun resolveData(path: String): Any? {
            if (path.isBlank()) return null
            if (path.startsWith("content://") || path.startsWith("file://")) {
                return Uri.parse(path)
            }
            val file = File(path)
            if (file.isAbsolute) return file
            val internalFile = File(context.filesDir, path)
            if (internalFile.exists()) return internalFile
            val imagesDir = File(context.filesDir, "images")
            val byName = File(imagesDir, File(path).name)
            return if (byName.exists()) byName else internalFile
        }

        val data = resolveData(item.imageOriginal) ?: resolveData(item.imageNoBg)
        data?.let {
            ImageRequest.Builder(context)
                .data(it)
                .crossfade(true)
                .build()
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// 🎨 HELPERS: Màu & Icon theo category
// ═════════════════════════════════════════════════════════════════

private fun getCategoryColor(category: String): Color = when (category) {
    "Tops" -> Color(0xFF42A5F5)
    "Bottoms" -> Color(0xFF66BB6A)
    "Dresses" -> Color(0xFFEC407A)
    "Footwear" -> Color(0xFF8D6E63)
    "Bags" -> Color(0xFFAB47BC)
    "Accessories" -> Color(0xFFFFA726)
    "Jewelry" -> Color(0xFFD4E157)
    else -> Color(0xFFBDBDBD)
}

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

private val sheetCategories = listOf("Tops", "Bottoms", "Dresses", "Footwear", "Bags", "Accessories", "Jewelry")

// ═════════════════════════════════════════════════════════════════
// 📝 NewClothingItemSheet — Bottom sheet thêm item mới (giữ lại)
// ═════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewClothingItemSheet(
    viewModel: ClothingViewModel,
    onItemAdded: () -> Unit,
    onError: (String) -> Unit
) {
    // ── Focus management ──────────────────────────────────────────
    // Mỗi field có FocusRequester riêng, bấm Enter/Done → nhảy field tiếp
    val focusManager = LocalFocusManager.current
    val categoryFocus = remember { FocusRequester() }
    val colorFocus = remember { FocusRequester() }
    val nameFocus = remember { FocusRequester() }
    val brandFocus = remember { FocusRequester() }
    val priceFocus = remember { FocusRequester() }

    // ── State ────────────────────────────────────────────────────
    var category by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var expandedMenu by remember { mutableStateOf(false) }
    var itemName by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var selectedSeason by remember { mutableStateOf("") }
    var selectedOccasion by remember { mutableStateOf("") }
    var purchaseDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    var purchaseDateText by remember { mutableStateOf(dateFormat.format(Date())) }
    var showDatePicker by remember { mutableStateOf(false) }

    val seasons = listOf("Spring", "Summer", "Autumn", "Winter")
    val occasions = listOf("Casual", "Work", "Sports", "Formal")
    val isLoading by viewModel.isLoading.collectAsState()
    val imagePickerState = rememberImagePickerState(onError = onError)
    val imagePath by imagePickerState.imagePath

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Quick Add Item",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        ImagePickerSection(
            title = "Item Image",
            imagePath = imagePath,
            onCameraClick = imagePickerState.onCameraClick,
            onGalleryClick = imagePickerState.onGalleryClick,
            titleStyle = MaterialTheme.typography.labelLarge
        )

        Spacer(Modifier.height(12.dp))

        // ── CATEGORY ─────────────────────────────────────────────
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
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMenu)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
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
                            focusManager.moveFocus(FocusDirection.Down)
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── COLOR ────────────────────────────────────────────────
        Text("Color", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = color,
            onValueChange = { color = it },
            label = { Text("Color (e.g. Red)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )

        Spacer(Modifier.height(12.dp))

        // ── ITEM NAME ────────────────────────────────────────────
        Text("Item Name", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = itemName,
            onValueChange = { itemName = it },
            label = { Text("Enter item name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("e.g. White Shirt") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )

        Spacer(Modifier.height(12.dp))

        // ── BRAND ────────────────────────────────────────────────
        Text("Brand", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = brand,
            onValueChange = { brand = it },
            label = { Text("Enter brand") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("e.g. Uniqlo, Nike") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )

        Spacer(Modifier.height(12.dp))

        // ── PRICE ────────────────────────────────────────────────
        Text("Price", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = price,
            onValueChange = { price = it },
            label = { Text("Enter price") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("e.g. 250000") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            prefix = { Text("₫ ") }
        )

        Spacer(Modifier.height(12.dp))

        // ── SEASON ───────────────────────────────────────────────
        Text("Season", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            seasons.forEach { season ->
                FilterChip(
                    selected = selectedSeason == season,
                    onClick = {
                        selectedSeason = if (selectedSeason == season) "" else season
                    },
                    label = { Text(season) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── OCCASION ─────────────────────────────────────────────
        Text("Occasion", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            occasions.forEach { occasion ->
                FilterChip(
                    selected = selectedOccasion == occasion,
                    onClick = {
                        selectedOccasion = if (selectedOccasion == occasion) "" else occasion
                    },
                    label = { Text(occasion) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── PURCHASE DATE ────────────────────────────────────────
        Text("Purchase Date", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(4.dp))
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = { showDatePicker = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Tap to select date",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = purchaseDateText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── ADD BUTTON ───────────────────────────────────────────
        Button(
            onClick = {
                if (imagePath == null) {
                    onError("Please select an image")
                    return@Button
                }
                if (category.isBlank() || color.isBlank()) return@Button
                val parsedPrice = price.toDoubleOrNull() ?: 0.0
                viewModel.addClothingItem(
                    imageFile = File(imagePath!!),
                    category = category,
                    color = color,
                    name = itemName,
                    season = selectedSeason,
                    occasion = selectedOccasion,
                    brand = brand,
                    purchaseDate = purchaseDate,
                    price = parsedPrice
                )
                onItemAdded()
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            enabled = !isLoading && category.isNotBlank() && color.isNotBlank() && imagePath != null
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

        Spacer(Modifier.height(16.dp))
    }

    // ── DATE PICKER ─────────────────────────────────────────────
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = purchaseDate,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis <= System.currentTimeMillis()
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedMillis ->
                        purchaseDate = selectedMillis
                        purchaseDateText = dateFormat.format(Date(selectedMillis))
                    }
                    showDatePicker = false
                }) { Text("Chọn") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Huỷ") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// 🏷️ CategoryChip — Chip filter danh mục (GIỮ NGUYÊN)
// ═════════════════════════════════════════════════════════════════

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
        color = if (isSelected) Color(0xFFFFD54F) else Color(0xFFF5F5F5),
        contentColor = if (isSelected) Color.Black else Color.Gray
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(text = category, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontSize = 16.sp)
            Spacer(Modifier.width(4.dp))
            Text(text = count.toString(), fontSize = 12.sp, color = if (isSelected) Color.Black.copy(alpha = 0.7f) else Color.Gray)
        }
    }
}

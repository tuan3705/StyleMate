package com.example.stylemate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stylemate.model.ClothingItemEntity
import com.example.stylemate.model.OutfitWithClothingItems
import com.example.stylemate.repository.ClothingRepository
import com.example.stylemate.repository.OutfitRepository
import com.example.stylemate.viewmodel.OutfitViewModel
import com.example.stylemate.viewmodel.OutfitViewModelFactory
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ═════════════════════════════════════════════════════════════════
// 📱 OutfitScreen — Màn hình Phối đồ (Outfit)
// ═════════════════════════════════════════════════════════════════
//
// 🧩 Bố cục UI gồm 3 khu vực chính:
//   1. [CreateOutfitSection]  — Khu vực tạo Outfit mới (draft + tên + nút lưu)
//   2. [ClosetSelectionSection] — Lưới items có thể chọn để thêm vào draft
//   3. [SavedOutfitsSection]  — Danh sách các Outfit đã lưu
//
// 🧠 UX Logic:
//   - Người dùng click item trong lưới → thêm/xoá khỏi draft (toggle).
//   - Draft items hiển thị ở LazyRow phía trên + badge đếm.
//   - Item đã chọn có viền xanh + icon check.
//   - Nút Lưu chỉ enable khi draft có item + tên không rỗng.
//   - Sau khi lưu, draft tự clear, text field reset.
// ═════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutfitScreen() {
    val context = LocalContext.current
    val apiService = com.example.stylemate.network.RetrofitClient.stylemateApiService

    val clothingRepo = ClothingRepository(apiService, context)
    val outfitRepo = OutfitRepository(apiService)
    val viewModel: OutfitViewModel = viewModel(
        factory = OutfitViewModelFactory(outfitRepo)
    )

    // ── Collect StateFlows ──────────────────────────────────────
    val filteredOutfits by viewModel.filteredOutfits.collectAsStateWithLifecycle()
    val outfitSearchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val draftItems by viewModel.draftOutfitItems.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    // 📦 Lấy toàn bộ ClothingItem từ DB (dùng trực tiếp Repository qua collectAsState)
    val allClothingItems by clothingRepo.getAllItems()
        .collectAsState(initial = emptyList())

    // ── Local state ─────────────────────────────────────────────
    var outfitName by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    // ── Hiển thị lỗi qua Snackbar ───────────────────────────────
    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }

    // ── Tính toán số lượng items đã chọn ────────────────────────
    val selectedCount = draftItems.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Phối Đồ", fontWeight = FontWeight.Bold)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ═══════════════════════════════════════════════════════
            // KHU VỰC 1: TẠO OUTFIT MỚI
            // ═══════════════════════════════════════════════════════
            item {
                CreateOutfitSection(
                    draftItems = draftItems,
                    selectedCount = selectedCount,
                    outfitName = outfitName,
                    onOutfitNameChange = { outfitName = it },
                    onRemoveFromDraft = { viewModel.removeClothingItemFromDraft(it) },
                    onClearDraft = { viewModel.clearDraft() },
                    onSave = {
                        viewModel.saveOutfit(outfitName)
                        outfitName = ""  // Clear text field sau khi lưu
                    },
                    isLoading = isLoading,
                    isSaveEnabled = selectedCount > 0 && outfitName.isNotBlank()
                )
            }

            // ═══════════════════════════════════════════════════════
            // KHU VỰC 2: LƯỚI ITEMS ĐỂ CHỌN (Tủ đồ)
            // ═══════════════════════════════════════════════════════
            item {
                ClosetSelectionSection(
                    items = allClothingItems,
                    draftItemIds = draftItems.map { it.id }.toSet(),
                    onItemToggle = { item ->
                        // 🎯 Toggle: nếu đã có trong draft thì xoá, chưa có thì thêm
                        if (draftItems.any { it.id == item.id }) {
                            viewModel.removeClothingItemFromDraft(item)
                        } else {
                            viewModel.addClothingItemToDraft(item)
                        }
                    }
                )
            }

            // ═══════════════════════════════════════════════════════
            // KHU VỰC 3: DANH SÁCH OUTFIT ĐÃ LƯU
            // ═══════════════════════════════════════════════════════
            item {
                OutfitSearchBar(
                    query = outfitSearchQuery,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    onClearQuery = { viewModel.clearSearchQuery() }
                )
            }

            item {
                SavedOutfitsHeader(outfitsCount = filteredOutfits.size)
            }

            if (filteredOutfits.isEmpty()) {
                item {
                    val emptyMessage = if (outfitSearchQuery.isNotBlank()) {
                        "Không tìm thấy bộ đồ phù hợp"
                    } else {
                        "Chưa có bộ đồ nào"
                    }
                    Text(
                        text = emptyMessage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(filteredOutfits, key = { it.outfit.id }) { outfitWithItems ->
                    SavedOutfitCard(
                        outfitWithItems = outfitWithItems,
                        onDelete = { viewModel.deleteOutfit(outfitWithItems.outfit) }
                    )
                }
            }

            // Spacer bottom
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun OutfitSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Tìm bộ đồ") },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search")
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(
                    onClick = {
                        onClearQuery()
                        focusManager.clearFocus()
                    }
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = { focusManager.clearFocus() }
        )
    )
}

// ═════════════════════════════════════════════════════════════════
// 🔷 COMPONENT 1: CreateOutfitSection
// ═════════════════════════════════════════════════════════════════
//
// 🎯 UX Logic:
//   - LazyRow hiển thị các item đang chọn (draft), mỗi item có nút "x" để bỏ chọn.
//   - Nếu draft rỗng → hiển thị text hướng dẫn.
//   - TextField + Nút Lưu ở bên dưới.
//   - Nút Lưu chỉ enable khi có item + tên không trống.
// ═════════════════════════════════════════════════════════════════

@Composable
private fun CreateOutfitSection(
    draftItems: List<ClothingItemEntity>,
    selectedCount: Int,
    outfitName: String,
    onOutfitNameChange: (String) -> Unit,
    onRemoveFromDraft: (ClothingItemEntity) -> Unit,
    onClearDraft: () -> Unit,
    onSave: () -> Unit,
    isLoading: Boolean,
    isSaveEnabled: Boolean
) {
    val focusManager = LocalFocusManager.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ── Header: Icon + Title + Badge ────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Style,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Tạo Bộ Đồ Mới",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                // 🏷️ Badge đếm số lượng items đã chọn
                if (selectedCount > 0) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "$selectedCount",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Danh sách Draft Items (LazyRow ngang) ───────────
            if (draftItems.isEmpty()) {
                // 📝 Empty state: hướng dẫn người dùng
                Text(
                    text = "Hãy chọn các món đồ bên dưới để tạo bộ đồ mới",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    items(draftItems, key = { it.id }) { item ->
                        DraftItemChip(
                            item = item,
                            onRemove = { onRemoveFromDraft(item) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Outfit Name TextField ───────────────────────────
            OutlinedTextField(
                value = outfitName,
                onValueChange = onOutfitNameChange,
                label = { Text("Tên bộ đồ") },
                placeholder = { Text("VD: Đi biển mùa hè, Dạo phố...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                )
            )

            Spacer(Modifier.height(12.dp))

            // ── Row: Nút Xoá Draft + Nút Lưu ────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 🧹 Nút xoá draft — chỉ hiện khi có item
                if (selectedCount > 0) {
                    OutlinedButton(
                        onClick = onClearDraft,
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Huỷ")
                    }
                }

                // 💾 Nút Lưu Outfit
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(if (selectedCount > 0) 1.5f else 1f).height(48.dp),
                    enabled = isSaveEnabled && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = if (isLoading) "Đang lưu..." else "Lưu Bộ Đồ",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// 🔷 COMPONENT 1a: DraftItemChip
// ═════════════════════════════════════════════════════════════════
//
// 🎯 UX Logic:
//   - Mỗi item trong draft được hiển thị dạng chip nhỏ.
//   - Có icon category + tên item + nút "×" để bỏ chọn.
// ═════════════════════════════════════════════════════════════════

@Composable
private fun DraftItemChip(
    item: ClothingItemEntity,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon đại diện category
            Text(
                text = getCategoryIcon(item.category),
                fontSize = 16.sp
            )
            Spacer(Modifier.width(6.dp))
            // Tên item (rút gọn nếu dài)
            Text(
                text = item.name.ifBlank { item.category },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 100.dp)
            )
            Spacer(Modifier.width(4.dp))
            // Nút xoá
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Xoá khỏi draft",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// 🔷 COMPONENT 2: ClosetSelectionSection
// ═════════════════════════════════════════════════════════════════
//
// 🎯 UX Logic:
//   - Lưới 2 cột hiển thị tất cả items trong tủ đồ.
//   - Click item → toggle add/remove khỏi draft.
//   - Visual feedback:
//       * Item đã chọn: viền xanh dày 3dp + icon checkmark góc.
//       * Item chưa chọn: viền trong suốt.
// ═════════════════════════════════════════════════════════════════

@Composable
private fun ClosetSelectionSection(
    items: List<ClothingItemEntity>,
    draftItemIds: Set<String>,
    onItemToggle: (ClothingItemEntity) -> Unit
) {
    Column {
        // ── Header ──────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text(
                text = "Chọn món đồ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "(${items.size} món)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (items.isEmpty()) {
            // Empty state
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = "Tủ đồ của bạn đang trống.\nHãy thêm quần áo vào tủ trước nhé!",
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // 📱 Lưới 2 cột — mỗi item có thể click để toggle chọn
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp) // Giới hạn chiều cao để scroll phần còn lại
            ) {
                items(items, key = { it.id }) { item ->
                    val isSelected = item.id in draftItemIds
                    SelectableClothingItemCard(
                        item = item,
                        isSelected = isSelected,
                        onClick = { onItemToggle(item) }
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// 🔷 COMPONENT 2a: SelectableClothingItemCard
// ═════════════════════════════════════════════════════════════════
//
// 🎯 UX Logic:
//   - Khi isSelected = true:
//       * Viền xanh lá 3dp (border)
//       * Overlay icon CheckCircle ở góc trên phải
//       * Background hơi xanh nhẹ
//   - Khi isSelected = false:
//       * Viền trong suốt
//       * Background màu theo category (giống ClosetScreen)
// ═════════════════════════════════════════════════════════════════

@Composable
private fun SelectableClothingItemCard(
    item: ClothingItemEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // 🎨 Màu viền: xanh lá nếu được chọn, trong suốt nếu không
    val borderColor = if (isSelected) Color(0xFF4CAF50) else Color.Transparent
    val borderWidth = if (isSelected) 3.dp else 0.dp
    val bgAlpha = if (isSelected) 0.3f else 0.15f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // ── Background: màu theo category ───────────────────
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(getCategoryColor(item.category).copy(alpha = bgAlpha)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Icon category
                    Text(
                        text = getCategoryIcon(item.category),
                        fontSize = 28.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    // Tên item
                    Text(
                        text = item.name.ifBlank { item.category },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    // Màu sắc
                    Text(
                        text = item.color,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            // ── Overlay khi được chọn: Icon Checkmark góc trên phải ──
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

            // ── Thông tin phía dưới (category + brand) ──────────
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
// 🔷 COMPONENT 3: SavedOutfitsHeader
// ═════════════════════════════════════════════════════════════════

@Composable
private fun SavedOutfitsHeader(outfitsCount: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Text(
            text = "Bộ Đồ Đã Lưu",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "($outfitsCount bộ)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ═════════════════════════════════════════════════════════════════
// 🔷 COMPONENT 4: SavedOutfitCard
// ═════════════════════════════════════════════════════════════════
//
// 🎯 UX Logic:
//   - Card hiển thị một Outfit đã lưu.
//   - Gồm: Tên outfit, ngày tạo (format dd/MM/yyyy), danh sách items dạng LazyRow.
//   - Mỗi item là một chip nhỏ hiển thị icon category + tên.
//   - Nút Delete để xoá outfit (CASCADE sẽ xoá luôn CrossRef).
// ═════════════════════════════════════════════════════════════════

@Composable
private fun SavedOutfitCard(
    outfitWithItems: OutfitWithClothingItems,
    onDelete: () -> Unit
) {
    val outfit = outfitWithItems.outfit
    val items = outfitWithItems.clothingItems

    // Format ngày tạo
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
            // ── Header: Tên Outfit + Ngày tạo + Nút Xoá ─────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
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

                // Tên + ngày
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

                // Nút xoá
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Xoá bộ đồ",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // ── Danh sách Items trong Outfit (LazyRow) ──────────
            if (items.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Món đồ trong bộ:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        OutfitItemThumbnail(item = item)
                    }
                }
            } else {
                // Empty state (hiếm khi xảy ra nhờ validation)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "⚠️ Bộ đồ này hiện không có món nào.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// 🔷 COMPONENT 4a: OutfitItemThumbnail
// ═════════════════════════════════════════════════════════════════
//
// 🎯 UX Logic:
//   - Chip nhỏ hiển thị từng item trong outfit đã lưu.
//   - Gồm icon category + tên item.
// ═════════════════════════════════════════════════════════════════

@Composable
private fun OutfitItemThumbnail(item: ClothingItemEntity) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = getCategoryColor(item.category).copy(alpha = 0.15f),
        modifier = Modifier.width(100.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = getCategoryIcon(item.category), fontSize = 24.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.name.ifBlank { item.category },
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// 🎨 HELPER: Màu nền theo category
// ═════════════════════════════════════════════════════════════════

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

// ═════════════════════════════════════════════════════════════════
// 🎨 HELPER: Icon emoji theo category
// ═════════════════════════════════════════════════════════════════

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

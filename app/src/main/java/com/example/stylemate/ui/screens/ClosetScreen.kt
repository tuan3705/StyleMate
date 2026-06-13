package com.example.stylemate.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.stylemate.R
import com.example.stylemate.model.Categories
import com.example.stylemate.model.ClothingItemEntity
import com.example.stylemate.model.OutfitEntity
import com.example.stylemate.model.OutfitItemWithPosition
import com.example.stylemate.model.OutfitWithClothingItems
import com.example.stylemate.repository.ClothingRepository
import com.example.stylemate.repository.OutfitRepository
import com.example.stylemate.repository.ImageProcessingRepository
import com.example.stylemate.ui.common.ImagePickerSection
import com.example.stylemate.ui.common.rememberImagePickerState
import com.example.stylemate.ui.common.resolveImageData
import com.example.stylemate.viewmodel.ImageProcessingViewModel
import com.example.stylemate.viewmodel.ImageProcessingViewModelFactory
import com.example.stylemate.viewmodel.ClothingViewModel
import com.example.stylemate.viewmodel.ClothingViewModelFactory
import com.example.stylemate.ui.components.CategoryIconImage
import com.example.stylemate.viewmodel.OutfitViewModel
import com.example.stylemate.viewmodel.OutfitViewModelFactory
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

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
fun ClosetScreen(
    onEditItem: (String) -> Unit,
    refreshSignal: StateFlow<Boolean>,
    onRefreshConsumed: () -> Unit,
    pendingActionSignal: StateFlow<String?>,
    onPendingActionConsumed: () -> Unit,
    accountMenu: @Composable () -> Unit = {}
) {
    val context = LocalContext.current

    // ── Khởi tạo Repositories & ViewModels (dùng RetrofitClient thay AppDatabase) ─
    val apiService = com.example.stylemate.network.RetrofitClient.stylemateApiService
    val clothingRepo = ClothingRepository(apiService, context)
    val clothingVM: ClothingViewModel = viewModel(
        factory = ClothingViewModelFactory(clothingRepo)
    )

    val outfitRepo = OutfitRepository(apiService)
    val outfitVM: OutfitViewModel = viewModel(
        factory = OutfitViewModelFactory(outfitRepo)
    )

    // ── Collect StateFlows (Items) ──────────────────────────────
    val selectedCategory by clothingVM.selectedCategory.collectAsStateWithLifecycle()
    val filteredItems by clothingVM.filteredItems.collectAsStateWithLifecycle()
    val searchQuery by clothingVM.searchQuery.collectAsStateWithLifecycle()
    val isItemsLoading by clothingVM.isLoading.collectAsStateWithLifecycle()
    val errorMessage by clothingVM.errorMessage.collectAsStateWithLifecycle()
    val allClosetItems by clothingVM.allItems.collectAsStateWithLifecycle()

    // ── Collect StateFlows (Outfits) ────────────────────────────
    val outfits by outfitVM.outfits.collectAsStateWithLifecycle()
    val isOutfitLoading by outfitVM.isLoading.collectAsStateWithLifecycle()
    val outfitError by outfitVM.errorMessage.collectAsStateWithLifecycle()
    val editingItems by outfitVM.editingItems.collectAsStateWithLifecycle()
    val editingOutfitName by outfitVM.editingOutfitName.collectAsStateWithLifecycle()
    val editSaveSuccess by outfitVM.editSaveSuccess.collectAsStateWithLifecycle()

    // ── Local states ────────────────────────────────────────────
    var selectedTab by remember { mutableIntStateOf(0) }  // 0 = Items, 1 = Outfits
    var showBottomSheet by remember { mutableStateOf(false) }    // Quick Add item
    var showCreateOutfitSheet by remember { mutableStateOf(false) } // Tạo outfit
    var showOutfitEditor by remember { mutableStateOf(false) }
    var showAddItemsSheet by remember { mutableStateOf(false) }
    var showSearchBar by rememberSaveable { mutableStateOf(false) }
    var itemPendingDelete by remember { mutableStateOf<ClothingItemEntity?>(null) }
    var outfitPendingDelete by remember { mutableStateOf<OutfitEntity?>(null) }
    val itemsGridState = rememberLazyGridState()
    // Mỗi BottomSheet có sheetState riêng để vuốt mượt, không conflict
    val quickAddSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val createOutfitSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val allCategories = listOf(Categories.ALL) + Categories.list
    val tabs = listOf(context.getString(R.string.tab_items), context.getString(R.string.tab_outfits))
    val isItemsAtTop by remember {
        derivedStateOf {
            itemsGridState.firstVisibleItemIndex == 0 &&
                    itemsGridState.firstVisibleItemScrollOffset == 0
        }
    }
    val itemsScrollConnection = remember(itemsGridState, selectedTab) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (selectedTab != 0) return Offset.Zero
                if (!isItemsAtTop) {
                    showSearchBar = false
                    return Offset.Zero
                }
                when {
                    available.y > 0 -> showSearchBar = true
                    available.y < 0 -> showSearchBar = false
                }
                return Offset.Zero
            }
        }
    }

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
    LaunchedEffect(isItemsAtTop, selectedTab) {
        if (selectedTab != 0 || !isItemsAtTop) {
            showSearchBar = false
        }
    }

    val refreshRequested by refreshSignal.collectAsStateWithLifecycle()

    LaunchedEffect(editSaveSuccess) {
        if (editSaveSuccess) {
            showOutfitEditor = false
            showAddItemsSheet = false
            outfitVM.clearEditSaveSuccess()
        }
    }
    LaunchedEffect(refreshRequested) {
        if (refreshRequested) {
            clothingVM.refreshItems()
            onRefreshConsumed()
        }
    }

    // 🔗 Pending action tu AI Stylist: mo sheet tuong ung sau khi chuyen sang Closet
    val pendingAction by pendingActionSignal.collectAsStateWithLifecycle()
    LaunchedEffect(pendingAction) {
        when (pendingAction) {
            "add_item" -> {
                selectedTab = 0
                showBottomSheet = true
                onPendingActionConsumed()
            }
            "create_outfit" -> {
                selectedTab = 1
                showCreateOutfitSheet = true
                onPendingActionConsumed()
            }
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
                    contentDescription = if (selectedTab == 0) stringResource(R.string.add_item_fab_desc) else stringResource(R.string.create_outfit_fab_desc)
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
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.closet_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    accountMenu()
                }
            }

            AnimatedVisibility(
                visible = selectedTab == 0 && showSearchBar,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(Modifier.height(8.dp))
                    ClosetSearchBar(
                        query = searchQuery,
                        onQueryChange = clothingVM::updateSearchQuery,
                        onClearQuery = clothingVM::clearSearchQuery
                    )
                    Spacer(Modifier.height(8.dp))
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
                    items = filteredItems,
                    searchQuery = searchQuery,
                    isLoading = isItemsLoading,
                    allCategories = allCategories,
                    gridState = itemsGridState,
                    nestedScrollConnection = itemsScrollConnection,
                    onItemClick = { itemId -> onEditItem(itemId) },
                    onDeleteItem = { item ->
                        itemPendingDelete = item
                    }
                )
            }
            // ═══════════════════════════════════════════════════════
            // TAB 1: BỘ ĐỒ
            // ═══════════════════════════════════════════════════════
            else {
                OutfitsTabContent(
                    outfits = outfits,
                    isLoading = isOutfitLoading,
                    outfitRepo = outfitRepo,
                    onDeleteOutfit = { outfitPendingDelete = it },
                    onOutfitClick = { outfit ->
                        outfitVM.startEditingOutfit(outfit.outfit.id, outfit.outfit.name)
                        showOutfitEditor = true
                    }
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

    if (showOutfitEditor) {
        OutfitCanvasEditorDialog(
            outfitName = editingOutfitName,
            items = editingItems,
            isSaving = isOutfitLoading,
            onDismiss = {
                showOutfitEditor = false
                showAddItemsSheet = false
            },
            onAddItems = { showAddItemsSheet = true },
            onSave = { outfitVM.saveEditingOutfit() },
            onPositionChange = { itemId, posX, posY ->
                outfitVM.updateEditingItemPosition(itemId, posX, posY)
            },
            onScaleChange = { itemId, scale ->
                outfitVM.updateEditingItemScale(itemId, scale)
            },
            onDeleteItem = { itemId ->
                outfitVM.removeEditingItem(itemId)
            }
        )
    }

    if (showOutfitEditor && showAddItemsSheet) {
        AddItemsBottomSheet(
            allItems = allClosetItems,
            existingIds = editingItems.map { it.item.id }.toSet(),
            onDismiss = { showAddItemsSheet = false },
            onConfirm = { selectedItems ->
                outfitVM.addItemsToEditing(selectedItems)
                showAddItemsSheet = false
            }
        )
    }

    itemPendingDelete?.let { item ->
        DeleteConfirmDialog(
            title = stringResource(R.string.confirm_delete_item_title),
            message = stringResource(R.string.confirm_delete_item_message),
            onDismiss = { itemPendingDelete = null },
            onConfirm = {
                itemPendingDelete = null
                clothingVM.deleteClothingItem(item) {
                    outfitVM.refreshAfterItemChange()
                }
            }
        )
    }

    outfitPendingDelete?.let { outfit ->
        DeleteConfirmDialog(
            title = stringResource(R.string.confirm_delete_outfit_title),
            message = stringResource(R.string.confirm_delete_outfit_message),
            onDismiss = { outfitPendingDelete = null },
            onConfirm = {
                outfitPendingDelete = null
                outfitVM.deleteOutfit(outfit)
            }
        )
    }
}

@Composable
private fun DeleteConfirmDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(stringResource(R.string.delete_confirm_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_button))
            }
        }
    )
}

@Composable
private fun ClosetSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        placeholder = { Text(stringResource(R.string.search_hint)) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search_content_desc))
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(
                    onClick = {
                        onClearQuery()
                        focusManager.clearFocus()
                    }
                ) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear_search_content_desc))
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = { focusManager.clearFocus() }
        )
    )
}

// ════════════════════════════════════════════════════════════════
// 🔷 TAB 0: ItemsTabContent (Canvas view)
// ═════════════════════════════════════════════════════════════════

@Composable
private fun ItemsTabContent(
    clothingVM: ClothingViewModel,
    selectedCategory: String,
    items: List<ClothingItemEntity>,
    searchQuery: String,
    isLoading: Boolean,
    allCategories: List<String>,
    gridState: LazyGridState,
    nestedScrollConnection: NestedScrollConnection,
    onItemClick: (String) -> Unit,
    onDeleteItem: (ClothingItemEntity) -> Unit
) {
    val context = LocalContext.current
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

        // ── Content: Items Grid ─────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                val isSearching = searchQuery.isNotBlank()
                val emptyMessage = if (isSearching) {
                    context.getString(R.string.empty_search_result)
                } else {
                    context.getString(R.string.empty_closet_no_items)
                }
                LazyVerticalGrid(
                    state = gridState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection),
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (items.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = emptyMessage,
                                    color = Color.Gray,
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(items, key = { it.id }) { clothingItem ->
                            ClothingItemCard(
                                item = clothingItem,
                                onClick = { onItemClick(clothingItem.id) },
                                onDelete = { onDeleteItem(clothingItem) }
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
    outfitRepo: OutfitRepository,
    onDeleteOutfit: (com.example.stylemate.model.OutfitEntity) -> Unit,
    onOutfitClick: (OutfitWithClothingItems) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        when {
            isLoading -> CircularProgressIndicator()
            outfits.isEmpty() -> {
                // 📝 Empty state
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp)
                ) {
                    Text(text = "🧥", fontSize = 48.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.empty_outfits_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.empty_outfits_hint),
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
                            outfitRepo = outfitRepo,
                            onDelete = { onDeleteOutfit(outfitWithItems.outfit) },
                            onClick = { onOutfitClick(outfitWithItems) }
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
    outfitRepo: OutfitRepository,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val outfit = outfitWithItems.outfit
    val items = outfitWithItems.clothingItems
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val formattedDate = remember(outfit.createdAt) {
        dateFormat.format(Date(outfit.createdAt))
    }
    // ⚡ CẢI TIẾN: Dùng items từ outfitWithItems (đã có sẵn khi load outfit list),
    // KHÔNG gọi API riêng cho từng card (tránh N+1).
    val previewPlacements = remember(items) {
        mapClothingItemsToPlacements(items)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
                    CategoryIconImage(
                            category = items.firstOrNull()?.category ?: "Other",
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
                        text = stringResource(R.string.closet_outfit_card_info_format, formattedDate, items.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete_outfit_content_desc),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            OutfitCanvasPreview(items = previewPlacements)
        }
    }
}

private fun mapOutfitPreviewPositions(
    items: List<OutfitItemWithPosition>
): List<OutfitViewModel.OutfitItemPlacement> {
    if (items.isEmpty()) return emptyList()
    val hasCustomPos = items.any { it.posX != 0f || it.posY != 0f }
    return if (hasCustomPos) {
        items.map { OutfitViewModel.OutfitItemPlacement(it.item, it.posX, it.posY, it.scale) }
    } else {
        items.mapIndexed { index, entry ->
            val (x, y) = defaultOutfitGridPosition(index)
            OutfitViewModel.OutfitItemPlacement(entry.item, x, y, entry.scale)
        }
    }
}

/**
 * Chuyển List<ClothingItemEntity> (từ outfit list đã load) thành
 * List<OutfitItemPlacement> với vị trí grid mặc định.
 * ⚡ Không gọi API riêng cho từng card.
 */
private fun mapClothingItemsToPlacements(
    items: List<ClothingItemEntity>
): List<OutfitViewModel.OutfitItemPlacement> {
    if (items.isEmpty()) return emptyList()
    // Ưu tiên vị trí đã lưu (canvasPosX/canvasPosY); chỉ fallback grid mặc định
    // khi toàn bộ item chưa có vị trí (outfit cũ lưu vị trí 0).
    val hasCustomPos = items.any { it.canvasPosX != 0f || it.canvasPosY != 0f }
    return if (hasCustomPos) {
        items.map { OutfitViewModel.OutfitItemPlacement(it, it.canvasPosX, it.canvasPosY, it.canvasScale) }
    } else {
        items.mapIndexed { index, item ->
            val (x, y) = defaultOutfitGridPosition(index)
            OutfitViewModel.OutfitItemPlacement(item, x, y, item.canvasScale)
        }
    }
}

private fun defaultOutfitGridPosition(index: Int): Pair<Float, Float> {
    val col = index % 2
    val row = index / 2
    val x = if (col == 0) 0.1f else 0.55f
    val y = (0.1f + row * 0.25f).coerceAtMost(0.8f)
    return x to y
}

// ─────────────────────────────────────────────────────────────────
// 📐 Hình học canvas dùng chung cho editor & preview.
// Cả hai phải ĐỒNG DẠNG (cùng aspect ratio + item theo cùng tỉ lệ bề rộng)
// thì bố cục chuẩn hoá 0..1 mới tái hiện y hệt nhau (tránh đè item ở preview).
// ─────────────────────────────────────────────────────────────────
private const val OUTFIT_CANVAS_ASPECT_RATIO = 0.82f   // width / height
private const val OUTFIT_ITEM_SIZE_FRACTION = 0.30f    // item width = 30% bề rộng canvas

@Composable
private fun OutfitCanvasPreview(
    items: List<OutfitViewModel.OutfitItemPlacement>
) {
    val density = LocalDensity.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F2EA))
    ) {
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_items_in_outfit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Card
        }

        // Bọc trong Box cao tối đa 220dp; canvas con giữ đúng aspect ratio của editor
        // (tự co bề rộng → canh giữa) để bố cục đồng dạng, item không bị đè.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            contentAlignment = Alignment.Center
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(OUTFIT_CANVAS_ASPECT_RATIO)
            ) {
                val canvasWidth = constraints.maxWidth.toFloat()
                val canvasHeight = constraints.maxHeight.toFloat()
                val itemSizePx = canvasWidth * OUTFIT_ITEM_SIZE_FRACTION

                items.forEach { placement ->
                    val item = placement.item
                    val imageModel = rememberItemImageModel(item)
                    val scaledSizePx = itemSizePx * placement.scale
                    val itemSize = with(density) { scaledSizePx.toDp() }
                    val maxX = (canvasWidth - scaledSizePx).coerceAtLeast(1f)
                    val maxY = (canvasHeight - scaledSizePx).coerceAtLeast(1f)
                    val offsetX = (placement.posX * maxX).roundToInt()
                    val offsetY = (placement.posY * maxY).roundToInt()

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(offsetX, offsetY) }
                            .size(itemSize)
                    ) {
                        if (imageModel != null) {
                            AsyncImage(
                                model = imageModel,
                                contentDescription = item.name.ifBlank { item.category },
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(getCategoryColor(item.category).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CategoryIconImage(category = item.category, fontSize = 24.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OutfitCanvasEditorDialog(
    outfitName: String,
    items: List<OutfitViewModel.OutfitItemPlacement>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onAddItems: () -> Unit,
    onSave: () -> Unit,
    onPositionChange: (String, Float, Float) -> Unit,
    onScaleChange: (String, Float) -> Unit,
    onDeleteItem: (String) -> Unit
) {
    var selectedItemId by remember { mutableStateOf<String?>(null) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = outfitName.ifBlank { stringResource(R.string.outfit_canvas_title) },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close_content_desc))
                    }
                }

                OutfitCanvas(
                    items = items,
                    selectedItemId = selectedItemId,
                    onSelectItem = { selectedItemId = it },
                    onDeleteItem = { itemId ->
                        onDeleteItem(itemId)
                        if (selectedItemId == itemId) selectedItemId = null
                    },
                    onPositionChange = onPositionChange,
                    onScaleChange = onScaleChange
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = onAddItems,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.add_items_button))
                    }
                    Button(
                        onClick = onSave,
                        modifier = Modifier.weight(1f),
                        enabled = items.isNotEmpty() && !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.saving_label))
                        } else {
                            Text(stringResource(R.string.save_outfit_button))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OutfitCanvas(
    items: List<OutfitViewModel.OutfitItemPlacement>,
    selectedItemId: String?,
    onSelectItem: (String) -> Unit,
    onDeleteItem: (String) -> Unit,
    onPositionChange: (String, Float, Float) -> Unit,
    onScaleChange: (String, Float) -> Unit
) {
    val density = LocalDensity.current
    val minScale = 0.4f
    val maxScale = 2.0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F2EA))
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(OUTFIT_CANVAS_ASPECT_RATIO)
        ) {
            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.no_items_in_outfit),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                return@BoxWithConstraints
            }

            val canvasWidth = constraints.maxWidth.toFloat()
            val canvasHeight = constraints.maxHeight.toFloat()
            // Base item size (chưa scale) — scale của từng item nhân lên trên giá trị này.
            val itemSizePx = canvasWidth * OUTFIT_ITEM_SIZE_FRACTION

            items.forEach { placement ->
                val item = placement.item
                val imageModel = rememberItemImageModel(item)
                val isSelected = item.id == selectedItemId
                var localPos by remember(item.id) {
                    mutableStateOf(Offset(placement.posX, placement.posY))
                }
                var localScale by remember(item.id) {
                    mutableStateOf(placement.scale)
                }
                LaunchedEffect(placement.posX, placement.posY) {
                    localPos = Offset(placement.posX, placement.posY)
                }
                LaunchedEffect(placement.scale) {
                    localScale = placement.scale
                }

                // maxX/maxY phụ thuộc kích thước đã scale → tính theo từng item.
                val scaledSizePx = itemSizePx * localScale
                val itemSize = with(density) { scaledSizePx.toDp() }
                val maxX = (canvasWidth - scaledSizePx).coerceAtLeast(1f)
                val maxY = (canvasHeight - scaledSizePx).coerceAtLeast(1f)

                val offsetX = (localPos.x * maxX).roundToInt()
                val offsetY = (localPos.y * maxY).roundToInt()

                Box(
                    modifier = Modifier
                        .offset { IntOffset(offsetX, offsetY) }
                        .size(itemSize)
                        .pointerInput(item.id) {
                            detectTapGestures(onTap = { onSelectItem(item.id) })
                        }
                        .pointerInput(item.id, maxX, maxY) {
                            detectDragGestures(
                                onDragStart = { onSelectItem(item.id) },
                                onDrag = { change, dragAmount ->
                                    change.consumePositionChange()
                                    val newX = (localPos.x * maxX + dragAmount.x).coerceIn(0f, maxX)
                                    val newY = (localPos.y * maxY + dragAmount.y).coerceIn(0f, maxY)
                                    localPos = Offset(newX / maxX, newY / maxY)
                                },
                                onDragEnd = {
                                    onPositionChange(item.id, localPos.x, localPos.y)
                                }
                            )
                        }
                ) {
                    if (imageModel != null) {
                        AsyncImage(
                            model = imageModel,
                            contentDescription = item.name.ifBlank { item.category },
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(getCategoryColor(item.category).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CategoryIconImage(category = item.category, fontSize = 28.sp)
                        }
                    }
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .border(2.dp, Color(0xFFFFD54F), RoundedCornerShape(12.dp))
                        )
                        // Handle resize (góc dưới-phải): kéo để phóng to/thu nhỏ item.
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 6.dp, y = 6.dp)
                                .size(18.dp)
                                .zIndex(1f)
                                .background(Color(0xFFFFD54F), CircleShape)
                                .pointerInput(item.id, canvasWidth, canvasHeight) {
                                    detectDragGestures(
                                        onDragStart = { onSelectItem(item.id) },
                                        onDrag = { change, dragAmount ->
                                            change.consumePositionChange()
                                            // Giữ nguyên vị trí pixel hiện tại, đổi scale rồi
                                            // tái chuẩn hoá pos theo maxX/maxY mới (item không nhảy/tràn).
                                            val curMaxX = (canvasWidth - itemSizePx * localScale).coerceAtLeast(1f)
                                            val curMaxY = (canvasHeight - itemSizePx * localScale).coerceAtLeast(1f)
                                            val curPxX = localPos.x * curMaxX
                                            val curPxY = localPos.y * curMaxY
                                            val delta = (dragAmount.x + dragAmount.y) / itemSizePx
                                            val newScale = (localScale + delta).coerceIn(minScale, maxScale)
                                            val newMaxX = (canvasWidth - itemSizePx * newScale).coerceAtLeast(1f)
                                            val newMaxY = (canvasHeight - itemSizePx * newScale).coerceAtLeast(1f)
                                            localScale = newScale
                                            localPos = Offset(
                                                curPxX.coerceIn(0f, newMaxX) / newMaxX,
                                                curPxY.coerceIn(0f, newMaxY) / newMaxY
                                            )
                                        },
                                        onDragEnd = {
                                            onScaleChange(item.id, localScale)
                                            onPositionChange(item.id, localPos.x, localPos.y)
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "⇔", fontSize = 10.sp, color = Color.Black)
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 5.dp, y = (-5).dp)
                                .size(16.dp)
                                .zIndex(1f)
                                .background(Color.Red.copy(alpha = 0.85f), CircleShape)
                                .clickable { onDeleteItem(item.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.remove_item_content_desc),
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddItemsBottomSheet(
    allItems: List<ClothingItemEntity>,
    existingIds: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<ClothingItemEntity>) -> Unit
) {
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    LaunchedEffect(existingIds) {
        selectedIds = emptySet()
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
                Text(
                text = stringResource(R.string.add_items_button),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 360.dp)
            ) {
                items(allItems, key = { it.id }) { item ->
                    val alreadyAdded = item.id in existingIds
                    val isSelected = item.id in selectedIds || alreadyAdded
                    AddItemSelectCard(
                        item = item,
                        isSelected = isSelected,
                        enabled = !alreadyAdded,
                        onToggle = {
                            selectedIds = if (item.id in selectedIds) {
                                selectedIds - item.id
                            } else {
                                selectedIds + item.id
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.cancel_outfit_button))
                }
                Button(
                    onClick = {
                        val selectedItems = allItems.filter { it.id in selectedIds }
                        onConfirm(selectedItems)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = selectedIds.isNotEmpty()
                ) {
                    Text(stringResource(R.string.add_outfit_button))
                }
            }
        }
    }
}

@Composable
private fun AddItemSelectCard(
    item: ClothingItemEntity,
    isSelected: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    val imageModel = rememberItemImageModel(item)
    val alpha = if (enabled) 1f else 0.5f
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable(enabled = enabled) { onToggle() }
            .alpha(alpha),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (imageModel != null) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = item.name.ifBlank { item.category },
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(getCategoryColor(item.category).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    CategoryIconImage(category = item.category, fontSize = 24.sp)
                }
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = stringResource(R.string.selected_content_desc),
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
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
                    text = item.name.ifBlank { item.category },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
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
// ════════════════════════════════════════════════════════════════

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
            text = stringResource(R.string.new_outfit_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // ── Tên bộ đồ ───────────────────────────────────────────
        OutlinedTextField(
            value = outfitName,
            onValueChange = { outfitName = it },
            label = { Text(stringResource(R.string.outfit_name_label)) },
            placeholder = { Text(stringResource(R.string.outfit_name_placeholder)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            )
        )

        Spacer(Modifier.height(8.dp))

        // ── Badge hiển thị số lượng đã chọn ───────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.select_items_title),
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
                    text = stringResource(R.string.empty_closet_warning),
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

        // ── Nút Lưu ────────────────────────────────────────────
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
                Text(stringResource(R.string.saving_label))
            } else {
                Text(stringResource(R.string.save_outfit_vn_button), fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ═════════════════════════════════════════════════════════════════
// 🖼️ Fallback composables cho item cards
// ═════════════════════════════════════════════════════════════════

@Composable
private fun SelectableItemCardFallback(item: ClothingItemEntity, bgAlpha: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(getCategoryColor(item.category).copy(alpha = bgAlpha)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CategoryIconImage(category = item.category, fontSize = 28.sp)
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
}

@Composable
private fun ClothingItemCardFallback(item: ClothingItemEntity) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(getCategoryColor(item.category).copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CategoryIconImage(category = item.category, fontSize = 32.sp)
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
}

// ═══════════════════════════════════════════════════════════════
// 🔷 SelectableItemCard — Item trong grid có thể chọn/bỏ chọn
// ════════════════════════════════════════════════════════════════
//
// 🎯 Visual feedback:
//   - Đã chọn: viền xanh 3dp + overlay icon ✅ góc trên
//   - Chưa chọn: viền trong suốt, background mờ theo category
// ═════════════════════════════════════════════════════════════════

@Composable
private fun SelectableItemCard(
    item: ClothingItemEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
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
            if (imageModel != null) {
                SubcomposeAsyncImage(
                    model = imageModel,
                    contentDescription = item.name.ifBlank { item.category },
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = { SelectableItemCardFallback(item, bgAlpha) },
                    error = { SelectableItemCardFallback(item, bgAlpha) }
                )
            } else {
                SelectableItemCardFallback(item, bgAlpha)
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = stringResource(R.string.selected_vn_content_desc),
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
            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete_item_content_desc),
                        tint = Color.White,
                        modifier = Modifier
                            .background(Color.Red.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// 🃏 ClothingItemCard — Card hiển thị một clothing item
// ═════════════════════════════════════════════════════════════════

@Composable
fun ClothingItemCard(
    item: ClothingItemEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val imageModel = rememberItemImageModel(item)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (imageModel != null) {
                SubcomposeAsyncImage(
                    model = imageModel,
                    contentDescription = item.name.ifBlank { item.category },
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = { ClothingItemCardFallback(item) },
                    error = { ClothingItemCardFallback(item) }
                )
            } else {
                ClothingItemCardFallback(item)
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
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete_item_content_desc),
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
        val data = resolveImageData(context, item.imageNoBg)
            ?: resolveImageData(context, item.imageOriginal)
        data?.let {
            ImageRequest.Builder(context)
                .data(it)
                .crossfade(false)
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
    "Tops" -> "T"
    "Bottoms" -> "B"
    "Dresses" -> "D"
    "Footwear" -> "F"
    "Bags" -> "Ba"
    "Accessories" -> "Ac"
    "Jewelry" -> "J"
    else -> "?"
}

private val sheetCategories = listOf("Tops", "Bottoms", "Dresses", "Footwear", "Bags", "Accessories", "Jewelry")

// ════════════════════════════════════════════════════════════════
// 📝 NewClothingItemSheet — Bottom sheet thêm item mới (giữ lại)
// ════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewClothingItemSheet(
    viewModel: ClothingViewModel,
    onItemAdded: () -> Unit,
    onError: (String) -> Unit
) {
    // ── Focus management ─────────────────────────────────────────
    // Mỗi field có FocusRequester riêng, bấm Enter/Done → nhảy field tiếp
    val focusManager = LocalFocusManager.current
    val categoryFocus = remember { FocusRequester() }
    val colorFocus = remember { FocusRequester() }
    val nameFocus = remember { FocusRequester() }
    val brandFocus = remember { FocusRequester() }
    val priceFocus = remember { FocusRequester() }

    val context = LocalContext.current
    val apiService = com.example.stylemate.network.RetrofitClient.stylemateApiService
    val imageProcessingRepository = ImageProcessingRepository(apiService, context)
    val imageProcessingViewModel: ImageProcessingViewModel = viewModel(
        factory = ImageProcessingViewModelFactory(imageProcessingRepository)
    )
    val aiFillState by imageProcessingViewModel.aiFillState.collectAsState()
    val autoTaggingState by imageProcessingViewModel.autoTaggingState.collectAsState()
    val removeBgState by imageProcessingViewModel.removeBgState.collectAsState()

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
    var noBgPath by remember { mutableStateOf<String?>(null) }

    val canRemoveBackground = imagePath?.let { current ->
        current.isNotBlank() && current != noBgPath
    } ?: false

    val onRemoveBackgroundClick = {
        val currentPath = imagePath
        if (currentPath.isNullOrBlank()) {
            onError(context.getString(R.string.please_select_image))
        } else {
            imageProcessingViewModel.removeBackground(currentPath)
        }
        Unit
    }

    LaunchedEffect(removeBgState.resultPath) {
        val newPath = removeBgState.resultPath
        if (!newPath.isNullOrBlank()) {
            imagePickerState.setImagePath(newPath)
            noBgPath = newPath
            imageProcessingViewModel.clearResult()
        }
    }

    LaunchedEffect(removeBgState.errorMessage) {
        val message = removeBgState.errorMessage
        if (!message.isNullOrBlank()) {
            onError(message)
            imageProcessingViewModel.clearResult()
        }
    }

    LaunchedEffect(aiFillState.suggestion) {
        val suggestion = aiFillState.suggestion
        if (suggestion != null) {
            suggestion.category?.let { category = it }
            suggestion.color?.let { color = it }
            suggestion.name?.let { itemName = it }
            imageProcessingViewModel.clearAiFillResult()
        }
    }

    LaunchedEffect(aiFillState.errorMessage) {
        val message = aiFillState.errorMessage
        if (!message.isNullOrBlank()) {
            onError(message)
            imageProcessingViewModel.clearAiFillResult()
        }
    }

    LaunchedEffect(autoTaggingState.suggestion) {
        val suggestion = autoTaggingState.suggestion
        if (suggestion != null) {
            suggestion.season?.let { selectedSeason = it }
            suggestion.occasion?.let { selectedOccasion = it }
            imageProcessingViewModel.clearAutoTaggingResult()
        }
    }

    LaunchedEffect(autoTaggingState.errorMessage) {
        val message = autoTaggingState.errorMessage
        if (!message.isNullOrBlank()) {
            onError(message)
            imageProcessingViewModel.clearAutoTaggingResult()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            stringResource(R.string.quick_add_item_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        ImagePickerSection(
            title = stringResource(R.string.item_image_label),
            imagePath = imagePath,
            onCameraClick = imagePickerState.onCameraClick,
            onGalleryClick = imagePickerState.onGalleryClick,
            titleStyle = MaterialTheme.typography.labelLarge,
            onRemoveBgClick = onRemoveBackgroundClick,
            isProcessing = removeBgState.isProcessing,
            canRemoveBg = canRemoveBackground
        )

        Spacer(Modifier.height(24.dp))
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                stringResource(R.string.quick_add_item_details),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = {
                        val currentPath = imagePath
                        if (currentPath.isNullOrBlank()) {
                            onError(context.getString(R.string.please_select_image_error))
                        } else {
                            imageProcessingViewModel.autoTagging(currentPath)
                        }
                    },
                    enabled = !autoTaggingState.isProcessing,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    if (autoTaggingState.isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.tagging_label))
                    } else {
                        Text(stringResource(R.string.auto_tagging_label), style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        val currentPath = imagePath
                        if (currentPath.isNullOrBlank()) {
                            onError(context.getString(R.string.please_select_image_error))
                        } else {
                            imageProcessingViewModel.fillWithAi(currentPath)
                        }
                    },
                    enabled = !aiFillState.isProcessing,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    if (aiFillState.isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.filling_label))
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.fill_with_ai_label), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // ── CATEGORY ─────────────────────────────────────────────
        Text(stringResource(R.string.category_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        ExposedDropdownMenuBox(
            expanded = expandedMenu,
            onExpandedChange = { expandedMenu = !expandedMenu }
        ) {
            OutlinedTextField(
                value = category,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.select_category_hint)) },
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

        // ── COLOR ───────────────────────────────────────────────
        Text(stringResource(R.string.color_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = color,
            onValueChange = { color = it },
            label = { Text(stringResource(R.string.color_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )

        Spacer(Modifier.height(12.dp))

        // ── ITEM NAME ────────────────────────────────────────────
        Text(stringResource(R.string.item_name_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = itemName,
            onValueChange = { itemName = it },
            label = { Text(stringResource(R.string.enter_item_name_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.item_name_placeholder)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )

        Spacer(Modifier.height(12.dp))

        // ── BRAND ────────────────────────────────────────────────
        Text(stringResource(R.string.brand_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = brand,
            onValueChange = { brand = it },
            label = { Text(stringResource(R.string.enter_brand_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.brand_placeholder_short)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
        )

        Spacer(Modifier.height(12.dp))

        // ── PRICE ───────────────────────────────────────────────
        Text(stringResource(R.string.price_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = price,
            onValueChange = { price = it },
            label = { Text(stringResource(R.string.enter_price_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.price_placeholder)) },
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
        Text(stringResource(R.string.season_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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

        // ─ OCCASION ─────────────────────────────────────────────
        Text(stringResource(R.string.occasion_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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

        // ── PURCHASE DATE ───────────────────────────────────────
        Text(stringResource(R.string.purchase_date_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
                        text = stringResource(R.string.tap_to_select_date),
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

        // ── ADD BUTTON ──────────────────────────────────────────
        Button(
            onClick = {
                if (category.isBlank() || color.isBlank()) return@Button
                val parsedPrice = price.toDoubleOrNull() ?: 0.0
                val imageFile = imagePath?.let { File(it) }
                val bgRemoved = imagePath != null && imagePath == noBgPath
                viewModel.addClothingItem(
                    imageFile = imageFile,
                    category = category,
                    color = color,
                    name = itemName,
                    season = selectedSeason,
                    occasion = selectedOccasion,
                    brand = brand,
                    purchaseDate = purchaseDate,
                    price = parsedPrice,
                    bgRemoved = bgRemoved
                )
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
                Text(stringResource(R.string.adding_item))
            } else {
                Text(stringResource(R.string.add_item))
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
                }) { Text(stringResource(R.string.select_date_button)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel_button)) }
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

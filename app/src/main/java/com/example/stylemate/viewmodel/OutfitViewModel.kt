package com.example.stylemate.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.stylemate.model.ClothingItemEntity
import com.example.stylemate.model.OutfitClothingCrossRef
import com.example.stylemate.model.OutfitEntity
import com.example.stylemate.model.OutfitWithClothingItems
import kotlin.math.min
import com.example.stylemate.repository.OutfitRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 🧩 OutfitViewModel — ViewModel cho module Phối đồ (Outfit).
 *
 * Quản lý 3 StateFlow chính:
 *   1. [outfits] — Danh sách toàn bộ Outfit (kèm ClothingItem bên trong).
 *   2. [draftOutfitItems] — Danh sách items tạm thời đang chọn để tạo outfit mới.
 *   3. [isLoading] — Trạng thái loading khi lưu outfit.
 *
 * 🔄 Luồng dữ liệu:
 *   UI (Compose) ← collect StateFlow ← OutfitViewModel ← OutfitRepository ← Retrofit ← Backend API
 */
class OutfitViewModel(
    private val repository: OutfitRepository
) : ViewModel() {

    companion object {
        private const val TAG = "OutfitViewModel"
    }

    // ─────────────────────────────────────────────────────────────
    // 🔄 Refresh Trigger
    // ─────────────────────────────────────────────────────────────

    /**
     * Trigger de force refresh danh sach outfits.
     * Sau khi tao/xoa outfit, tang gia tri nay -> flatMapLatest duoc kich hoat lai.
     */
    private val _refreshTrigger = MutableStateFlow(0L)

    // ─────────────────────────────────────────────────────────────
    // 🔷 Reactive State: Danh sách Outfit
    // ─────────────────────────────────────────────────────────────

    /**
     * Danh sách tất cả Outfit (kèm ClothingItem) dưới dạng [StateFlow].
     *
     * Su dung [flatMapLatest] tren [_refreshTrigger] de:
     *   - Lan dau subscribe -> goi API
     *   - Sau khi tao/xoa outfit -> _refreshTrigger++ -> goi API lai
     *
     * Nho [stateIn] voi [SharingStarted.WhileSubscribed], Flow chi hoat dong
     * khi co UI observer — tiet kiem tai nguyen, tranh memory leak.
     */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    @OptIn(FlowPreview::class)
    private val debouncedSearchQuery = _searchQuery
        .map { it.trim() }
        .debounce(250)
        .distinctUntilChanged()
        .onStart { emit("") }

    /**
     * Danh sách tất cả Outfit (kèm ClothingItem) dưới dạng [StateFlow].
     * 
     * ⚡ CẢI TIẾN DATA PIPELINE (06/2026):
     *   - flowOn(Dispatchers.Default) đảm bảo combine + flatMapLatest không block Main Thread
     *   - Repository.getAllOutfitsWithItems() đã có .flowOn(Dispatchers.IO)
     *   - stateIn với WhileSubscribed(5_000) — tự động stop khi không có UI observer
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val outfits: StateFlow<List<OutfitWithClothingItems>> = combine(
        _refreshTrigger,
        debouncedSearchQuery
    ) { _, query ->
        query
    }
        .flatMapLatest { query ->
            repository.getAllOutfitsWithItems(query)
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val filteredOutfits: StateFlow<List<OutfitWithClothingItems>> = outfits

    /**
     * 🔄 Goi khi mot clothing item bi xoa/sua o noi khac (vd: ClothingViewModel).
     * Xoa cache clothes + bump trigger de danh sach outfit (va preview canvas)
     * re-fetch voi du lieu tuoi, loai bo item da xoa.
     */
    fun refreshAfterItemChange() {
        repository.invalidateClothesCache()
        _refreshTrigger.value = System.currentTimeMillis()
    }

    // ─────────────────────────────────────────────────────────────
    // 🔷 Draft State: Danh sách items đang chọn để tạo outfit mới
    // ─────────────────────────────────────────────────────────────

    /**
     * 📝 draftOutfitItems — Danh sách tạm thời các ClothingItem mà người dùng
     * đang chọn/kéo thả để chuẩn bị lưu thành một Outfit mới.
     *
     * 🎯 Mục đích:
     *   - Cho phép người dùng build outfit từng bước (chọn items, xoay sắp xếp).
     *   - Chỉ khi nhấn "Save", dữ liệu mới được ghi xuống DB.
     *   - Nếu huỷ, draft bị clear mà không ảnh hưởng DB.
     *
     * 🔄 UI observe StateFlow này để hiển thị danh sách items đã chọn
     * (vd: trong một hàng ngang có thể kéo thả sắp xếp).
     */
    private val _draftOutfitItems = MutableStateFlow<List<ClothingItemEntity>>(emptyList())
    val draftOutfitItems: StateFlow<List<ClothingItemEntity>> = _draftOutfitItems

    /**
     * Số lượng items hiện có trong draft (dùng để hiện badge trên UI).
     */
    val draftCount: StateFlow<Int> = MutableStateFlow(0).also { flow ->
        // Cập nhật mỗi khi draft thay đổi
        viewModelScope.launch {
            _draftOutfitItems.collect { items ->
                (flow as MutableStateFlow).value = items.size
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 🔷 Loading State
    // ─────────────────────────────────────────────────────────────

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // ─────────────────────────────────────────────────────────────
    // 🔷 Error State
    // ─────────────────────────────────────────────────────────────

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _editingOutfitId = MutableStateFlow<String?>(null)
    val editingOutfitId: StateFlow<String?> = _editingOutfitId

    private val _editingOutfitName = MutableStateFlow("")
    val editingOutfitName: StateFlow<String> = _editingOutfitName

    data class OutfitItemPlacement(
        val item: ClothingItemEntity,
        val posX: Float,
        val posY: Float,
        val scale: Float
    )

    private val _editingItems = MutableStateFlow<List<OutfitItemPlacement>>(emptyList())
    val editingItems: StateFlow<List<OutfitItemPlacement>> = _editingItems

    private val _editSaveSuccess = MutableStateFlow(false)
    val editSaveSuccess: StateFlow<Boolean> = _editSaveSuccess

    // ═════════════════════════════════════════════════════════════
    // 🎯 HÀM THAO TÁC TRÊN DRAFT
    // ═════════════════════════════════════════════════════════════

    /**
     * ➕ Thêm một ClothingItem vào draft outfit.
     *
     * Kiểm tra trùng lặp dựa trên [ClothingItemEntity.id] — nếu item đã có
     * trong draft thì không thêm nữa (tránh duplicate).
     *
     * @param item ClothingItem cần thêm vào draft.
     */
    fun addClothingItemToDraft(item: ClothingItemEntity) {
        val currentDraft = _draftOutfitItems.value
        // Kiểm tra trùng lặp — nếu đã có thì bỏ qua
        if (currentDraft.any { it.id == item.id }) {
            Log.d(TAG, "⚠️ Item ${item.name} đã có trong draft, bỏ qua")
            return
        }
        _draftOutfitItems.value = currentDraft + item
        Log.d(TAG, "➕ Đã thêm '${item.name}' vào draft. Tổng: ${_draftOutfitItems.value.size}")
    }

    /**
     * ❌ Xoá một ClothingItem khỏi draft outfit.
     *
     * Dùng filter để loại bỏ item có id tương ứng.
     *
     * @param item ClothingItem cần xoá khỏi draft.
     */
    fun removeClothingItemFromDraft(item: ClothingItemEntity) {
        _draftOutfitItems.value = _draftOutfitItems.value.filter { it.id != item.id }
        Log.d(TAG, "➖ Đã xoá '${item.name}' khỏi draft. Còn lại: ${_draftOutfitItems.value.size}")
    }

    /**
     * 🧹 Xoá toàn bộ draft (reset về rỗng).
     * Gọi khi huỷ tạo outfit hoặc sau khi lưu thành công.
     */
    fun clearDraft() {
        _draftOutfitItems.value = emptyList()
        Log.d(TAG, "🧹 Đã xoá toàn bộ draft")
    }

    // ═════════════════════════════════════════════════════════════
    // 💾 HÀM LƯU OUTFIT XUỐNG DB
    // ═════════════════════════════════════════════════════════════

    /**
     * 💾 Lưu outfit mới từ draft xuống database.
     *
     * 📐 Quy trình:
     *   1. Validate: draft không được rỗng, name không được trống.
     *   2. Tạo [OutfitEntity] mới với UUID và tên do người dùng đặt.
     *   3. Tạo danh sách [OutfitClothingCrossRef] từ draft items.
     *   4. Ghi Outfit + CrossRef vào DB (cùng một Transaction ở DAO).
     *   5. Clear draft.
     *
     * @param name Tên của outfit (do người dùng nhập).
     */
    fun saveOutfit(name: String) {
        // ── Validate ─────────────────────────────────────────────
        if (name.isBlank()) {
            _errorMessage.value = "Please enter a name for the outfit"
            return
        }
        val draftItems = _draftOutfitItems.value
        if (draftItems.isEmpty()) {
            _errorMessage.value = "Please select at least one item"
            return
        }

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                // ── Bước 1: Tạo OutfitEntity ─────────────────────
                val outfitId = UUID.randomUUID().toString()
                val newOutfit = OutfitEntity(
                    id = outfitId,
                    name = name.trim(),
                    createdAt = System.currentTimeMillis()
                )

                Log.d(TAG, "👔 Đang lưu outfit: '$name' với ${draftItems.size} items")

                // ── Bước 2: Tạo danh sách CrossRef ───────────────
                val crossRefs = draftItems.mapIndexed { index, item ->
                    val (x, y) = defaultGridPosition(index)
                    OutfitClothingCrossRef(
                        outfitId = outfitId,
                        clothingItemId = item.id,
                        posX = x,
                        posY = y,
                        scale = 1f
                    )
                }

                // ── Bước 3: Ghi vào DB ───────────────────────────
                repository.insertOutfit(newOutfit)
                repository.insertOutfitClothingCrossRefs(crossRefs)

                Log.d(TAG, "✅ Đã lưu outfit '$name' thành công!")

                // ── Bước 4: Clear draft ──────────────────────────
                clearDraft()

                // 🔄 Refresh danh sach outfits
                _refreshTrigger.value = System.currentTimeMillis()

            } catch (e: Exception) {
                Log.e(TAG, "❌ Lỗi khi lưu outfit: ${e.message}", e)
                _errorMessage.value = "Cannot save outfit: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun startEditingOutfit(outfitId: String, outfitName: String, items: List<ClothingItemEntity>) {
        _editingOutfitId.value = outfitId
        _editingOutfitName.value = outfitName
        _editSaveSuccess.value = false
        // ⚡ Dùng vị trí/scale có sẵn trong clothingItems (canvasPosX/Y/Scale) — set NGAY LẬP TỨC,
        // không gọi API riêng (tránh nháy canvas cũ rồi mới load canvas hiện tại).
        _editingItems.value = mapClothingItemsWithDefaults(items)
    }

    private fun mapClothingItemsWithDefaults(items: List<ClothingItemEntity>): List<OutfitItemPlacement> {
        if (items.isEmpty()) return emptyList()
        val hasCustomPos = items.any { it.canvasPosX != 0f || it.canvasPosY != 0f }
        if (hasCustomPos) {
            return items.map { OutfitItemPlacement(it, it.canvasPosX, it.canvasPosY, it.canvasScale) }
        }
        return items.mapIndexed { index, item ->
            val (x, y) = defaultGridPosition(index)
            OutfitItemPlacement(item, x, y, item.canvasScale)
        }
    }

    private fun defaultGridPosition(index: Int): Pair<Float, Float> {
        val col = index % 2
        val row = index / 2
        val x = if (col == 0) 0.1f else 0.55f
        val y = min(0.1f + row * 0.25f, 0.8f)
        return x to y
    }

    fun updateEditingItemPosition(itemId: String, posX: Float, posY: Float) {
        _editingItems.value = _editingItems.value.map { placement ->
            if (placement.item.id == itemId) {
                placement.copy(posX = posX, posY = posY)
            } else placement
        }
    }

    fun updateEditingItemScale(itemId: String, scale: Float) {
        _editingItems.value = _editingItems.value.map { placement ->
            if (placement.item.id == itemId) {
                placement.copy(scale = scale)
            } else placement
        }
    }

    fun addItemsToEditing(items: List<ClothingItemEntity>) {
        val existingIds = _editingItems.value.map { it.item.id }.toSet()
        val newItems = items.filterNot { it.id in existingIds }
        if (newItems.isEmpty()) return
        val startIndex = _editingItems.value.size
        val appended = newItems.mapIndexed { index, item ->
            val (x, y) = defaultGridPosition(startIndex + index)
            OutfitItemPlacement(item, x, y, 1f)
        }
        _editingItems.value = _editingItems.value + appended
    }

    fun removeEditingItem(itemId: String) {
        _editingItems.value = _editingItems.value.filterNot { it.item.id == itemId }
    }

    fun saveEditingOutfit() {
        val outfitId = _editingOutfitId.value ?: return
        val items = _editingItems.value
        if (items.isEmpty()) {
            _errorMessage.value = "Please select at least one item"
            return
        }
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                repository.clearOutfitItems(outfitId)
                val crossRefs = items.map { placement ->
                    OutfitClothingCrossRef(
                        outfitId = outfitId,
                        clothingItemId = placement.item.id,
                        posX = placement.posX,
                        posY = placement.posY,
                        scale = placement.scale
                    )
                }
                repository.insertOutfitClothingCrossRefs(crossRefs)
                _editSaveSuccess.value = true
                // 🔄 Refresh danh sach outfits
                _refreshTrigger.value = System.currentTimeMillis()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Lỗi khi lưu outfit edit: ${e.message}", e)
                _errorMessage.value = "Cannot save outfit: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearEditSaveSuccess() {
        _editSaveSuccess.value = false
    }

    // ═════════════════════════════════════════════════════════════
    // ❌ HÀM XOÁ OUTFIT
    // ═════════════════════════════════════════════════════════════

    /**
     * ❌ Xoá một Outfit khỏi database.
     *
     * Nhờ CASCADE, các dòng CrossRef liên quan cũng tự động bị xoá.
     * UI sẽ tự động cập nhật nhờ [outfits] StateFlow.
     *
     * @param outfit Entity cần xoá.
     */
    fun deleteOutfit(outfit: OutfitEntity) {
        viewModelScope.launch {
            try {
                repository.deleteOutfit(outfit)
                // 🔄 Refresh danh sach outfits
                _refreshTrigger.value = System.currentTimeMillis()
                Log.d(TAG, "🗑️ Đã xoá outfit: ${outfit.name}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Lỗi khi xoá outfit: ${e.message}", e)
                _errorMessage.value = "Cannot delete outfit: ${e.message}"
            }
        }
    }

    // ═════════════════════════════════════════════════════════════
    // 🧹 HÀM CLEAR ERROR
    // ═════════════════════════════════════════════════════════════

    fun clearError() {
        _errorMessage.value = null
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearchQuery() {
        _searchQuery.value = ""
    }
}

/**
 * 🏭 OutfitViewModelFactory — Factory pattern để inject [OutfitRepository] vào ViewModel.
 *
 * Tuân thủ [ViewModelProvider.Factory] — cho phép truyền dependency
 * mà không cần Dagger/Hilt.
 *
 * @param repository Repository instance (sẽ được tạo từ AppDatabase trong Activity/Fragment).
 */
class OutfitViewModelFactory(
    private val repository: OutfitRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OutfitViewModel::class.java)) {
            return OutfitViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

package com.example.stylemate.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.stylemate.model.Categories
import com.example.stylemate.model.ClothingItemEntity
import com.example.stylemate.repository.ClothingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * 🧩 ClothingViewModel — ViewModel cho màn hình Quản lý tủ đồ.
 */
class ClothingViewModel(
    private val repository: ClothingRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ClothingViewModel"
    }

    private val _selectedCategory = MutableStateFlow(Categories.ALL)
    private val _refreshTrigger = MutableStateFlow(0L)

    val selectedCategory: StateFlow<String> = _selectedCategory

    /**
     * Tất cả items (không filter category) — dùng cho AddItems BottomSheet.
     * ⚡ Share subscription qua ViewModel để tránh infinite recomposition loop.
     */
    val allItems: StateFlow<List<ClothingItemEntity>> = repository.getAllItems()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val items: StateFlow<List<ClothingItemEntity>> = combine(
        _selectedCategory,
        _refreshTrigger
    ) { category, _ -> category }
        .flatMapLatest { category ->
            if (category == Categories.ALL) {
                repository.getAllItems()
            } else {
                repository.getItemsByCategory(category)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val debouncedSearchQuery = _searchQuery
        .map { it.trim() }
        .debounce(250)
        .distinctUntilChanged()
        .onStart { emit("") }

    val filteredItems: StateFlow<List<ClothingItemEntity>> = combine(
        items,
        debouncedSearchQuery
    ) { currentItems, query ->
        if (query.isBlank()) {
            currentItems
        } else {
            currentItems.filter { matchesSearchQuery(it, query) }
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    /**
     * Thêm một clothing item mới vào tủ đồ.
     */
    fun addClothingItem(
        imageFile: File,
        category: String,
        color: String,
        name: String,
        season: String,
        occasion: String,
        brand: String,
        purchaseDate: Long,
        price: Double
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                Log.d(TAG, "📸 Bắt đầu xử lý ảnh: ${imageFile.name}")

                // Đã gỡ bỏ logic tách nền (remove.bg)
                val imagePath = imageFile.absolutePath

                val newItem = ClothingItemEntity(
                    id = UUID.randomUUID().toString(),
                    imageOriginal = imagePath,
                    imageNoBg = "",
                    category = category,
                    color = color,
                    name = name,
                    season = season,
                    occasion = occasion,
                    brand = brand,
                    purchaseDate = purchaseDate,
                    price = price,
                    canvasPosX = 0.5f,
                    canvasPosY = 0.1f,
                    createdAt = System.currentTimeMillis()
                )

                repository.insertItem(newItem)
                _refreshTrigger.value = System.currentTimeMillis()

            } catch (e: Exception) {
                Log.e(TAG, "❌ Lỗi khi thêm item: ${e.message}", e)
                _errorMessage.value = "Không thể thêm item: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * ✏️ Cập nhật một item.
     */
    fun updateClothingItem(
        originalItem: ClothingItemEntity,
        updatedItem: ClothingItemEntity,
        newImageFile: File?
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val finalItem = if (newImageFile != null) {
                    val imagePath = newImageFile.absolutePath
                    updatedItem.copy(
                        imageOriginal = imagePath,
                        imageNoBg = ""
                    )
                } else {
                    updatedItem.copy(
                        imageOriginal = originalItem.imageOriginal,
                        imageNoBg = originalItem.imageNoBg
                    )
                }

                repository.updateItem(finalItem)
                _refreshTrigger.value = System.currentTimeMillis()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Lỗi khi cập nhật item: ${e.message}", e)
                _errorMessage.value = "Không thể cập nhật item: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteClothingItem(item: ClothingItemEntity) {
        viewModelScope.launch {
            try {
                repository.deleteItem(item)
                _refreshTrigger.value = System.currentTimeMillis()
                Log.d(TAG, "🗑️ Đã xoá item: ${item.id}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Lỗi khi xoá item: ${e.message}", e)
                _errorMessage.value = "Không thể xoá item: ${e.message}"
            }
        }
    }

    fun getItemCountByCategory(category: String): Flow<Int> {
        return if (category == Categories.ALL) {
            repository.getTotalItemCount()
        } else {
            repository.getItemCountByCategory(category)
        }
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearchQuery() {
        _searchQuery.value = ""
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun refreshItems() {
        _refreshTrigger.value = System.currentTimeMillis()
    }

    fun updateItemCanvasPosition(itemId: String, posX: Float, posY: Float) {
        viewModelScope.launch {
            try {
                repository.updateItemCanvasPosition(itemId, posX, posY)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Lỗi khi cập nhật vị trí item: ${e.message}", e)
                _errorMessage.value = "Không thể cập nhật vị trí item: ${e.message}"
            }
        }
    }

    private fun matchesSearchQuery(item: ClothingItemEntity, query: String): Boolean {
        val tokens = query.trim()
            .lowercase()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        if (tokens.isEmpty()) {
            return true
        }

        val searchableText = buildString {
            append(item.name)
            append(' ')
            append(item.color)
            append(' ')
            append(item.season)
            append(' ')
            append(item.occasion)
            append(' ')
            append(item.brand)
        }.lowercase()

        return tokens.all { searchableText.contains(it) }
    }
}

class ClothingViewModelFactory(
    private val repository: ClothingRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClothingViewModel::class.java)) {
            return ClothingViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

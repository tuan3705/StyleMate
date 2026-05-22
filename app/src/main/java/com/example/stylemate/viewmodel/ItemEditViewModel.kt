package com.example.stylemate.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.stylemate.model.ClothingItemEntity
import com.example.stylemate.model.OutfitWithClothingItems
import com.example.stylemate.repository.ClothingRepository
import com.example.stylemate.repository.OutfitRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 🧩 ItemEditViewModel — ViewModel cho màn hình chỉnh sửa item.
 *
 * Tuân thủ MVVM:
 * - UI quan sát StateFlow [uiState] + [relevantOutfits]
 * - ViewModel xử lý async với viewModelScope
 */
class ItemEditViewModel(
    private val clothingRepository: ClothingRepository,
    private val outfitRepository: OutfitRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ItemEditViewModel"
    }

    data class ItemEditUiState(
        val isLoading: Boolean = false,
        val isSaving: Boolean = false,
        val errorMessage: String? = null,
        val itemId: String = "",
        val imageOriginal: String = "",
        val imageNoBg: String = "",
        val category: String = "",
        val color: String = "",
        val name: String = "",
        val season: String = "",
        val occasion: String = "",
        val brand: String = "",
        val purchaseDate: Long = System.currentTimeMillis(),
        val price: String = "",
        val newImagePath: String? = null
    )

    private val _uiState = MutableStateFlow(ItemEditUiState())
    val uiState: StateFlow<ItemEditUiState> = _uiState

    private val _itemId = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val relevantOutfits: StateFlow<List<OutfitWithClothingItems>> = _itemId
        .flatMapLatest { itemId ->
            if (itemId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                outfitRepository.getOutfitsContainingItem(itemId)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess

    private var originalItem: ClothingItemEntity? = null

    fun loadItem(itemId: String) {
        if (_uiState.value.itemId == itemId && originalItem != null) return
        _itemId.value = itemId
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                val item = clothingRepository.getItemById(itemId)
                if (item == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Không tìm thấy item để chỉnh sửa"
                    )
                    return@launch
                }
                originalItem = item
                _uiState.value = ItemEditUiState(
                    isLoading = false,
                    itemId = item.id,
                    imageOriginal = item.imageOriginal,
                    imageNoBg = item.imageNoBg,
                    category = item.category,
                    color = item.color,
                    name = item.name,
                    season = item.season,
                    occasion = item.occasion,
                    brand = item.brand,
                    purchaseDate = item.purchaseDate,
                    price = if (item.price == 0.0) "" else item.price.toString()
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Lỗi khi load item: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Không thể tải item: ${e.message}"
                )
            }
        }
    }

    fun updateCategory(value: String) = updateState { copy(category = value) }
    fun updateColor(value: String) = updateState { copy(color = value) }
    fun updateName(value: String) = updateState { copy(name = value) }
    fun updateSeason(value: String) = updateState { copy(season = value) }
    fun updateOccasion(value: String) = updateState { copy(occasion = value) }
    fun updateBrand(value: String) = updateState { copy(brand = value) }
    fun updatePurchaseDate(value: Long) = updateState { copy(purchaseDate = value) }
    fun updatePrice(value: String) = updateState { copy(price = value) }
    fun updateImagePath(value: String?) = updateState { copy(newImagePath = value) }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearSaveSuccess() {
        _saveSuccess.value = false
    }

    fun saveChanges() {
        val current = _uiState.value
        val original = originalItem ?: return
        viewModelScope.launch {
            try {
                _uiState.value = current.copy(isSaving = true, errorMessage = null)

                val parsedPrice = current.price.toDoubleOrNull() ?: 0.0
                val baseItem = original.copy(
                    category = current.category,
                    color = current.color,
                    name = current.name,
                    season = current.season,
                    occasion = current.occasion,
                    brand = current.brand,
                    purchaseDate = current.purchaseDate,
                    price = parsedPrice
                )

                val newImageFile = current.newImagePath?.let { File(it) }
                val finalItem = if (newImageFile != null) {
                    val (noBgFile, removeBgError) = clothingRepository.removeBackground(newImageFile)
                    if (removeBgError != null) {
                        _uiState.value = _uiState.value.copy(errorMessage = removeBgError)
                    }
                    baseItem.copy(
                        imageOriginal = newImageFile.absolutePath,
                        imageNoBg = noBgFile?.absolutePath ?: newImageFile.absolutePath
                    )
                } else {
                    baseItem.copy(
                        imageOriginal = original.imageOriginal,
                        imageNoBg = original.imageNoBg
                    )
                }

                withContext(Dispatchers.IO) {
                    clothingRepository.updateItem(finalItem)
                }

                originalItem = finalItem
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    imageOriginal = finalItem.imageOriginal,
                    imageNoBg = finalItem.imageNoBg,
                    newImagePath = null
                )
                _saveSuccess.value = true
            } catch (e: Exception) {
                Log.e(TAG, "❌ Lỗi khi lưu item: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "Không thể cập nhật item: ${e.message}"
                )
            }
        }
    }

    private fun updateState(block: ItemEditUiState.() -> ItemEditUiState) {
        _uiState.value = _uiState.value.block()
    }
}

/**
 * 🏭 ItemEditViewModelFactory — Factory để inject [ClothingRepository] + [OutfitRepository].
 */
class ItemEditViewModelFactory(
    private val clothingRepository: ClothingRepository,
    private val outfitRepository: OutfitRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ItemEditViewModel::class.java)) {
            return ItemEditViewModel(clothingRepository, outfitRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

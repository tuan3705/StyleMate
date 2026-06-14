package com.example.stylemate.ui.screens.style_assess

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stylemate.data.MockDataProvider
import com.example.stylemate.data.models.ClosetItem
import com.example.stylemate.data.models.StyleAssessmentResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StyleAssessViewModel : ViewModel() {

    private val _selectedItems = MutableStateFlow<List<ClosetItem>>(emptyList())
    val selectedItems: StateFlow<List<ClosetItem>> = _selectedItems.asStateFlow()

    private val _uiState = MutableStateFlow<StyleAssessUiState>(StyleAssessUiState.Idle)
    val uiState: StateFlow<StyleAssessUiState> = _uiState.asStateFlow()

    fun toggleItemSelection(item: ClosetItem) {
        val current = _selectedItems.value.toMutableList()
        if (current.any { it.id == item.id }) {
            current.removeAll { it.id == item.id }
        } else if (current.size < 5) {
            current.add(item)
        }
        _selectedItems.value = current
    }

    fun removeItem(item: ClosetItem) {
        _selectedItems.value = _selectedItems.value.filter { it.id != item.id }
    }

    fun runAssessment() {
        if (_selectedItems.value.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = StyleAssessUiState.Loading
            try {
                // Mock data moved to MockDataProvider - no hardcoded strings in ViewModel
                kotlinx.coroutines.delay(2000)
                val mockResult = MockDataProvider.StyleAssessment.DEFAULT_MOCK
                _uiState.value = StyleAssessUiState.Success(mockResult)
            } catch (e: Exception) {
                _uiState.value = StyleAssessUiState.Error(
                    e.message ?: "style_assess_error_generic"
                )
            }
        }
    }

    fun reset() {
        _uiState.value = StyleAssessUiState.Idle
        _selectedItems.value = emptyList()
    }
}

sealed class StyleAssessUiState {
    object Idle : StyleAssessUiState()
    object Loading : StyleAssessUiState()
    data class Success(val result: StyleAssessmentResult) : StyleAssessUiState()
    data class Error(val message: String) : StyleAssessUiState()
}
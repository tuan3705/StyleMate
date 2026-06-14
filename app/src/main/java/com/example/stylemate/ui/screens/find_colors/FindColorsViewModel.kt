package com.example.stylemate.ui.screens.find_colors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stylemate.data.MockDataProvider
import com.example.stylemate.data.models.ColorPalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class FindColorsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<ColorUiState>(ColorUiState.Intro)
    val uiState: StateFlow<ColorUiState> = _uiState.asStateFlow()

    fun startCapture() {
        _uiState.value = ColorUiState.Capture
    }

    fun analyze() {
        viewModelScope.launch {
            _uiState.value = ColorUiState.Analyzing
            delay(2000)

            // Mock data moved to MockDataProvider - no hardcoded strings in ViewModel
            val mockPalette = MockDataProvider.ColorAnalysis.DEFAULT_MOCK
            _uiState.value = ColorUiState.Result(mockPalette)
        }
    }

    fun reset() {
        _uiState.value = ColorUiState.Intro
    }
}

sealed class ColorUiState {
    object Intro : ColorUiState()
    object Capture : ColorUiState()
    object Analyzing : ColorUiState()
    data class Result(val palette: ColorPalette) : ColorUiState()
}
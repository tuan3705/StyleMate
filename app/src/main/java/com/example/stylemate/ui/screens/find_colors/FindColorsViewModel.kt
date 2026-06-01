package com.example.stylemate.ui.screens.find_colors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
            
            val mockPalette = ColorPalette(
                season = "Spring Warm",
                palette = listOf("#FAD9C1", "#FFDD88", "#99E0A7"),
                avoid = listOf("#4B5563", "#0A0A0A"),
                confidence = 0.85,
                description = "You have warm undertones. These bright, clear colors will make your skin glow."
            )
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

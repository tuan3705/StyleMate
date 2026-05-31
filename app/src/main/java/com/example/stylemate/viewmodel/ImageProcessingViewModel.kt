package com.example.stylemate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.stylemate.repository.ImageProcessingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ImageProcessingViewModel(
    private val repository: ImageProcessingRepository
) : ViewModel() {

    data class RemoveBgUiState(
        val isProcessing: Boolean = false,
        val errorMessage: String? = null,
        val resultPath: String? = null
    )

    data class AiFillUiState(
        val isProcessing: Boolean = false,
        val errorMessage: String? = null,
        val suggestion: com.example.stylemate.network.AiFillSuggestionDto? = null
    )

    private val _removeBgState = MutableStateFlow(RemoveBgUiState())
    val removeBgState: StateFlow<RemoveBgUiState> = _removeBgState

    private val _aiFillState = MutableStateFlow(AiFillUiState())
    val aiFillState: StateFlow<AiFillUiState> = _aiFillState

    fun removeBackground(currentPath: String) {
        if (currentPath.isBlank()) return
        viewModelScope.launch {
            _removeBgState.value = RemoveBgUiState(isProcessing = true)
            val result = repository.removeBackground(currentPath)
            _removeBgState.value = if (result.isSuccess) {
                RemoveBgUiState(resultPath = result.getOrNull())
            } else {
                RemoveBgUiState(errorMessage = result.exceptionOrNull()?.message)
            }
        }
    }

    fun clearResult() {
        val current = _removeBgState.value
        if (current.resultPath != null || current.errorMessage != null) {
            _removeBgState.value = current.copy(resultPath = null, errorMessage = null)
        }
    }

    fun fillWithAi(currentPath: String) {
        if (currentPath.isBlank()) return
        viewModelScope.launch {
            _aiFillState.value = AiFillUiState(isProcessing = true)
            val result = repository.fillWithAi(currentPath)
            _aiFillState.value = if (result.isSuccess) {
                AiFillUiState(suggestion = result.getOrNull())
            } else {
                AiFillUiState(errorMessage = result.exceptionOrNull()?.message)
            }
        }
    }

    fun clearAiFillResult() {
        val current = _aiFillState.value
        if (current.suggestion != null || current.errorMessage != null) {
            _aiFillState.value = current.copy(suggestion = null, errorMessage = null)
        }
    }
}

class ImageProcessingViewModelFactory(
    private val repository: ImageProcessingRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ImageProcessingViewModel::class.java)) {
            return ImageProcessingViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}


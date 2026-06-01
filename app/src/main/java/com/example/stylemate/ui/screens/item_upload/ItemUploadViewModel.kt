package com.example.stylemate.ui.screens.item_upload

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ItemUploadViewModel : ViewModel() {

    private val _currentStep = MutableStateFlow(UploadStep.SELECT_IMAGE)
    val currentStep: StateFlow<UploadStep> = _currentStep.asStateFlow()

    fun nextStep() {
        val next = when (_currentStep.value) {
            UploadStep.SELECT_IMAGE -> UploadStep.MASK_EDITOR
            UploadStep.MASK_EDITOR -> UploadStep.REVIEW_METADATA
            UploadStep.REVIEW_METADATA -> UploadStep.SELECT_IMAGE
        }
        _currentStep.value = next
    }

    fun previousStep() {
        val prev = when (_currentStep.value) {
            UploadStep.SELECT_IMAGE -> UploadStep.SELECT_IMAGE
            UploadStep.MASK_EDITOR -> UploadStep.SELECT_IMAGE
            UploadStep.REVIEW_METADATA -> UploadStep.MASK_EDITOR
        }
        _currentStep.value = prev
    }
}

enum class UploadStep {
    SELECT_IMAGE, MASK_EDITOR, REVIEW_METADATA
}

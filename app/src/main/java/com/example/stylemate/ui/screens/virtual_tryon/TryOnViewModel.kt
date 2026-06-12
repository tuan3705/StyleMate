package com.example.stylemate.ui.screens.virtual_tryon

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stylemate.data.models.ProcessingJob
import com.example.stylemate.data.models.JobStatus
import com.example.stylemate.model.OutfitWithClothingItems
import com.example.stylemate.network.RetrofitClient
import com.example.stylemate.repository.OutfitRepository
import com.example.stylemate.repository.TryOnRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TryOnViewModel(application: Application) : AndroidViewModel(application) {

    private val tryOnRepository = TryOnRepository(application)
    val outfitRepository = OutfitRepository(RetrofitClient.stylemateApiService)

    private val _jobState = MutableStateFlow<ProcessingJob?>(null)
    val jobState: StateFlow<ProcessingJob?> = _jobState.asStateFlow()

    private val _selectedOutfit = MutableStateFlow<OutfitWithClothingItems?>(null)
    val selectedOutfit: StateFlow<OutfitWithClothingItems?> = _selectedOutfit.asStateFlow()

    fun selectOutfit(outfit: OutfitWithClothingItems?) {
        _selectedOutfit.value = outfit
    }

    fun startTryOn(bodyImageUri: Uri) {
        val outfit = _selectedOutfit.value ?: return
        val selectedItemIds = outfit.clothingItems.map { it.id }
        if (selectedItemIds.isEmpty()) return

        viewModelScope.launch {
            _jobState.value = ProcessingJob(
                jobId = "",
                userId = "",
                type = "virtual-tryon",
                status = JobStatus.QUEUED,
                progress = 0
            )

            val result = tryOnRepository.performTryOnWithItemIds(
                bodyImageUri = bodyImageUri,
                selectedItemIds = selectedItemIds,
                onProgress = { job ->
                    _jobState.value = job
                }
            )

            result.fold(
                onSuccess = { imageUrl ->
                    _jobState.value = _jobState.value?.copy(
                        status = JobStatus.COMPLETED,
                        progress = 100,
                        resultUrls = listOf(imageUrl)
                    )
                },
                onFailure = { error ->
                    _jobState.value = _jobState.value?.copy(
                        status = JobStatus.FAILED,
                        error = error.message ?: "Try-on failed"
                    )
                }
            )
        }
    }

    fun reset() {
        _jobState.value = null
        _selectedOutfit.value = null
    }
}
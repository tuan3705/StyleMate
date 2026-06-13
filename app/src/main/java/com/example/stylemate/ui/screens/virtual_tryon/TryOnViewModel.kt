package com.example.stylemate.ui.screens.virtual_tryon

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stylemate.data.models.ProcessingJob
import com.example.stylemate.data.models.JobStatus
import com.example.stylemate.model.ClothingItemEntity
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
    val clothingRepository = com.example.stylemate.repository.ClothingRepository(
        RetrofitClient.stylemateApiService,
        application
    )

    private val _jobState = MutableStateFlow<ProcessingJob?>(null)
    val jobState: StateFlow<ProcessingJob?> = _jobState.asStateFlow()

    // 🆕 Chọn item riêng lẻ thay vì outfit (try-on chỉ hỗ trợ 1 item)
    private val _selectedItem = MutableStateFlow<ClothingItemEntity?>(null)
    val selectedItem: StateFlow<ClothingItemEntity?> = _selectedItem.asStateFlow()

    fun selectItem(item: ClothingItemEntity?) {
        _selectedItem.value = item
    }

    fun startTryOn(bodyImageUri: Uri) {
        val item = _selectedItem.value ?: return
        val selectedItemIds = listOf(item.id)
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

            // ⚡ Phòng trường hợp onProgress chưa kịp gửi COMPLETED (race condition)
            result.fold(
                onSuccess = { imageUrl ->
                    val current = _jobState.value
                    if (current == null || current.status != JobStatus.COMPLETED) {
                        _jobState.value = ProcessingJob(
                            jobId = current?.jobId ?: "",
                            userId = "",
                            type = "virtual-tryon",
                            status = JobStatus.COMPLETED,
                            progress = 100,
                            resultUrls = listOf(imageUrl)
                        )
                    }
                },
                onFailure = { error ->
                    if (_jobState.value?.status != JobStatus.COMPLETED) {
                        _jobState.value = _jobState.value?.copy(
                            status = JobStatus.FAILED,
                            error = error.message ?: "Try-on failed"
                        ) ?: ProcessingJob(
                            jobId = "",
                            userId = "",
                            type = "virtual-tryon",
                            status = JobStatus.FAILED,
                            progress = 0,
                            error = error.message ?: "Try-on failed"
                        )
                    }
                }
            )
        }
    }

    fun reset() {
        _jobState.value = null
        _selectedItem.value = null
    }
}
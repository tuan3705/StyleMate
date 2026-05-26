package com.example.stylemate.ui.screens.virtual_tryon

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stylemate.data.models.ProcessingJob
import com.example.stylemate.data.models.JobStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class TryOnViewModel : ViewModel() {

    private val _jobState = MutableStateFlow<ProcessingJob?>(null)
    val jobState: StateFlow<ProcessingJob?> = _jobState.asStateFlow()

    fun startTryOn(bodyImageUri: String, itemId: String) {
        viewModelScope.launch {
            val jobId = "job_${System.currentTimeMillis()}"
            _jobState.value = ProcessingJob(
                jobId = jobId,
                userId = "u123",
                type = "virtual-tryon",
                status = JobStatus.QUEUED,
                progress = 0
            )

            // Simulate progress
            delay(1000)
            _jobState.value = _jobState.value?.copy(status = JobStatus.IN_PROGRESS, progress = 20)
            
            delay(2000)
            _jobState.value = _jobState.value?.copy(progress = 60)
            
            delay(2000)
            _jobState.value = _jobState.value?.copy(
                status = JobStatus.COMPLETED, 
                progress = 100,
                resultUrls = listOf("https://via.placeholder.com/600x800")
            )
        }
    }

    fun reset() {
        _jobState.value = null
    }
}

package com.example.stylemate.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.stylemate.model.CalendarEventEntity
import com.example.stylemate.model.OutfitWithClothingItems
import com.example.stylemate.repository.CalendarRepository
import com.example.stylemate.repository.OutfitRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID

// Error codes - UI maps via stringResource
enum class ErrorReason { NETWORK, DATABASE }

sealed class CalendarError {
    data class AssignFailed(val reason: ErrorReason) : CalendarError()
    data class RemoveFailed(val reason: ErrorReason) : CalendarError()
}

data class CalendarUiState(
    val isLoading: Boolean = false,
    val error: CalendarError? = null,
    val eventsInMonth: Set<Long> = emptySet(),
    val selectedDate: Long = 0L,
    val assignedOutfit: OutfitWithClothingItems? = null,
    val allOutfits: List<OutfitWithClothingItems> = emptyList(),
    val eventForSelectedDate: CalendarEventEntity? = null
)

/**
 * CalendarViewModel (minSdk 25 compatible)
 *
 * Uses java.util.Calendar instead of java.time to avoid desugaring complexity.
 * UI only collects uiState: StateFlow<CalendarUiState>
 */
class CalendarViewModel(
    private val calendarRepository: CalendarRepository,
    private val outfitRepository: OutfitRepository
) : ViewModel() {

    companion object {
        private const val TAG = "CalendarViewModel"
    }

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val _monthTrigger = MutableStateFlow(todayEpochMidnight())

    init {
        _uiState.value = _uiState.value.copy(selectedDate = todayEpochMidnight())

        val dateFlow = MutableStateFlow(_uiState.value.selectedDate)
        viewModelScope.launch {
            _uiState.collect { dateFlow.value = it.selectedDate }
        }

        @OptIn(ExperimentalCoroutinesApi::class)
        val eventFlow = dateFlow
            .flatMapLatest { date -> calendarRepository.observeEventByDate(date) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

        val outfitsFlow = outfitRepository.getAllOutfitsWithItems()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        @OptIn(ExperimentalCoroutinesApi::class)
        val monthFlow = _monthTrigger
            .flatMapLatest { epoch ->
                val (start, end) = monthRange(epoch)
                calendarRepository.getEventsBetween(start, end)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        viewModelScope.launch {
            combine(eventFlow, outfitsFlow, monthFlow) { event, outfits, monthEvents ->
                _uiState.value.copy(
                    eventForSelectedDate = event,
                    allOutfits = outfits,
                    assignedOutfit = event?.let { ev -> outfits.find { it.outfit.id == ev.outfitId } },
                    eventsInMonth = monthEvents.map { it.date }.toSet()
                )
            }.collect { newState -> _uiState.value = newState }
        }
    }

    fun selectDate(date: Long) {
        _uiState.value = _uiState.value.copy(selectedDate = date, error = null)
        Log.d(TAG, "Chon ngay: $date")
    }

    fun loadEventsInMonth(currentMonthEpoch: Long) {
        _monthTrigger.value = currentMonthEpoch
    }

    fun assignOutfitToSelectedDate(outfitId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val event = CalendarEventEntity(
                    id = UUID.randomUUID().toString(),
                    date = _uiState.value.selectedDate,
                    outfitId = outfitId
                )
                calendarRepository.assignOutfitToDate(event)
            } catch (e: Exception) {
                Log.e(TAG, "Loi gan outfit: ${e.message}", e)
                _uiState.value = _uiState.value.copy(error = CalendarError.AssignFailed(mapError(e)))
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun removeOutfitFromSelectedDate() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val event = _uiState.value.eventForSelectedDate ?: return@launch
                calendarRepository.removeEvent(event)
            } catch (e: Exception) {
                Log.e(TAG, "Loi xoa outfit: ${e.message}", e)
                _uiState.value = _uiState.value.copy(error = CalendarError.RemoveFailed(mapError(e)))
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // -- Helpers (minSdk 25 compatible, no java.time) --

    private fun mapError(e: Exception): ErrorReason = when (e) {
        is java.net.SocketTimeoutException,
        is java.net.UnknownHostException,
        is java.net.ConnectException -> ErrorReason.NETWORK
        else -> ErrorReason.DATABASE
    }

    private fun todayEpochMidnight(): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /** Returns (startOfMonth, endOfMonth) epoch millis */
    private fun monthRange(epochMillis: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = epochMillis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        val end = cal.timeInMillis
        return start to end
    }
}

class CalendarViewModelFactory(
    private val calendarRepository: CalendarRepository,
    private val outfitRepository: OutfitRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            return CalendarViewModel(calendarRepository, outfitRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
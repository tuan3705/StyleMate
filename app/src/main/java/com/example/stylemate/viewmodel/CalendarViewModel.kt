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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
 * ⚡ Fix hiệu năng:
 * - Cache events trong tháng để tránh gọi API lại khi chọn ngày khác
 * - Chỉ fetch API thực sự khi: load tháng mới, gán/xoá outfit
 * - selectDate() dùng cache, không gọi API
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

    // Cache events trong tháng theo epoch
    private var monthCache: Map<Long, CalendarEventEntity> = emptyMap()
    private var cachedMonthEpoch: Long = 0L
    private var cachedOutfits: List<OutfitWithClothingItems> = emptyList()

    init {
        val today = todayEpochMidnight()
        _uiState.value = _uiState.value.copy(selectedDate = today)

        // Load outfits ngay khi khởi tạo
        loadOutfits()
        // Load tháng hiện tại
        loadMonthEvents(today)
    }

    fun selectDate(date: Long) {
        _uiState.value = _uiState.value.copy(selectedDate = date, error = null)

        // ⚡ Dùng cache: tìm event trong monthCache
        val event = monthCache[date]
        val outfit = event?.let { ev ->
            cachedOutfits.find { it.outfit.id == ev.outfitId }
        }

        _uiState.value = _uiState.value.copy(
            eventForSelectedDate = event,
            assignedOutfit = outfit
        )
        Log.d(TAG, "Chon ngay: $date (from cache: ${event != null})")
    }

    fun loadEventsInMonth(currentMonthEpoch: Long) {
        if (cachedMonthEpoch != currentMonthEpoch) {
            cachedMonthEpoch = currentMonthEpoch
            loadMonthEvents(currentMonthEpoch)
        }
    }

    fun refreshOutfits() {
        loadOutfits()
    }

    fun refreshCalendarData() {
        // Refresh cả month cache và outfits
        loadOutfits()
        loadMonthEvents(_uiState.value.selectedDate)
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
                // ⚡ Update cache ngay lập tức, không cần đợi API
                monthCache = monthCache + (event.date to event)
                _uiState.value = _uiState.value.copy(
                    eventForSelectedDate = event,
                    assignedOutfit = cachedOutfits.find { it.outfit.id == outfitId },
                    eventsInMonth = monthCache.keys
                )
                // Refresh đồng bộ background (UI đã có data ngay)
                refreshCalendarData()
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
                // ⚡ Update cache ngay lập tức
                monthCache = monthCache - event.date
                _uiState.value = _uiState.value.copy(
                    eventForSelectedDate = null,
                    assignedOutfit = null,
                    eventsInMonth = monthCache.keys
                )
                // Refresh đồng bộ background
                refreshCalendarData()
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

    // ── Private helpers ──────────────────────────────────────────

    private fun loadOutfits() {
        viewModelScope.launch {
            try {
                val response = outfitRepository.getAllOutfitsWithItems()
                response.first().let { outfits ->
                    cachedOutfits = outfits
                    // Re-calculate assignedOutfit với selectedDate hiện tại
                    val currentEvent = monthCache[_uiState.value.selectedDate]
                    _uiState.value = _uiState.value.copy(
                        allOutfits = outfits,
                        assignedOutfit = currentEvent?.let { ev ->
                            outfits.find { it.outfit.id == ev.outfitId }
                        }
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "loadOutfits loi: ${e.message}")
            }
        }
    }

    private fun loadMonthEvents(currentMonthEpoch: Long) {
        viewModelScope.launch {
            try {
                val (start, end) = monthRange(currentMonthEpoch)
                val response = calendarRepository.getEventsBetween(start, end)
                response.first().let { events ->
                    monthCache = events.associateBy { it.date }
                    val currentEvent = monthCache[_uiState.value.selectedDate]
                    _uiState.value = _uiState.value.copy(
                        eventForSelectedDate = currentEvent,
                        eventsInMonth = monthCache.keys,
                        assignedOutfit = currentEvent?.let { ev ->
                            cachedOutfits.find { it.outfit.id == ev.outfitId }
                        }
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "loadMonthEvents loi: ${e.message}")
            }
        }
    }

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
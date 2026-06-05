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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID

/**
 * 🧩 CalendarViewModel — ViewModel cho màn hình Lịch (Calendar).
 *
 * Quản lý các StateFlow chính:
 *   1. [selectedDate] — Ngày đang được chọn trên lịch (epoch midnight).
 *   2. [eventForSelectedDate] — Sự kiện (nếu có) cho ngày đang chọn.
 *   3. [allOutfits] — Danh sách tất cả Outfit để chọn gán.
 *   4. [eventsInMonth] — Tập hợp các ngày có sự kiện trong tháng hiện tại.
 *   5. [assignedOutfit] — Outfit đã gán cho ngày đang chọn.
 *   6. [isLoading] — Trạng thái loading.
 *   7. [errorMessage] — Thông báo lỗi.
 *
 * 🔄 Luồng dữ liệu:
 *   UI ← collect StateFlow ← CalendarViewModel ← CalendarRepository + OutfitRepository
 */
class CalendarViewModel(
    private val calendarRepository: CalendarRepository,
    private val outfitRepository: OutfitRepository
) : ViewModel() {

    companion object {
        private const val TAG = "CalendarViewModel"
    }

    // ──────────────────────────────────────────────────────────────
    // 🔷 State: Ngày được chọn
    // ──────────────────────────────────────────────────────────────

    /**
     * Ngày đang được chọn trên lịch, lưu dưới dạng epoch midnight (UTC).
     * Khởi tạo bằng ngày hôm nay.
     */
    private val _selectedDate = MutableStateFlow(todayEpochMidnight())
    val selectedDate: StateFlow<Long> = _selectedDate

    // ──────────────────────────────────────────────────────────────
    // 🔷 Refresh Trigger
    // ──────────────────────────────────────────────────────────────

    /**
     * 🔄 Trigger để force refresh eventForSelectedDate.
     * Mỗi lần gán/xoá outfit, ta tăng giá trị này → flatMapLatest được kích hoạt lại.
     */
    private val _refreshTrigger = MutableStateFlow(0L)

    // ──────────────────────────────────────────────────────────────
    // 🔷 State: Sự kiện cho ngày đang chọn (reactive)
    // ──────────────────────────────────────────────────────────────

    /**
     * Sự kiện lịch của ngày đang chọn.
     *
     * Sử dụng [flatMapLatest trên cả [_selectedDate] và [_refreshTrigger] để:
     *   - Khi đổi ngày → tự động query lại
     *   - Khi gán/xoá outfit → force query lại mà không cần đổi ngày
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val eventForSelectedDate: StateFlow<CalendarEventEntity?> = combine(
        _selectedDate,
        _refreshTrigger
    ) { date, _ ->
        date
    }
        .flatMapLatest { date ->
            calendarRepository.observeEventByDate(date)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    // ──────────────────────────────────────────────────────────────
    // 🔷 State: Tất cả Outfit (để chọn gán vào ngày)
    // ──────────────────────────────────────────────────────────────

    /**
     * Danh sách tất cả Outfit kèm ClothingItem — dùng cho BottomSheet
     * chọn bộ đồ để gán vào ngày đã chọn.
     */
    val allOutfits: StateFlow<List<OutfitWithClothingItems>> =
        outfitRepository.getAllOutfitsWithItems()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    // ──────────────────────────────────────────────────────────────
    // 🔷 State: Outfit đã gán cho ngày đang chọn (để hiển thị)
    // ──────────────────────────────────────────────────────────────

    /**
     * Outfit (kèm ClothingItem) đã được gán cho ngày đang chọn.
     * null nếu chưa có outfit nào được gán.
     *
     * 🐛 FIX: Sử dụng combine() thay vì .map{} để lắng nghe cả 2 flow cùng lúc,
     * tránh race condition khi eventForSelectedDate thay đổi nhưng allOutfits chưa kịp cập nhật.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val assignedOutfit: StateFlow<OutfitWithClothingItems?> = combine(
        eventForSelectedDate,
        allOutfits,
        _refreshTrigger
    ) { event, outfits, _ ->
            if (event != null) {
            outfits.find { it.outfit.id == event.outfitId }
            } else null
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    // ──────────────────────────────────────────────────────────────
    // 🔷 State: Các ngày có sự kiện trong tháng hiện tại
    // ──────────────────────────────────────────────────────────────

    /**
     * Tập hợp các epoch midnight của những ngày có sự kiện trong tháng hiện tại.
     * Dùng để hiển thị chấm tròn trên lịch tháng.
     */
    private val _eventsInMonth = MutableStateFlow<Set<Long>>(emptySet())
    val eventsInMonth: StateFlow<Set<Long>> = _eventsInMonth
    /**
     * Load danh sách ngày có sự kiện trong tháng của [currentMonth].
     * Được gọi từ UI khi tháng thay đổi.
     */
    fun loadEventsInMonth(currentMonthEpoch: Long) {
        viewModelScope.launch {
            try {
                val cal = epochToCalendar(currentMonthEpoch)
                // Ngày đầu tháng
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val startOfMonth = cal.timeInMillis

                // Ngày cuối tháng
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                val endOfMonth = cal.timeInMillis

                calendarRepository.getEventsBetween(startOfMonth, endOfMonth)
                    .collect { events ->
                        _eventsInMonth.value = events.map { it.date }.toSet()
                    }
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ loadEventsInMonth() lỗi: ${e.message}")
                _eventsInMonth.value = emptySet()
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // 🔷 State: Loading & Error
    // ──────────────────────────────────────────────────────────────

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage
    // ═════════════════════════════════════════════════════════════
    // 🎯 HÀM THAO TÁC
    // ═════════════════════════════════════════════════════════════

    /**
     * 📅 Chọn một ngày trên lịch.
     *
     * @param date Epoch midnight của ngày được chọn.
     */
    fun selectDate(date: Long) {
        _selectedDate.value = date
        Log.d(TAG, "📅 Đã chọn ngày: $date")
    }
/**
     * 👔 Gán một bộ đồ vào ngày đang chọn.
     *
     * Nếu ngày đó đã có outfit, nó sẽ bị ghi đè.
     *
     * @param outfitId UUID của Outfit cần gán.
 */
    fun assignOutfitToSelectedDate(outfitId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val event = CalendarEventEntity(
                    id = UUID.randomUUID().toString(),
                    date = _selectedDate.value,
                    outfitId = outfitId
                )

                calendarRepository.assignOutfitToDate(event)
                // 🔄 Refresh để UI cập nhật ngay lập tức
                _refreshTrigger.value = System.currentTimeMillis()
                Log.d(TAG, "✅ Đã gán outfit $outfitId cho ngày ${_selectedDate.value}")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Lỗi khi gán outfit: ${e.message}", e)
                _errorMessage.value = "Không thể gán bộ đồ: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 🗑️ Xoá bộ đồ đã gán khỏi ngày đang chọn.
     */
    fun removeOutfitFromSelectedDate() {
        viewModelScope.launch {
            try {
                val event = eventForSelectedDate.value ?: return@launch
                calendarRepository.removeEvent(event)
                // 🔄 Refresh để UI cập nhật ngay lập tức
                _refreshTrigger.value = System.currentTimeMillis()
                Log.d(TAG, "🗑️ Đã xoá outfit khỏi ngày ${_selectedDate.value}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Lỗi xoá outfit: ${e.message}", e)
                _errorMessage.value = "Không thể xoá bộ đồ: ${e.message}"
            }
        }
    }

    /**
     * 🧹 Xoá thông báo lỗi.
     */
    fun clearError() {
        _errorMessage.value = null
    }

    // ═════════════════════════════════════════════════════════════
    // 🛠️ TIỆN ÍCH
    // ═════════════════════════════════════════════════════════════

    private fun todayEpochMidnight(): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun epochToCalendar(epochMillis: Long): Calendar {
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = epochMillis
        }
    }
}

/**
 * 🏭 CalendarViewModelFactory — Factory để inject [CalendarRepository] + [OutfitRepository].
 */
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

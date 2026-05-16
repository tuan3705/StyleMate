package com.example.stylemate.repository

import com.example.stylemate.model.CalendarEventDao
import com.example.stylemate.model.CalendarEventEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * 🏪 CalendarRepository — Repository Pattern cho module Lịch (Calendar).
 *
 * Lớp trung gian duy nhất giữa [CalendarViewModel] và [CalendarEventDao].
 * - Tất cả thao tác ghi đều chạy trên [Dispatchers.IO].
 * - Flow từ DAO được giữ nguyên để giữ tính reactive.
 */
class CalendarRepository(private val calendarEventDao: CalendarEventDao) {

    // ─────────────────────────────────────────────────────────────
    // 📋 Đọc (Read)
    // ─────────────────────────────────────────────────────────────

    /**
     * Lấy tất cả sự kiện lịch dưới dạng Flow.
     */
    fun getAllEvents(): Flow<List<CalendarEventEntity>> {
        return calendarEventDao.getAllEvents()
    }

    /**
     * Lấy sự kiện của một ngày cụ thể (suspend).
     *
     * @param date Epoch midnight của ngày.
     * @return CalendarEventEntity hoặc null.
     */
    suspend fun getEventByDate(date: Long): CalendarEventEntity? = withContext(Dispatchers.IO) {
        calendarEventDao.getEventByDate(date)
    }

    /**
     * Lấy sự kiện của một ngày dưới dạng Flow (reactive).
     *
     * @param date Epoch midnight của ngày.
     */
    fun observeEventByDate(date: Long): Flow<CalendarEventEntity?> {
        return calendarEventDao.observeEventByDate(date)
    }

    /**
     * Lấy tất cả sự kiện trong khoảng thời gian.
     *
     * @param startDate Epoch midnight ngày bắt đầu.
     * @param endDate Epoch midnight ngày kết thúc.
     */
    fun getEventsBetween(startDate: Long, endDate: Long): Flow<List<CalendarEventEntity>> {
        return calendarEventDao.getEventsBetween(startDate, endDate)
    }

    // ─────────────────────────────────────────────────────────────
    // ➕ Ghi (Write)
    // ─────────────────────────────────────────────────────────────

    /**
     * Gán một bộ đồ vào một ngày.
     *
     * @param event CalendarEventEntity cần lưu.
     */
    suspend fun assignOutfitToDate(event: CalendarEventEntity) = withContext(Dispatchers.IO) {
        calendarEventDao.insertEvent(event)
    }

    /**
     * Xoá sự kiện khỏi một ngày.
     *
     * @param event Entity cần xoá.
     */
    suspend fun removeEvent(event: CalendarEventEntity) = withContext(Dispatchers.IO) {
        calendarEventDao.deleteEvent(event)
    }

    /**
     * Xoá sự kiện theo ngày.
     *
     * @param date Epoch midnight của ngày cần xoá.
     */
    suspend fun removeEventByDate(date: Long) = withContext(Dispatchers.IO) {
        calendarEventDao.deleteEventByDate(date)
    }
}

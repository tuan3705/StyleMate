package com.example.stylemate.repository

import android.util.Log
import com.example.stylemate.model.CalendarEventEntity
import com.example.stylemate.network.CalendarEventDto
import com.example.stylemate.network.StylemateApiService

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * ═══════════════════════════════════════════════════════════════
 * 🏪 CALENDAR REPOSITORY — Phiên bản API (thay thế Room hoàn toàn)
 * ═══════════════════════════════════════════════════════════════
 *
 * THAY THẾ hoàn toàn CalendarRepository cũ (dùng CalendarEventDao / Room)
 * bằng phiên bản gọi Retrofit API lên Backend Node.js.
 *
 * ✅ GIỮ NGUYÊN tên các phương thức + kiểu trả về để CalendarViewModel cũ
 *    không bị lỗi đỏ:
 *    - getAllEvents(): Flow<List<CalendarEventEntity>>
 *    - getEventByDate(date: Long): CalendarEventEntity?
 *    - observeEventByDate(date: Long): Flow<CalendarEventEntity?>
 *    - getEventsBetween(startDate, endDate): Flow<List<CalendarEventEntity>>
 *    - assignOutfitToDate(event: CalendarEventEntity)
 *    - removeEvent(event: CalendarEventEntity)
 *    - removeEventByDate(date: Long)
 *
 * 🔄 Map DTO (từ API) → Entity (cho ViewModel) và ngược lại.
 * ───────────────────────────────────────────────────────────────
 */
class CalendarRepository(private val apiService: StylemateApiService) {

    companion object {
        private const val TAG = "CalendarRepository"

        /**
         * Chuyển CalendarEventDto (từ API) → CalendarEventEntity (cho ViewModel).
         */
        fun CalendarEventDto.toEntity() = CalendarEventEntity(
            id = this.id,
            date = this.date,
            outfitId = this.outfitId
        )

        /**
         * Chuyển CalendarEventEntity (từ ViewModel) → CalendarEventDto (gửi API).
         */
        fun CalendarEventEntity.toDto() = CalendarEventDto(
            id = this.id,
            date = this.date,
            outfitId = this.outfitId,
            createdAt = System.currentTimeMillis()
        )
    }

    // ═════════════════════════════════════════════════════════════
    // 📋 Đọc (Read) — Reactive với Flow
    // ═════════════════════════════════════════════════════════════

    /**
     * 📋 Lấy tất cả sự kiện lịch dưới dạng Flow.
     *
     * ✅ Giữ đúng tên: getAllEvents()
     * ✅ Giữ đúng kiểu trả về: Flow<List<CalendarEventEntity>>
     *
     * ⚡ Flow chỉ emit 1 lần (không realtime như Room).
     * ViewModel cần gọi refresh thủ công nếu cần.
     */
    fun getAllEvents(): Flow<List<CalendarEventEntity>> = flow {
        try {
            val response = apiService.getCalendarEvents()
            if (response.isSuccessful) {
                val body = response.body()
                emit(body?.data?.map { it.toEntity() } ?: emptyList())
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ getAllEvents() lỗi: ${e.message}")
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 📋 Lấy sự kiện của một ngày cụ thể (suspend).
     *
     * ✅ Giữ đúng tên: getEventByDate(date: Long)
     * ✅ Giữ đúng kiểu trả về: CalendarEventEntity?
     *
     * Gọi API GET /api/calendar?date={date} để lọc theo ngày.
     * Backend trả về mảng — lấy phần tử đầu tiên (mỗi ngày chỉ có 1 sự kiện).
     *
     * @param date Epoch midnight của ngày cần tra (UTC).
     * @return CalendarEventEntity hoặc null nếu không có sự kiện.
     */
    suspend fun getEventByDate(date: Long): CalendarEventEntity? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getCalendarEvents(date = date)
            if (response.isSuccessful) {
                val body = response.body()
                body?.data?.firstOrNull()?.toEntity()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ getEventByDate() lỗi: ${e.message}")
            null
        }
    }

    /**
     * 📋 Lấy sự kiện của một ngày dưới dạng Flow (reactive).
     *
     * ✅ Giữ đúng tên: observeEventByDate(date: Long)
     * ✅ Giữ đúng kiểu trả về: Flow<CalendarEventEntity?>
     *
     * ⚡ Flow chỉ emit 1 lần tại thời điểm gọi.
     * Để refresh, ViewModel cần gọi lại hàm này hoặc dùng flatMapLatest
     * trên selectedDate (như cách CalendarViewModel đang làm).
     *
     * @param date Epoch midnight của ngày cần tra.
     */
    fun observeEventByDate(date: Long): Flow<CalendarEventEntity?> = flow {
        try {
            val response = apiService.getCalendarEvents(date = date)
            if (response.isSuccessful) {
                val body = response.body()
                emit(body?.data?.firstOrNull()?.toEntity())
            } else {
                emit(null)
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ observeEventByDate() lỗi: ${e.message}")
            emit(null)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 📋 Lấy tất cả sự kiện trong khoảng thời gian.
     *
     * ✅ Giữ đúng tên: getEventsBetween(startDate: Long, endDate: Long)
     * ✅ Giữ đúng kiểu trả về: Flow<List<CalendarEventEntity>>
     *
     * Gọi API GET /api/calendar?from={startDate}&to={endDate}
     * Dùng cho việc hiển thị chấm tròn trên các ngày có sự kiện.
     *
     * @param startDate Epoch midnight của ngày bắt đầu.
     * @param endDate Epoch midnight của ngày kết thúc.
     */
    fun getEventsBetween(startDate: Long, endDate: Long): Flow<List<CalendarEventEntity>> = flow {
        try {
            val response = apiService.getCalendarEvents(
                from = startDate,
                to = endDate
            )
            if (response.isSuccessful) {
                val body = response.body()
                emit(body?.data?.map { it.toEntity() } ?: emptyList())
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ getEventsBetween() lỗi: ${e.message}")
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 📊 Lấy số lượng sự kiện trong một khoảng thời gian.
     *
     * ✅ Giữ đúng tên: getEventCountBetween(startDate: Long, endDate: Long)
     * ✅ Giữ đúng kiểu trả về: Int (suspend)
     *
     * @param startDate Epoch midnight ngày bắt đầu.
     * @param endDate Epoch midnight ngày kết thúc.
     * @return Số lượng sự kiện trong khoảng.
     */
    suspend fun getEventCountBetween(startDate: Long, endDate: Long): Int =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getCalendarEvents(
                    from = startDate,
                    to = endDate
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    body?.count ?: (body?.data?.size ?: 0)
                } else {
                    0
                }
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ getEventCountBetween() lỗi: ${e.message}")
                0
            }
        }

    // ═════════════════════════════════════════════════════════════
    // ➕ Ghi (Write) — Suspend + IO
    // ═════════════════════════════════════════════════════════════

    /**
     * ➕ Gán một bộ đồ vào một ngày.
     *
     * ✅ Giữ đúng tên: assignOutfitToDate(event: CalendarEventEntity)
     *
     * Chuyển Entity → DTO → gọi API POST /api/calendar.
     * Backend hỗ trợ upsert: nếu trùng date → ghi đè.
     *
     * @param event CalendarEventEntity cần lưu.
     */
    suspend fun assignOutfitToDate(event: CalendarEventEntity) = withContext(Dispatchers.IO) {
        val dto = event.toDto()
        apiService.createCalendarEvent(dto)
    }

    /**
     * ❌ Xoá sự kiện khỏi một ngày.
     *
     * ✅ Giữ đúng tên: removeEvent(event: CalendarEventEntity)
     *
     * Gọi API DELETE /api/calendar/{id} với id của sự kiện.
     *
     * @param event Entity cần xoá.
     */
    suspend fun removeEvent(event: CalendarEventEntity) = withContext(Dispatchers.IO) {
        apiService.deleteCalendarEvent(event.id)
    }

    /**
     * ❌ Xoá sự kiện theo ngày.
     *
     * ✅ Giữ đúng tên: removeEventByDate(date: Long)
     *
     * Gọi API DELETE /api/calendar/by-date/{date}
     *
     * @param date Epoch midnight của ngày cần xoá.
     */
    suspend fun removeEventByDate(date: Long) = withContext(Dispatchers.IO) {
        apiService.deleteCalendarEventByDate(date)
    }


}

package com.example.stylemate.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * 🗄️ CalendarEventDao — Data Access Object cho CalendarEventEntity.
 *
 * Sử dụng [Flow] để UI observe các sự kiện trong lịch một cách reactive.
 * Sử dụng [suspend] cho các thao tác ghi để không block Main Thread.
 */
@Dao
interface CalendarEventDao {

    // ─────────────────────────────────────────────────────────────
    // 📋 Truy vấn
    // ─────────────────────────────────────────────────────────────

    /**
     * Lấy tất cả sự kiện lịch, trả về [Flow] để UI observe reactive.
     * Sắp xếp theo ngày giảm dần (mới nhất trước).
     */
    @Query("SELECT * FROM calendar_events ORDER BY date DESC")
    fun getAllEvents(): Flow<List<CalendarEventEntity>>

    /**
     * Lấy sự kiện của một ngày cụ thể.
     *
     * @param date Epoch midnight của ngày cần tra.
     * @return CalendarEventEntity hoặc null nếu không có sự kiện.
     */
    @Query("SELECT * FROM calendar_events WHERE date = :date LIMIT 1")
    suspend fun getEventByDate(date: Long): CalendarEventEntity?

    /**
     * Lấy sự kiện của một ngày dưới dạng Flow — reactive.
     * Khi sự kiện thay đổi (insert/delete), Flow tự emit giá trị mới.
     *
     * @param date Epoch midnight của ngày cần tra.
     */
    @Query("SELECT * FROM calendar_events WHERE date = :date LIMIT 1")
    fun observeEventByDate(date: Long): Flow<CalendarEventEntity?>

    /**
     * Lấy tất cả sự kiện trong một khoảng thời gian.
     * Dùng cho việc hiển thị chấm tròn trên các ngày có sự kiện.
     *
     * @param startDate Epoch midnight của ngày bắt đầu.
     * @param endDate Epoch midnight của ngày kết thúc.
     */
    @Query("SELECT * FROM calendar_events WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getEventsBetween(startDate: Long, endDate: Long): Flow<List<CalendarEventEntity>>

    /**
     * Lấy số lượng sự kiện trong một khoảng thời gian.
     * Dùng để kiểm tra nhanh nếu có sự kiện.
     */
    @Query("SELECT COUNT(*) FROM calendar_events WHERE date >= :startDate AND date <= :endDate")
    suspend fun getEventCountBetween(startDate: Long, endDate: Long): Int

    // ─────────────────────────────────────────────────────────────
    // ➕ Ghi
    // ─────────────────────────────────────────────────────────────

    /**
     * Thêm một sự kiện lịch mới.
     * Dùng [OnConflictStrategy.REPLACE] để nếu đã có sự kiện cho ngày đó,
     * nó sẽ ghi đè (thay outfit cũ bằng outfit mới).
     *
     * @param event CalendarEventEntity cần lưu.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CalendarEventEntity)

    /**
     * Xoá một sự kiện lịch.
     *
     * @param event Entity cần xoá.
     */
    @Delete
    suspend fun deleteEvent(event: CalendarEventEntity)

    /**
     * Xoá tất cả sự kiện cho một ngày cụ thể.
     *
     * @param date Epoch midnight của ngày cần xoá.
     */
    @Query("DELETE FROM calendar_events WHERE date = :date")
    suspend fun deleteEventByDate(date: Long)

    /**
     * Xoá tất cả sự kiện liên quan đến một Outfit.
     * Dùng khi Outfit bị xoá — nhưng nhờ CASCADE nên không cần gọi tay.
     *
     * @param outfitId UUID của Outfit.
     */
    @Query("DELETE FROM calendar_events WHERE outfitId = :outfitId")
    suspend fun deleteEventsByOutfitId(outfitId: String)
}

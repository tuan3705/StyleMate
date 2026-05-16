package com.example.stylemate.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 📅 CalendarEventEntity — Entity Room cho module Lịch (Calendar).
 *
 * Gán một bộ đồ (Outfit) vào một ngày cụ thể.
 *
 * 🏗️ Quan hệ:
 *   - CalendarEvent N:1 Outfit (một ngày chỉ chứa tối đa một bộ đồ,
 *     nhưng một bộ đồ có thể được gán vào nhiều ngày khác nhau).
 *
 * 🧠 Giải thích ForeignKey:
 *   - [outfitId] là khoá ngoại tham chiếu tới [OutfitEntity.id].
 *   - onDelete = CASCADE: Nếu Outfit bị xoá, các sự kiện lịch liên quan
 *     cũng tự động bị xoá — tránh dữ liệu "mồ côi" (orphan records).
 *   - Index trên [date] để tăng tốc truy vấn theo ngày.
 *   - UNIQUE constraint trên [date]: Mỗi ngày chỉ gán tối đa một outfit.
 *
 * 💡 Lưu ý về [date]:
 *   Lưu epoch midnight (00:00:00 UTC) của ngày đó, không lưu timestamp
 *   chứa giờ/phút/giây. Cách tính:
 *     val midnight = date.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
 *
 * @property id Mã định danh duy nhất (UUID string).
 * @property date Epoch millis của 00:00 UTC ngày đó — dùng để query.
 * @property outfitId Khoá ngoại → OutfitEntity.id.
 */
@Entity(
    tableName = "calendar_events",
    foreignKeys = [
        ForeignKey(
            entity = OutfitEntity::class,
            parentColumns = ["id"],
            childColumns = ["outfitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["date"], unique = true),   // Mỗi ngày chỉ 1 outfit
        Index(value = ["outfitId"])                // Tăng tốc truy vấn ngược
    ]
)
data class CalendarEventEntity(
    @PrimaryKey
    val id: String,
    val date: Long,      // Epoch midnight (UTC) — lưu giờ 00:00 của ngày đó
    val outfitId: String
)

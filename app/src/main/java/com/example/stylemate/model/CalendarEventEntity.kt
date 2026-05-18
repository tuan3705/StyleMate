package com.example.stylemate.model

/**
 * 📅 CalendarEventEntity — Sự kiện lịch: gán Outfit vào một ngày.
 *
 * Trước đây là Entity Room, nay là POJO thuần.
 *
 * @property id Mã định danh duy nhất (UUID string).
 * @property date Epoch millis của 00:00 UTC ngày đó.
 * @property outfitId UUID của Outfit được gán.
 */
data class CalendarEventEntity(
    val id: String,
    val date: Long,
    val outfitId: String
)

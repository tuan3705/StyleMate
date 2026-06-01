package com.example.stylemate.network

import com.google.gson.annotations.SerializedName

/**
 * ═══════════════════════════════════════════════════════════════
 * 📦 API DATA CLASSES (DTOs) — Ánh xạ JSON từ Backend Node.js
 * ═══════════════════════════════════════════════════════════════
 *
 * Tất cả các class này dùng để unmarshal JSON từ Backend qua Gson.
 * Backend dùng '_id' làm key trong JSON → map về 'id' ở Kotlin
 * bằng @SerializedName("_id").
 *
 * Lưu ý: Backend trả về các field trong một object:
 *   { success: true, data: { ... } }
 * Nên Retrofit chỉ unmarshal phần {...data} vào các class ở đây.
 * ───────────────────────────────────────────────────────────────
 */

// ═══════════════════════════════════════════════════════════════
// 👕 CLOTHING ITEM (Đồng bộ với ClothingItemEntity cũ)
// ═══════════════════════════════════════════════════════════════

/**
 * DTO cho ClothingItem — map từ JSON của Backend.
 * Backend trả về "_id" thay vì "id" — dùng @SerializedName để map.
 *
 * Tất cả field tương ứng 1:1 với ClothingItemEntity cũ (Room).
 */
data class ClothingItemDto(
    @SerializedName("_id")
    val id: String,

    val imageOriginal: String,
    val imageNoBg: String,
    val category: String,
    val color: String,
    val name: String,
    val season: String,
    val occasion: String,
    val brand: String,
    val purchaseDate: Long,
    val price: Double,
    val canvasPosX: Float,
    val canvasPosY: Float,
    val createdAt: Long
)

// ═══════════════════════════════════════════════════════════════
// 👔 OUTFIT (Backend trả về dạng lồng nhau — items nằm trong outfit)
// ═══════════════════════════════════════════════════════════════

/**
 * Sub-DTO: Một item trong mảng clothingItems của Outfit.
 * Backend trả về dạng:
 *   "clothingItems": [
 *       { "clothingItemId": "uuid", "posX": 0.5, "posY": 0.5 }
 *   ]
 */
data class OutfitClothingItemRefDto(
    val clothingItemId: String,
    val posX: Float,
    val posY: Float
)

/**
 * DTO chính cho Outfit — map từ JSON Backend.
 *
 * Backend trả về dạng:
 * {
 *   "_id": "uuid",
 *   "name": "Outfit name",
 *   "clothingItems": [ { "clothingItemId": "...", "posX": ..., "posY": ... } ],
 *   "createdAt": 123456789
 * }
 *
 * ⚠️ Quan trọng: Backend KHÔNG tách bảng trung gian riêng.
 * Items đã được lồng sẵn trong mảng clothingItems.
 */
data class OutfitDto(
    @SerializedName("_id")
    val id: String,

    val name: String,
    val clothingItems: List<OutfitClothingItemRefDto>,
    val createdAt: Long
)

/**
 * Response tổng hợp cho Outfit — dùng để thay thế
 * OutfitWithClothingItems cũ (Room).
 *
 * Backend API trả về OutfitDto như trên,
 * nhưng ViewModel cũ có thể cần OutfitWithClothingItemsResponse
 * với danh sách ClothingItemDto đầy đủ.
 *
 * Để đơn giản, ViewModel sẽ map từ OutfitDto sang entity tương ứng.
 * Class này giúp chứa outfit + danh sách items đã được populate.
 */
data class OutfitWithClothingItemsResponse(
    val outfit: OutfitDto,
    val clothingItems: List<ClothingItemDto>
)

// ═══════════════════════════════════════════════════════════════
// 📅 CALENDAR EVENT
// ═══════════════════════════════════════════════════════════════

/**
 * DTO cho CalendarEvent — map từ JSON Backend.
 *
 * Backend trả về:
 * {
 *   "_id": "uuid",
 *   "date": 1704067200000,      // epoch midnight UTC
 *   "outfitId": "uuid-outfit",
 *   "createdAt": 1704067200000
 * }
 */
data class CalendarEventDto(
    @SerializedName("_id")
    val id: String,

    val date: Long,
    val outfitId: String,
    val createdAt: Long
)

// ═══════════════════════════════════════════════════════════════
// 🌤️ WEATHER (WeatherAPI.com response — giữ nguyên từ code cũ)
// ═══════════════════════════════════════════════════════════════

/**
 * Các class cho Weather response — giữ nguyên từ WeatherModels.kt cũ.
 * Dùng cho api v1/forecast.json của weatherapi.com.
 */
data class WeatherApiResponse(
    val location: LocationInfo,
    val current: CurrentWeather,
    val forecast: ForecastResponse
)

data class LocationInfo(
    val name: String,
    val region: String,
    val country: String,
    val localtime: String
)

data class CurrentWeather(
    val temp_c: Double,
    val condition: WeatherCondition,
    val humidity: Int,
    val feelslike_c: Double,
    val wind_kph: Double,
    val uv: Double
)

data class ForecastResponse(
    val forecastday: List<ForecastDay>
)

data class ForecastDay(
    val date: String,
    val day: DayWeather,
    val astro: Astro?
)

data class DayWeather(
    val maxtemp_c: Double,
    val mintemp_c: Double,
    val condition: WeatherCondition
)

data class Astro(
    val sunrise: String,
    val sunset: String
)

data class WeatherCondition(
    val text: String,
    val icon: String
)

// ═══════════════════════════════════════════════════════════════
// 🖼️ UPLOAD RESPONSE — Backend trả về sau khi upload ảnh
// ═══════════════════════════════════════════════════════════════

/**
 * Response từ API upload ảnh.
 * Backend trả về: { success: true, url: "/uploads/abc.jpg" }
 */
data class UploadResponse(
    val success: Boolean,
    val url: String
)

// ═══════════════════════════════════════════════════════════════
// 🤖 AI AUTO-FILL RESPONSE
// ═══════════════════════════════════════════════════════════════

data class AiFillCategoryCandidateDto(
    val category: String,
    val confidence: Double,
    val source: String
)

data class AiFillCandidatesDto(
    val categories: List<AiFillCategoryCandidateDto> = emptyList()
)

data class AiFillSuggestionDto(
    val category: String?,
    val categoryConfidence: Double,
    val categorySource: String?,
    val color: String?,
    val colorConfidence: Double,
    val name: String?,
    val nameConfidence: Double,
    val candidates: AiFillCandidatesDto?
)

data class AiAutoTagTagDto(
    val label: String,
    val confidence: Double? = null
)

data class AiAutoTaggingSuggestionDto(
    val season: String?,
    val occasion: String?,
    val tags: List<AiAutoTagTagDto> = emptyList()
)

// ═══════════════════════════════════════════════════════════════
// 🗑️ Các class response wrapper (Backend trả về success/count/data)
// ═══════════════════════════════════════════════════════════════

/**
 * Generic response wrapper — Backend luôn trả về:
 * { success: true, count: N, data: [...] }
 * hoặc { success: true, data: { ... } }
 */
data class ApiListResponse<T>(
    val success: Boolean,
    val count: Int?,
    val data: List<T>
)

data class ApiSingleResponse<T>(
    val success: Boolean,
    val message: String?,
    val data: T
)

// ═══════════════════════════════════════════════════════════════
// 🔔 FCM TOKEN
// ═══════════════════════════════════════════════════════════════

data class FcmTokenRequest(
    val userId: String,
    val fcmToken: String,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class UserDeviceDto(
    @SerializedName("_id")
    val id: String,
    val userId: String,
    val fcmToken: String,
    val latitude: Double?,
    val longitude: Double?,
    val createdAt: Long,
    val updatedAt: Long
)

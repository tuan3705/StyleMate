package com.example.stylemate.network

import com.google.gson.annotations.SerializedName

/**
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


/**
 * Sub-DTO: Một item trong mảng clothingItems của Outfit.
 * Backend trả về dạng:
 *   "clothingItems": [
 *       { "clothingItemId": "uuid", "posX": 0.5, "posY": 0.5, "scale": 1.0 }
 *   ]
 */
data class OutfitClothingItemRefDto(
    val clothingItemId: String,
    val posX: Float,
    val posY: Float,
    val scale: Float = 1f,
    val rotation: Float = 0f,
    val flipX: Boolean = false
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


/**
 * Response từ API upload ảnh.
 * Backend trả về: { success: true, url: "/uploads/abc.jpg" }
 */
data class UploadResponse(
    val success: Boolean,
    val url: String?
)

/**
 * Request body for saving try-on result to collection.
 * Gửi lên khi user muốn lưu ảnh kết quả try-on vào tủ đồ.
 */
data class SaveToCollectionRequest(
    val name: String = "Try-On Result",
    val category: String = "Tops",
    val color: String = "",
    val season: String = "",
    val occasion: String = "",
    val brand: String = "",
    val price: Double = 0.0
)


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


data class ChatRequest(
    val userId: String,
    val message: String,
    val sessionId: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val dateMillis: Long? = null
)

data class OutfitSectionDto(
    val label: String,
    val item_description: String,
    val matching_item_ids: List<String> = emptyList()
)

data class SuggestedOutfitDto(
    val id: String? = null,
    val style_title: String? = null,
    val description: String? = null,
    val date: String? = null,
    val location: String? = null,
    val temp: String? = null,
    val sections: List<OutfitSectionDto>? = null,
    val name: String? = null,
    val headline: String? = null,
    val message: String? = null,
    val reason: String? = null,
    val confidence: Double? = null,
    val image_urls: Map<String, String>? = null,
    val items: List<String>? = null
)

data class ChatResponse(
    val sessionId: String? = null,
    val headline: String? = null, // Tiêu đề bắt mắt cho gợi ý trang chủ
    val message: String? = null,
    val suggested_outfits: List<SuggestedOutfitDto>? = null,
    val followups: List<String>? = null
)

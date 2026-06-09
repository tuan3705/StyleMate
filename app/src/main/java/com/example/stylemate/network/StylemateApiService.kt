package com.example.stylemate.network

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * ═══════════════════════════════════════════════════════════════
 * 🌐 STYLEMATE API SERVICE — Retrofit Interface
 * ═══════════════════════════════════════════════════════════════
 *
 * Định nghĩa tất cả endpoints của Backend Node.js.
 * Base URL: http://10.0.2.2:3000 (Android Emulator → localhost)
 *
 * Backend trả về JSON dạng:
 *   GET list:    { success: true, count: N, data: [...] }
 *   GET single:  { success: true, data: { ... } }
 *   POST/PUT:    { success: true, data: { ... } }
 *   DELETE:      { success: true, message: "...", data: {} }
 *
 * ───────────────────────────────────────────────────────────────
 */

interface StylemateApiService {

    // ═════════════════════════════════════════════════════════════
    // 🖼️ UPLOAD — /api/clothes/upload
    // ═════════════════════════════════════════════════════════════

    /**
     * POST /api/clothes/upload
     * Upload file ảnh lên server.
     *
     * @param file File ảnh dạng MultipartBody.Part
     * @return UploadResponse chứa success + url (/uploads/xxx.jpg)
     */
    @Multipart
    @POST("api/clothes/upload")
    suspend fun uploadImage(
        @Part file: MultipartBody.Part
    ): Response<UploadResponse>

    // ═════════════════════════════════════════════════════════════
    // 👕 CLOTHES — /api/clothes
    // ═════════════════════════════════════════════════════════════

    /**
     * GET /api/clothes
     * Lấy danh sách tất cả clothing items.
     * Query param: ?category=Tops (lọc theo danh mục, optional)
     *
     * @param category Lọc theo category (null = lấy tất cả)
     * @return ApiListResponse chứa danh sách ClothingItemDto
     */
    @GET("api/clothes")
    suspend fun getAllClothes(
        @Query("category") category: String? = null
    ): Response<ApiListResponse<ClothingItemDto>>

    /**
     * GET /api/clothes/{id}
     * Lấy chi tiết một clothing item theo UUID.
     *
     * @param id UUID của item
     * @return ApiSingleResponse chứa ClothingItemDto
     */
    @GET("api/clothes/{id}")
    suspend fun getClothingItemById(
        @Path("id") id: String
    ): Response<ApiSingleResponse<ClothingItemDto>>

    /**
     * POST /api/clothes
     * Tạo mới (hoặc upsert nếu _id đã tồn tại).
     *
     * Body gửi lên là ClothingItemDto với _id (UUID do Client sinh).
     *
     * @param item ClothingItemDto cần tạo
     * @return ApiSingleResponse chứa ClothingItemDto đã lưu
     */
    @POST("api/clothes")
    suspend fun createClothingItem(
        @Body item: ClothingItemDto
    ): Response<ApiSingleResponse<ClothingItemDto>>

    /**
     * POST /api/images/remove-bg
     */
    @Multipart
    @POST("api/images/remove-bg")
    suspend fun removeBackgroundImage(
        @Part image: MultipartBody.Part
    ): Response<ResponseBody>

    /**
     * POST /api/images/ai-fill
     */
    @Multipart
    @POST("api/images/ai-fill")
    suspend fun aiFillFromImage(
        @Part image: MultipartBody.Part
    ): Response<ApiSingleResponse<AiFillSuggestionDto>>

    /**
     * POST /api/images/auto-tagging
     */
    @Multipart
    @POST("api/images/auto-tagging")
    suspend fun autoTaggingFromImage(
        @Part image: MultipartBody.Part
    ): Response<ApiSingleResponse<AiAutoTaggingSuggestionDto>>

    /**
     * PUT /api/clothes/{id}
     * Cập nhật (partial update) một clothing item.
     *
     * @param id UUID của item cần cập nhật
     * @param item Map chứa các field cần update
     * @return ApiSingleResponse chứa ClothingItemDto đã cập nhật
     */
    @PUT("api/clothes/{id}")
    suspend fun updateClothingItem(
        @Path("id") id: String,
        @Body item: Map<String, @JvmSuppressWildcards Any>
    ): Response<ApiSingleResponse<ClothingItemDto>>

    /**
     * DELETE /api/clothes/{id}
     * Xoá một clothing item.
     *
     * @param id UUID của item cần xoá
     * @return ApiSingleResponse (data = empty map)
     */
    @DELETE("api/clothes/{id}")
    suspend fun deleteClothingItem(
        @Path("id") id: String
    ): Response<ApiSingleResponse<Map<String, Any>>>

    // ═════════════════════════════════════════════════════════════
    // 👔 OUTFITS — /api/outfits
    // ═════════════════════════════════════════════════════════════

    /**
     * GET /api/outfits
     * Lấy danh sách tất cả outfits.
     * Backend trả về mỗi outfit kèm mảng clothingItems (lồng nhau).
     *
     * @param populate Nếu "true" → Backend join thêm ClothingItem (optional)
     * @return ApiListResponse chứa danh sách OutfitDto
     */
    @GET("api/outfits")
    suspend fun getAllOutfits(
        @Query("populate") populate: String? = null,
        @Query("name") name: String? = null
    ): Response<ApiListResponse<OutfitDto>>

    /**
     * GET /api/outfits/{id}
     * Lấy chi tiết một outfit theo UUID.
     *
     * @param id UUID của outfit
     * @return ApiSingleResponse chứa OutfitDto
     */
    @GET("api/outfits/{id}")
    suspend fun getOutfitById(
        @Path("id") id: String
    ): Response<ApiSingleResponse<OutfitDto>>

    /**
     * GET /api/outfits/by-item/{clothingItemId}
     * Lấy danh sách outfits có chứa một clothing item.
     *
     * @param clothingItemId UUID của clothing item
     * @return ApiListResponse chứa danh sách OutfitDto
     */
    @GET("api/outfits/by-item/{clothingItemId}")
    suspend fun getOutfitsContainingItem(
        @Path("clothingItemId") clothingItemId: String
    ): Response<ApiListResponse<OutfitDto>>

    /**
     * POST /api/outfits
     * Tạo outfit mới.
     *
     * Body gửi lên gồm:
     *   - _id: UUID do Client sinh
     *   - name: String (tên bộ đồ)
     *   - clothingItems: Mảng [{ clothingItemId, posX, posY, scale }]
     *
     * @param outfit OutfitDto cần tạo
     * @return ApiSingleResponse chứa OutfitDto đã lưu
     */
    @POST("api/outfits")
    suspend fun createOutfit(
        @Body outfit: OutfitDto
    ): Response<ApiSingleResponse<OutfitDto>>

    /**
     * PUT /api/outfits/{id}
     * Cập nhật outfit (name và/hoặc clothingItems).
     *
     * @param id UUID của outfit cần cập nhật
     * @param outfit Map chứa các field cần update
     * @return ApiSingleResponse chứa OutfitDto đã cập nhật
     */
    @PUT("api/outfits/{id}")
    suspend fun updateOutfit(
        @Path("id") id: String,
        @Body outfit: Map<String, @JvmSuppressWildcards Any>
    ): Response<ApiSingleResponse<OutfitDto>>

    /**
     * DELETE /api/outfits/{id}
     * Xoá outfit.
     * ⚠️ Backend tự động CASCADE xoá CalendarEvent liên quan.
     *
     * @param id UUID của outfit cần xoá
     * @return ApiSingleResponse (data = empty map)
     */
    @DELETE("api/outfits/{id}")
    suspend fun deleteOutfit(
        @Path("id") id: String
    ): Response<ApiSingleResponse<Map<String, Any>>>

    // ═════════════════════════════════════════════════════════════
    // 📅 CALENDAR — /api/calendar
    // ═════════════════════════════════════════════════════════════

    /**
     * GET /api/calendar
     * Lấy danh sách sự kiện lịch.
     *
     * Query params (optional):
     *   - ?date=epochMidnight    (lấy sự kiện 1 ngày)
     *   - ?from=epoch&to=epoch   (lấy sự kiện trong khoảng)
     *
     * @param date Epoch midnight (lấy 1 ngày, optional)
     * @param from Epoch midnight bắt đầu (optional)
     * @param to Epoch midnight kết thúc (optional)
     * @return ApiListResponse chứa danh sách CalendarEventDto
     */
    @GET("api/calendar")
    suspend fun getCalendarEvents(
        @Query("date") date: Long? = null,
        @Query("from") from: Long? = null,
        @Query("to") to: Long? = null
    ): Response<ApiListResponse<CalendarEventDto>>

    /**
     * GET /api/calendar/{id}
     * Lấy chi tiết một sự kiện lịch theo UUID.
     *
     * @param id UUID của sự kiện
     * @return ApiSingleResponse chứa CalendarEventDto
     */
    @GET("api/calendar/{id}")
    suspend fun getCalendarEventById(
        @Path("id") id: String
    ): Response<ApiSingleResponse<CalendarEventDto>>

    /**
     * POST /api/calendar
     * Gán outfit vào ngày (upsert: nếu trùng date → ghi đè).
     *
     * Body gửi lên:
     *   - _id: UUID do Client sinh
     *   - date: Epoch midnight của ngày
     *   - outfitId: UUID của outfit
     *
     * @param event CalendarEventDto cần tạo/cập nhật
     * @return ApiSingleResponse chứa CalendarEventDto đã lưu
     */
    @POST("api/calendar")
    suspend fun createCalendarEvent(
        @Body event: CalendarEventDto
    ): Response<ApiSingleResponse<CalendarEventDto>>

    /**
     * DELETE /api/calendar/{id}
     * Xoá sự kiện lịch theo UUID.
     *
     * @param id UUID của sự kiện
     * @return ApiSingleResponse (data = empty map)
     */
    @DELETE("api/calendar/{id}")
    suspend fun deleteCalendarEvent(
        @Path("id") id: String
    ): Response<ApiSingleResponse<Map<String, Any>>>

    /**
     * DELETE /api/calendar/by-date/{date}
     * Xoá sự kiện theo ngày (epoch midnight).
     *
     * @param date Epoch midnight của ngày cần xoá
     * @return ApiSingleResponse
     */
    @DELETE("api/calendar/by-date/{date}")
    suspend fun deleteCalendarEventByDate(
        @Path("date") date: Long
    ): Response<ApiSingleResponse<CalendarEventDto>>

    // ═════════════════════════════════════════════════════════════
    // 🔐 AUTH — /api/auth
    // ═════════════════════════════════════════════════════════════

    /**
     * POST /api/auth/login
     * Đăng nhập người dùng.
     */
    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse<AuthLoginData>>

    /**
     * POST /api/auth/register
     * Đăng ký người dùng mới.
     */
    @POST("api/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<AuthResponse<AuthLoginData>>

    /**
     * POST /api/auth/refresh
     * Làm mới Access Token bằng Refresh Token.
     */
    @POST("api/auth/refresh")
    suspend fun refresh(
        @Body request: RefreshRequest
    ): Response<AuthResponse<RefreshData>>

    /**
     * POST /api/auth/logout
     * Đăng xuất.
     */
    @POST("api/auth/logout")
    suspend fun logout(): Response<SimpleMessageResponse>

    // ═════════════════════════════════════════════════════════════
    // 🔔 FCM — /api/user/fcm-token
    // ═════════════════════════════════════════════════════════════

    /**
     * POST /api/user/fcm-token
     * Lưu hoặc cập nhật FCM Token cho người dùng.
     */
    @POST("api/user/fcm-token")
    suspend fun saveFcmToken(
        @Body request: FcmTokenRequest
    ): Response<ApiSingleResponse<Map<String, Any>>>

    // ═════════════════════════════════════════════════════════════
    // 🤖 AI STYLIST — /api/ai-stylist
    // ═════════════════════════════════════════════════════════════

    /**
     * POST /api/ai-stylist/chat
     * Chat với AI Stylist (Gemini) để nhận gợi ý phối đồ.
     */
    @POST("api/ai-stylist/chat")
    suspend fun chatWithAi(
        @Body request: ChatRequest
    ): Response<ChatResponse>

    /**
     * GET /api/ai-stylist/home-suggestions
     */
    @GET("api/ai-stylist/home-suggestions")
    suspend fun getHomeSuggestions(
        @Query("userId") userId: String,
        @Query("lat") lat: Double?,
        @Query("lon") lon: Double?
    ): Response<ChatResponse>

    // ═════════════════════════════════════════════════════════════
    // 🌤️ WEATHER — /api/weather/forecast (Proxy)
    // ═════════════════════════════════════════════════════════════

    /**
     * GET /api/weather/forecast
     * Proxy: Backend gọi WeatherAPI.com thay Client.
     * Trả nguyên JSON của WeatherAPI.com về.
     *
     * @param lat Vĩ độ (latitude)
     * @param lon Kinh độ (longitude)
     * @return model.weather.WeatherApiResponse (dùng chung với UI)
     */
    @GET("api/weather/forecast")
    suspend fun getWeatherForecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): Response<com.example.stylemate.model.weather.WeatherApiResponse>
}

package com.example.stylemate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stylemate.network.ChatRequest
import com.example.stylemate.network.RetrofitClient
import com.example.stylemate.model.weather.WeatherApiResponse
import com.example.stylemate.repository.WeatherRepository
import com.example.stylemate.data.auth.AuthStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

import com.example.stylemate.network.SuggestedOutfitDto

data class AIStylistUiState(
    val headline: String = "Sporty, comfortable style for a rainy day",
    val recommendationText: String = "",
    val dateText: String = "Jun 9",
    val locationText: String = "Hanoi",
    val tempText: String = "26 / 22°C",
    val suggestedOutfits: List<SuggestedOutfitDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isFromCache: Boolean = false
)

/**
 * ⚡ Cache entry for HomeSuggestion results
 */
private data class HomeSuggestionCacheEntry(
    val headline: String,
    val recommendationText: String,
    val suggestedOutfits: List<SuggestedOutfitDto>,
    val timestamp: Long
)

class AIStylistViewModel(
    private val authStorage: AuthStorage,
    private val weatherRepository: WeatherRepository = WeatherRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AIStylistUiState())
    val uiState: StateFlow<AIStylistUiState> = _uiState

    // ⚡ Cache: key = "userId_lat_lon", value = cached data
    private val homeSuggestionCache = mutableMapOf<String, HomeSuggestionCacheEntry>()
    private val CACHE_TTL_MS = 10 * 60 * 1000L // 10 phút

    // Cache cho weather data (theo tọa độ)
    private var cachedWeatherResponse: WeatherApiResponse? = null
    private var cachedWeatherLat: Double = 0.0
    private var cachedWeatherLon: Double = 0.0
    private var cachedWeatherTimestamp: Long = 0L
    private val WEATHER_CACHE_TTL_MS = 5 * 60 * 1000L // 5 phút

    init {
        val sdf = SimpleDateFormat("MMM d", Locale.ENGLISH)
        _uiState.value = _uiState.value.copy(dateText = sdf.format(Date()))
    }

    fun refreshWeatherAndRecommendation(lat: Double? = null, lon: Double? = null, forceRefresh: Boolean = false, refreshWeather: Boolean = false) {
        viewModelScope.launch {
            val userId = authStorage.userIdFlow.firstOrNull()
            if (userId == null) {
                _uiState.value = _uiState.value.copy(error = "Please log in again")
                return@launch
            }

            val finalLat = lat ?: 10.8231
            val finalLon = lon ?: 106.6297

            // ⚡ Kiểm tra cache HomeSuggestion trước
            val roundedLat = Math.round(finalLat * 100.0) / 100.0
            val roundedLon = Math.round(finalLon * 100.0) / 100.0
            val cacheKey = "${userId}_${roundedLat}_${roundedLon}"
            val cached = homeSuggestionCache[cacheKey]
            val now = System.currentTimeMillis()

            // Nếu forceRefresh -> xóa cache HomeSuggestion + xóa cache weather
            if (forceRefresh) {
                homeSuggestionCache.remove(cacheKey)
                cachedWeatherResponse = null
            } else if (cached != null && now - cached.timestamp < CACHE_TTL_MS) {
                // Dùng cache cho HomeSuggestion, nhưng vẫn cập nhật weather + date
                val weather = getCachedOrFetchWeather(finalLat, finalLon)
                val location = if (weather != null) {
                    "${weather.location.name}, ${weather.location.region}"
                } else {
                    _uiState.value.locationText
                }
                val sdf = SimpleDateFormat("MMM d", Locale.ENGLISH)
                val dateStr = sdf.format(Date())

                _uiState.value = _uiState.value.copy(
                    headline = cached.headline,
                    recommendationText = cached.recommendationText,
                    suggestedOutfits = cached.suggestedOutfits,
                    locationText = location,
                    dateText = dateStr,
                    isLoading = false,
                    error = null,
                    isFromCache = true
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null, isFromCache = false)
            try {
                // 1. Fetch Weather (dùng cache nếu có)
                val weather = getCachedOrFetchWeather(finalLat, finalLon)
                val location = if (weather != null) {
                    "${weather.location.name}, ${weather.location.region}"
                } else {
                    "Hanoi"
                }
                val currentTemp = weather?.current?.tempC?.toInt() ?: 25
                val minTemp = weather?.forecast?.forecastDay?.firstOrNull()?.day?.minTempC?.toInt() ?: (currentTemp - 5)
                val maxTemp = weather?.forecast?.forecastDay?.firstOrNull()?.day?.maxTempC?.toInt() ?: (currentTemp + 5)

                val sdf = SimpleDateFormat("MMM d", Locale.ENGLISH)
                val dateStr = sdf.format(Date())

                // 2. Fetch AI Recommendation from Home Suggestions API
                val response = RetrofitClient.stylemateApiService.getHomeSuggestions(
                    userId = userId,
                    lat = finalLat,
                    lon = finalLon
                )

                if (response.isSuccessful) {
                    val data = response.body()
                    val newHeadline = data?.headline ?: "Today's suggestion"
                    val newMessage = data?.message ?: ""
                    val newOutfits = data?.suggested_outfits ?: emptyList()

                    // ⚡ Lưu vào cache
                    homeSuggestionCache[cacheKey] = HomeSuggestionCacheEntry(
                        headline = newHeadline,
                        recommendationText = newMessage,
                        suggestedOutfits = newOutfits,
                        timestamp = now
                    )

                    _uiState.value = _uiState.value.copy(
                        headline = newHeadline,
                        recommendationText = newMessage,
                        suggestedOutfits = newOutfits,
                        locationText = location,
                        tempText = "$maxTemp / $minTemp°C",
                        dateText = dateStr,
                        isLoading = false,
                        isFromCache = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        locationText = location,
                        tempText = "$maxTemp / $minTemp°C",
                        dateText = dateStr,
                        isLoading = false,
                        error = "Home Suggestions API error: ${response.code()}"
                    )
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error: ${e.message}"
                )
            }
        }
    }

    /**
     * ⚡ Lấy weather từ cache nếu còn hạn, nếu không thì fetch mới
     */
    private suspend fun getCachedOrFetchWeather(lat: Double, lon: Double): WeatherApiResponse? {
        val now = System.currentTimeMillis()
        if (cachedWeatherResponse != null &&
            cachedWeatherLat == lat &&
            cachedWeatherLon == lon &&
            now - cachedWeatherTimestamp < WEATHER_CACHE_TTL_MS) {
            return cachedWeatherResponse
        }

        return try {
            val weather = weatherRepository.getWeatherForecast(lat, lon)
            cachedWeatherResponse = weather
            cachedWeatherLat = lat
            cachedWeatherLon = lon
            cachedWeatherTimestamp = now
            weather
        } catch (e: Exception) {
            // Nếu fetch thất bại, trả về cache cũ nếu có
            cachedWeatherResponse
        }
    }
}
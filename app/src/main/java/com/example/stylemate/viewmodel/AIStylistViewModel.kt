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
    val headline: String = "Phong cách thể thao năng động, thoải mái cho ngày mưa",
    val recommendationText: String = "",
    val dateText: String = "9 thg 6",
    val locationText: String = "Thành phố Hà Nội",
    val tempText: String = "26 / 22°C",
    val suggestedOutfits: List<SuggestedOutfitDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class AIStylistViewModel(
    private val authStorage: AuthStorage,
    private val weatherRepository: WeatherRepository = WeatherRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AIStylistUiState())
    val uiState: StateFlow<AIStylistUiState> = _uiState

    init {
        // Initial load with default coordinates if needed,
        // or wait for explicit refresh.
        // For now, let's just keep the default dummy state.
        val sdf = SimpleDateFormat("dd 'thg' M", Locale("vi"))
        _uiState.value = _uiState.value.copy(dateText = sdf.format(Date()))
    }

    fun refreshWeatherAndRecommendation(lat: Double? = null, lon: Double? = null) {
        viewModelScope.launch {
            val userId = authStorage.userIdFlow.firstOrNull()
            if (userId == null) {
                _uiState.value = _uiState.value.copy(error = "Vui lòng đăng nhập lại")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // Use provided coordinates or fallback to a sensible default if null.
                // In a real app, this fallback should ideally be the last known location or a user-selected city.
                val finalLat = lat ?: 10.8231
                val finalLon = lon ?: 106.6297

                // 1. Fetch Weather
                val weather = weatherRepository.getWeatherForecast(finalLat, finalLon)
                val location = "${weather.location.name}, ${weather.location.region}"
                val currentTemp = weather.current.tempC.toInt()
                val minTemp = weather.forecast.forecastDay.firstOrNull()?.day?.minTempC?.toInt() ?: (currentTemp - 5)
                val maxTemp = weather.forecast.forecastDay.firstOrNull()?.day?.maxTempC?.toInt() ?: (currentTemp + 5)

                val sdf = SimpleDateFormat("dd 'thg' M", Locale("vi"))
                val dateStr = sdf.format(Date())

                // 2. Fetch AI Recommendation from Home Suggestions API
                val response = RetrofitClient.stylemateApiService.getHomeSuggestions(
                    userId = userId,
                    lat = finalLat,
                    lon = finalLon
                )

                if (response.isSuccessful) {
                    val data = response.body()
                    _uiState.value = _uiState.value.copy(
                        headline = data?.headline ?: "Gợi ý hôm nay",
                        recommendationText = data?.message ?: _uiState.value.recommendationText,
                        suggestedOutfits = data?.suggested_outfits ?: emptyList(),
                        locationText = location,
                        tempText = "$maxTemp / $minTemp°C",
                        dateText = dateStr,
                        isLoading = false
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
                    error = "Lỗi: ${e.message}"
                )
            }
        }
    }
}

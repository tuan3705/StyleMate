package com.example.stylemate.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.stylemate.model.weather.WeatherAnalysis
import com.example.stylemate.model.weather.WeatherApiResponse
import com.example.stylemate.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 🧩 WeatherViewModel — ViewModel cho màn hình Thời tiết.
 *
 * Quản lý 4 StateFlow chính:
 *   1. [weatherData] — Dữ liệu thời tiết (hiện tại + dự báo).
 *   2. [weatherAnalysis] — Kết quả phân tích thời tiết (cho Chatbot).
 *   3. [isLoading] — Trạng thái loading.
 *   4. [errorMessage] — Thông báo lỗi nếu có.
 *
 * 🔄 Luồng dữ liệu:
 *   UI (Compose) ← collect StateFlow ← WeatherViewModel ← WeatherRepository ← Retrofit ← API
 */
class WeatherViewModel(
    private val repository: WeatherRepository
) : ViewModel() {

    companion object {
        private const val TAG = "WeatherViewModel"

        // 🌏 Toạ độ mặc định: Hà Nội, Việt Nam
        // Được dùng khi không có quyền truy cập vị trí
        const val DEFAULT_LAT = 21.0285
        const val DEFAULT_LON = 105.8542
    }

    // ──────────────────────────────────────────────────────────────
    // 🔷 State: Dữ liệu thời tiết
    // ──────────────────────────────────────────────────────────────

    private val _weatherData = MutableStateFlow<WeatherApiResponse?>(null)
    val weatherData: StateFlow<WeatherApiResponse?> = _weatherData

    // ──────────────────────────────────────────────────────────────
    // 🔷 State: Tên thành phố / vị trí đang hiển thị
    // ──────────────────────────────────────────────────────────────

    private val _locationName = MutableStateFlow("Determining…")
    val locationName: StateFlow<String> = _locationName

    // ──────────────────────────────────────────────────────────────
    // 🔷 State: Toạ độ hiện tại (để refresh)
    // ──────────────────────────────────────────────────────────────

    private var currentLat = DEFAULT_LAT
    private var currentLon = DEFAULT_LON

    // ──────────────────────────────────────────────────────────────
    // 🔷 State: Kết quả phân tích thời tiết (cho Chatbot context)
    // ──────────────────────────────────────────────────────────────

    private val _weatherAnalysis = MutableStateFlow<WeatherAnalysis?>(null)
    val weatherAnalysis: StateFlow<WeatherAnalysis?> = _weatherAnalysis

    // ──────────────────────────────────────────────────────────────
    // 🔷 State: Loading & Error
    // ──────────────────────────────────────────────────────────────

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // ═════════════════════════════════════════════════════════════
    // 🎯 HÀM LẤY DỮ LIỆU THỜI TIẾT
    // ═════════════════════════════════════════════════════════════

    /**
     * 🌤️ Lấy dữ liệu thời tiết cho toạ độ chỉ định.
     *
     * Gọi từ UI khi màn hình được hiển thị (onStart/LaunchedEffect).
     *
     * @param lat Vĩ độ.
     * @param lon Kinh độ.
     * @param cityName Tên thành phố (hiển thị trên UI), null nếu dùng mặc định.
     */
    fun fetchWeather(lat: Double, lon: Double, cityName: String? = null) {
        viewModelScope.launch {
            try {
                // ── Bước 1: Bật loading ──────────────────────────
                _isLoading.value = true
                _errorMessage.value = null
                currentLat = lat
                currentLon = lon

                Log.d(TAG, "🌤️ Đang lấy dữ liệu thời tiết cho: $lat, $lon")

                // ── Bước 2: Gọi API ──────────────────────────────
                val response = repository.getWeatherForecast(lat, lon)

                // ── Bước 3: Cập nhật State ───────────────────────
                _weatherData.value = response

                // ── Bước 4: Cập nhật tên thành phố từ API ────────
                val apiCity = response.location.name
                val apiCountry = response.location.country
                _locationName.value = if (apiCountry == "Vietnam") {
                    apiCity // Chỉ hiện "Hanoi", "Saigon"...
                } else {
                    "$apiCity, $apiCountry"
                }

                Log.d(TAG, "✅ Thành công! Vị trí: ${_locationName.value}, " +
                        "Nhiệt độ: ${response.current.tempC}°C")

                // ── Bước 5: Phân tích thời tiết ──────────────────
                val analysis = repository.analyzeWeather(response.current.tempC)
                _weatherAnalysis.value = analysis
                Log.d(TAG, "🧠 Phân tích: ${analysis.label}")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Lỗi: ${e.message}", e)
                val msg = e.message ?: ""
                _errorMessage.value = when {
                    msg.contains("403") ->
                        "Invalid API key. Register a free key at weatherapi.com and set it in RetrofitClient.kt"
                    msg.contains("Unable to resolve host") || msg.contains("Failed to connect") ->
                        "No internet connection. Please check your network."
                    else -> "Unable to load weather data: ${e.message}"
                }
            } finally {
                // ── Bước 6: Tắt loading ──────────────────────────
                _isLoading.value = false
            }
        }
    }

    /**
     * 🌍 Cập nhật vị trí từ GPS và fetch thời tiết.
     *
     * @param lat Vĩ độ từ GPS.
     * @param lon Kinh độ từ GPS.
     */
    fun fetchWeatherByGps(lat: Double, lon: Double) {
        _locationName.value = "Current location"
        fetchWeather(lat, lon, null)
    }

    /**
     * 🔄 Refresh dữ liệu thời tiết (giữ nguyên toạ độ).
     */
    fun refresh() {
        fetchWeather(currentLat, currentLon)
    }

    /**
     * 🧹 Xoá thông báo lỗi.
     */
    fun clearError() {
        _errorMessage.value = null
    }
}

/**
 * 🏭 WeatherViewModelFactory — Factory để inject [WeatherRepository].
 */
class WeatherViewModelFactory(
    private val repository: WeatherRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WeatherViewModel::class.java)) {
            return WeatherViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

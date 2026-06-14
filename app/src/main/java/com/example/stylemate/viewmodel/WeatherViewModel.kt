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
 * 🧩 WeatherViewModel — ViewModel for Weather screen.
 *
 * Manages 4 main StateFlows:
 *   1. [weatherData] — Weather data (current + forecast).
 *   2. [weatherAnalysis] — Weather analysis result (for Chatbot).
 *   3. [isLoading] — Loading state.
 *   4. [errorMessage] — Error message if any.
 *
 * 🔄 Data flow:
 *   UI (Compose) ← collect StateFlow ← WeatherViewModel ← WeatherRepository ← Retrofit ← API
 */
class WeatherViewModel(
    private val repository: WeatherRepository
) : ViewModel() {

    companion object {
        private const val TAG = "WeatherViewModel"

        // 🌏 Default coordinates: Hanoi, Vietnam
        // Used when location permission is not granted
        const val DEFAULT_LAT = 21.0285
        const val DEFAULT_LON = 105.8542
    }

    // ──────────────────────────────────────────────────────────────
    // 🔷 State: Weather data
    // ──────────────────────────────────────────────────────────────

    private val _weatherData = MutableStateFlow<WeatherApiResponse?>(null)
    val weatherData: StateFlow<WeatherApiResponse?> = _weatherData

    // ──────────────────────────────────────────────────────────────
    // 🔷 State: City name / current location display
    // ──────────────────────────────────────────────────────────────

    private val _locationName = MutableStateFlow("Determining…")
    val locationName: StateFlow<String> = _locationName

    // ──────────────────────────────────────────────────────────────
    // 🔷 State: Current coordinates (for refresh)
    // ──────────────────────────────────────────────────────────────

    private var currentLat = DEFAULT_LAT
    private var currentLon = DEFAULT_LON

    // ──────────────────────────────────────────────────────────────
    // 🔷 State: Weather analysis result (for Chatbot context)
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
    // 🎯 WEATHER DATA FETCHING
    // ═════════════════════════════════════════════════════════════

    /**
     * 🌤️ Fetch weather data for specified coordinates.
     *
     * Called from UI when the screen is displayed (onStart/LaunchedEffect).
     *
     * @param lat Latitude.
     * @param lon Longitude.
     * @param cityName City name (displayed on UI), null if using default.
     */
    fun fetchWeather(lat: Double, lon: Double, cityName: String? = null) {
        viewModelScope.launch {
            try {
                // ── Step 1: Enable loading ──────────────────────────
                _isLoading.value = true
                _errorMessage.value = null
                currentLat = lat
                currentLon = lon

                Log.d(TAG, "🌤️ Fetching weather data for: $lat, $lon")

                // ── Step 2: Call API ──────────────────────────────
                val response = repository.getWeatherForecast(lat, lon)

                // ── Step 3: Update State ───────────────────────
                _weatherData.value = response

                // ── Step 4: Update city name from API ────────
                val apiCity = response.location.name
                val apiCountry = response.location.country
                _locationName.value = if (apiCountry == "Vietnam") {
                    apiCity // Just show "Hanoi", "Saigon"...
                } else {
                    "$apiCity, $apiCountry"
                }

                Log.d(TAG, "✅ Success! Location: ${_locationName.value}, " +
                        "Temperature: ${response.current.tempC}°C")

                // ── Step 5: Analyze weather ──────────────────
                val analysis = repository.analyzeWeather(response.current.tempC)
                _weatherAnalysis.value = analysis
                Log.d(TAG, "🧠 Analysis: ${analysis.label}")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error: ${e.message}", e)
                val msg = e.message ?: ""
                _errorMessage.value = when {
                    msg.contains("403") ->
                        "Invalid API key. Register a free key at weatherapi.com and set it in RetrofitClient.kt"
                    msg.contains("Unable to resolve host") || msg.contains("Failed to connect") ->
                        "No internet connection. Please check your network."
                    else -> "Unable to load weather data: ${e.message}"
                }
            } finally {
                // ── Step 6: Disable loading ──────────────────────────
                _isLoading.value = false
            }
        }
    }

    /**
     * 🌍 Update location from GPS and fetch weather.
     *
     * @param lat Latitude from GPS.
     * @param lon Longitude from GPS.
     */
    fun fetchWeatherByGps(lat: Double, lon: Double) {
        _locationName.value = "Current location"
        fetchWeather(lat, lon, null)
    }

    /**
     * 🔄 Refresh weather data (keep current coordinates).
     */
    fun refresh() {
        fetchWeather(currentLat, currentLon)
    }

    /**
     * 🧹 Clear error message.
     */
    fun clearError() {
        _errorMessage.value = null
    }
}

/**
 * 🏭 WeatherViewModelFactory — Factory to inject [WeatherRepository].
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

package com.example.stylemate.repository

import com.example.stylemate.model.weather.WeatherAnalysis
import com.example.stylemate.model.weather.WeatherApiResponse
import com.example.stylemate.network.RetrofitClient
import com.example.stylemate.network.StylemateApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 🏪 WeatherRepository — Repository Pattern for Weather module.
 *
 * 🔒 Calls Backend proxy (/api/weather/forecast), NOT WeatherAPI.com directly.
 *    -> WEATHER_API_KEY only stored on server (.env), not exposed on Client.
 *
 * ViewModel calls this repository to fetch weather data.
 */
class WeatherRepository(
    private val apiService: StylemateApiService = RetrofitClient.stylemateApiService
) {

    companion object {
        /**
         * Resource key prefix for weather suggestions defined in strings.xml.
         * Usage: context.getString(
         *     context.resources.getIdentifier(key, "string", context.packageName)
         * )
         */
        const val SUGGESTION_RESOURCE_PREFIX = "weather_suggestion_"

        /**
         * Returns the resource key for a given weather label.
         * Example: "VeryCold" -> "weather_suggestion_very_cold"
         */
        fun getSuggestionResourceKey(label: String): String {
            return "$SUGGESTION_RESOURCE_PREFIX${label.lowercase().replace(' ', '_')}"
        }
    }

    /**
     * Fetches weather data + 3-day forecast via Backend proxy.
     *
     * Backend receives lat+lon, adds API Key, calls WeatherAPI.com, returns JSON.
     *
     * @param lat Latitude.
     * @param lon Longitude.
     * @return [WeatherApiResponse] if successful.
     * @throws Exception if network error or API returns an error.
     */
    suspend fun getWeatherForecast(lat: Double, lon: Double): WeatherApiResponse =
        withContext(Dispatchers.IO) {
            val response = apiService.getWeatherForecast(lat, lon)
            if (response.isSuccessful) {
                response.body() ?: throw Exception("Response body is null")
            } else {
                throw Exception("Weather API error: ${response.code()} ${response.message()}")
            }
        }

    /**
     * Quick weather analysis for Chatbot context.
     *
     * Returns a [WeatherAnalysis] containing only a label.
     * The suggestion text must be resolved from string resources
     * in the ViewModel/UI layer using [getSuggestionResourceKey].
     *
     * Simple rules based on temperature:
     *   - < 10°C  -> "VeryCold"
     *   - < 20°C  -> "Cold"
     *   - < 25°C  -> "Cool"
     *   - < 30°C  -> "Warm"
     *   - >= 30°C -> "Hot"
     *
     * @param tempC Current temperature (°C).
     * @return [WeatherAnalysis] with label only. Suggestion is resolved via resource key.
     */
    fun analyzeWeather(tempC: Double): WeatherAnalysis {
        return when {
            tempC < 10 -> WeatherAnalysis(label = "VeryCold")
            tempC < 20 -> WeatherAnalysis(label = "Cold")
            tempC < 25 -> WeatherAnalysis(label = "Cool")
            tempC < 30 -> WeatherAnalysis(label = "Warm")
            else -> WeatherAnalysis(label = "Hot")
        }
    }
}
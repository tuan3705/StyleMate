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
     * Simple rules based on temperature:
     *   - < 10°C  -> "VeryCold"
     *   - < 20°C  -> "Cold"
     *   - < 25°C  -> "Cool"
     *   - < 30°C  -> "Warm"
     *   - >= 30°C -> "Hot"
     *
     * @param tempC Current temperature (°C).
     * @return [WeatherAnalysis] with label + suggestion.
     */
    fun analyzeWeather(tempC: Double): WeatherAnalysis {
        return when {
            tempC < 10 -> WeatherAnalysis(
                label = "VeryCold",
                suggestion = "Very cold! Wear a heavy coat, scarf, and gloves."
            )
            tempC < 20 -> WeatherAnalysis(
                label = "Cold",
                suggestion = "Cold weather. Sweater, light jacket, and long pants are suitable."
            )
            tempC < 25 -> WeatherAnalysis(
                label = "Cool",
                suggestion = "Cool weather. Long sleeves, jeans, or light skirt."
            )
            tempC < 30 -> WeatherAnalysis(
                label = "Warm",
                suggestion = "Warm weather. Short sleeves, dresses, lightweight pants."
            )
            else -> WeatherAnalysis(
                label = "Hot",
                suggestion = "Hot weather! Shorts, tank tops, maxi dresses, breathable fabrics."
            )
        }
    }
}
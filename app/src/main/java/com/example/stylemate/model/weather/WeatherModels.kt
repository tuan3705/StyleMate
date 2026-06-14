package com.example.stylemate.model.weather

import com.google.gson.annotations.SerializedName

/**
 * 🌤️ WeatherModels — Data classes cho WeatherAPI.com response.
 *
 * API endpoint: GET /v1/forecast.json?key={key}&q={lat},{lon}&days=3&aqi=no&alerts=no
 *
 * Dùng @SerializedName để map snake_case từ JSON → camelCase Kotlin.
 */
data class WeatherApiResponse(
    @SerializedName("location") val location: LocationInfo,
    @SerializedName("current") val current: CurrentWeather,
    @SerializedName("forecast") val forecast: ForecastResponse
)

data class CurrentWeather(
    @SerializedName("temp_c") val tempC: Double,
    @SerializedName("condition") val condition: WeatherCondition,
    @SerializedName("humidity") val humidity: Int,
    @SerializedName("feelslike_c") val feelsLikeC: Double,
    @SerializedName("wind_kph") val windKph: Double,
    @SerializedName("uv") val uv: Double
)

/**
 * 📍 LocationInfo — Thông tin vị trí từ API WeatherAPI.com.
 *
 * JSON mẫu:
 * {
 *   "name": "Hanoi",
 *   "region": "Hanoi",
 *   "country": "Vietnam",
 *   "localtime": "2024-01-15 10:00"
 * }
 */
data class LocationInfo(
    @SerializedName("name") val name: String,
    @SerializedName("region") val region: String,
    @SerializedName("country") val country: String,
    @SerializedName("localtime") val localtime: String
)

/**
 * 🌤️ WeatherCondition — Trạng thái thời tiết hiện tại.
 */
data class WeatherCondition(
    @SerializedName("text") val text: String,
    @SerializedName("icon") val icon: String
)

data class ForecastResponse(
    @SerializedName("forecastday") val forecastDay: List<ForecastDay>
)

data class ForecastDay(
    @SerializedName("date") val date: String,
    @SerializedName("day") val day: DayWeather,
    @SerializedName("astro") val astro: Astro? = null
)

data class DayWeather(
    @SerializedName("maxtemp_c") val maxTempC: Double,
    @SerializedName("mintemp_c") val minTempC: Double,
    @SerializedName("condition") val condition: WeatherCondition
)

data class Astro(
    @SerializedName("sunrise") val sunrise: String,
    @SerializedName("sunset") val sunset: String
)

/**
 * 🧠 WeatherAnalysis — Weather analysis result used as context for Chatbot.
 *
 * @property label Weather label: "VeryCold", "Cold", "Cool", "Warm", "Hot"
 * @property suggestion Outfit suggestion based on weather (resolved from strings.xml resources).
 */
data class WeatherAnalysis(
    val label: String,
    val suggestion: String = ""
)

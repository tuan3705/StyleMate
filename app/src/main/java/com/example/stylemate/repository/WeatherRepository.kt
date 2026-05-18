package com.example.stylemate.repository

import com.example.stylemate.model.weather.WeatherAnalysis
import com.example.stylemate.model.weather.WeatherApiResponse
import com.example.stylemate.network.RetrofitClient
import com.example.stylemate.network.StylemateApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 🏪 WeatherRepository — Repository Pattern cho module Thời tiết.
 *
 * 🔒 Gọi Backend proxy (/api/weather/forecast), KHÔNG gọi trực tiếp WeatherAPI.com.
 *    -> WEATHER_API_KEY chi nam o server (file .env), khong lo tren Client.
 *
 * ViewModel se goi repository nay de lay du lieu thoi tiet.
 */
class WeatherRepository(
    private val apiService: StylemateApiService = RetrofitClient.stylemateApiService
) {

    /**
     * Lay du lieu thoi tiet + du bao 3 ngay qua Backend proxy.
     *
     * Backend nhan lat+lon, tu them API Key, goi WeatherAPI.com, tra JSON ve.
     *
     * @param lat Vi do.
     * @param lon Kinh do.
     * @return [WeatherApiResponse] neu thanh cong.
     * @throws Exception neu network error hoac API tra ve loi.
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
     * Phan tich nhanh thoi tiet de lam context cho Chatbot.
     *
     * Quy tac don gian dua tren nhiet do:
     *   - < 10°C  -> "VeryCold"
     *   - < 20°C  -> "Cold"
     *   - < 25°C  -> "Cool"
     *   - < 30°C  -> "Warm"
     *   - >= 30°C -> "Hot"
     *
     * @param tempC Nhiet do hien tai (°C).
     * @return [WeatherAnalysis] chua nhan + goi y.
     */
    fun analyzeWeather(tempC: Double): WeatherAnalysis {
        return when {
            tempC < 10 -> WeatherAnalysis(
                label = "VeryCold",
                suggestion = "Troi rat lanh! Hay mac ao khoac day, khan quang co va gang tay."
            )
            tempC < 20 -> WeatherAnalysis(
                label = "Cold",
                suggestion = "Troi lanh. Ao len, ao khoac nhe va quan dai la lua chon phu hop."
            )
            tempC < 25 -> WeatherAnalysis(
                label = "Cool",
                suggestion = "Troi mat me. Ao dai tay, quan jeans hoac chan vay nhe."
            )
            tempC < 30 -> WeatherAnalysis(
                label = "Warm",
                suggestion = "Troi am ap. Ao ngan tay, vay mat, quan vai nhe."
            )
            else -> WeatherAnalysis(
                label = "Hot",
                suggestion = "Troi nong! Quan short, ao ba lo, vay maxi, chat lieu thoang mat."
            )
        }
    }
}

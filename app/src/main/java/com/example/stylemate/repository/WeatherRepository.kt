package com.example.stylemate.repository

import com.example.stylemate.model.weather.WeatherAnalysis
import com.example.stylemate.model.weather.WeatherApiResponse
import com.example.stylemate.network.RetrofitClient
import com.example.stylemate.network.WeatherApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 🏪 WeatherRepository — Repository Pattern cho module Thời tiết.
 *
 * Gọi [WeatherApiService] qua Retrofit, xử lý exception và parse dữ liệu.
 * ViewModel sẽ gọi repository này để lấy dữ liệu thời tiết.
 */
class WeatherRepository(
    private val apiService: WeatherApiService = RetrofitClient.weatherApiService
) {

    /**
     * 🌤️ Lấy dữ liệu thời tiết + dự báo 3 ngày.
     *
     * @param lat Vĩ độ.
     * @param lon Kinh độ.
     * @return [WeatherApiResponse] nếu thành công.
     * @throws Exception nếu network error hoặc API trả về lỗi.
     */
    suspend fun getWeatherForecast(lat: Double, lon: Double): WeatherApiResponse =
        withContext(Dispatchers.IO) {
            val query = "$lat,$lon"
            apiService.getForecast(
                apiKey = RetrofitClient.WEATHER_API_KEY,
                q = query,
                days = 3
            )
        }

    /**
     * 🧠 Phân tích nhanh thời tiết để làm context cho Chatbot.
     *
     * Quy tắc đơn giản dựa trên nhiệt độ:
     *   - < 10°C  → "VeryCold" — "Mặc ấm, áo khoác dày"
     *   - < 20°C  → "Cold"     — "Áo len, áo khoác nhẹ"
     *   - < 25°C  → "Cool"     — "Áo dài tay, quần jeans"
     *   - < 30°C  → "Warm"     — "Áo ngắn tay, váy mát"
     *   - ≥ 30°C  → "Hot"      — "Quần short, áo ba lỗ, đồ thoáng mát"
     *
     * @param tempC Nhiệt độ hiện tại (°C).
     * @return [WeatherAnalysis] chứa nhãn + gợi ý.
     */
    fun analyzeWeather(tempC: Double): WeatherAnalysis {
        return when {
            tempC < 10 -> WeatherAnalysis(
                label = "VeryCold",
                suggestion = "Trời rất lạnh! Hãy mặc áo khoác dày, khăn quàng cổ và găng tay."
            )
            tempC < 20 -> WeatherAnalysis(
                label = "Cold",
                suggestion = "Trời lạnh. Áo len, áo khoác nhẹ và quần dài là lựa chọn phù hợp."
            )
            tempC < 25 -> WeatherAnalysis(
                label = "Cool",
                suggestion = "Trời mát mẻ. Áo dài tay, quần jeans hoặc chân váy nhẹ."
            )
            tempC < 30 -> WeatherAnalysis(
                label = "Warm",
                suggestion = "Trời ấm áp. Áo ngắn tay, váy mát, quần vải nhẹ."
            )
            else -> WeatherAnalysis(
                label = "Hot",
                suggestion = "Trời nóng! Quần short, áo ba lỗ, váy maxi, chất liệu thoáng mát."
            )
        }
    }
}

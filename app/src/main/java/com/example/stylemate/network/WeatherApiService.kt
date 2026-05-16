package com.example.stylemate.network

import com.example.stylemate.model.weather.WeatherApiResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 🌐 WeatherApiService — Retrofit interface cho WeatherAPI.com.
 *
 * Endpoint: GET /v1/forecast.json
 *
 * @param key API key (từ WeatherAPI.com hoặc OpenWeather).
 * @param q Toạ độ dạng "lat,lon" (vd: "21.0285,105.8542" cho Hà Nội).
 * @param days Số ngày dự báo (3 ngày).
 */
interface WeatherApiService {

    @GET("v1/forecast.json")
    suspend fun getForecast(
        @Query("key") apiKey: String,
        @Query("q") q: String, // "lat,lon"
        @Query("days") days: Int = 3,
        @Query("aqi") aqi: String = "no",
        @Query("alerts") alerts: String = "no"
    ): WeatherApiResponse
}

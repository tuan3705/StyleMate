package com.example.stylemate.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 🏗️ RetrofitClient — Singleton quản lý Retrofit instance.
 *
 * Cấu hình:
 * - Base URL: https://api.weatherapi.com (WeatherAPI.com)
 * - Timeout: 30s connect + 30s read
 * - Logging interceptor (debug only)
 */
object RetrofitClient {

    // ⚠️ Thay bằng API key thật của bạn từ https://www.weatherapi.com/
    const val WEATHER_API_KEY = "02adf784ac1545719b963122261605"

    private const val BASE_URL = "https://api.weatherapi.com/"
    private const val TIMEOUT_SECONDS = 30L

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val weatherApiService: WeatherApiService by lazy {
        retrofit.create(WeatherApiService::class.java)
    }
}

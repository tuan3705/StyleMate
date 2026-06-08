package com.example.stylemate.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stylemate.model.weather.ForecastDay
import com.example.stylemate.model.weather.WeatherAnalysis
import com.example.stylemate.model.weather.WeatherApiResponse
import com.example.stylemate.repository.WeatherRepository
import com.example.stylemate.viewmodel.WeatherViewModel
import com.example.stylemate.viewmodel.WeatherViewModelFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.compose.runtime.rememberCoroutineScope
import com.example.stylemate.StyleMateApp
import com.example.stylemate.notification.fetchFcmToken
import kotlinx.coroutines.launch

@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel = viewModel(
        factory = WeatherViewModelFactory(
            WeatherRepository()
        )
    ),
    accountMenu: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as StyleMateApp
    val scope = rememberCoroutineScope()

    val weatherData by viewModel.weatherData.collectAsState()
    val locationName by viewModel.locationName.collectAsState()
    val weatherAnalysis by viewModel.weatherAnalysis.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // ── Trạng thái: đã thử lấy vị trí từ GPS chưa? ──────────────
    var hasAttemptedLocation by remember { mutableStateOf(false) }

    fun syncDeviceLocation(lat: Double, lon: Double) {
        scope.launch {
            val token = fetchFcmToken()
            app.fcmRepository.syncFcmToken(token, lat, lon)
        }
    }

    // ── Yêu cầu quyền vị trí ────────────────────────────────────
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            // Đã cấp quyền → lấy vị trí GPS thực
            getLastKnownLocation(context) { lat, lon ->
                viewModel.fetchWeatherByGps(lat, lon)
                syncDeviceLocation(lat, lon)
            }
        } else {
            // Không cấp quyền → dùng toạ độ mặc định (Hà Nội)
            Toast.makeText(context, "Dùng vị trí mặc định (Hà Nội)", Toast.LENGTH_SHORT).show()
            viewModel.fetchWeather(
                WeatherViewModel.DEFAULT_LAT,
                WeatherViewModel.DEFAULT_LON
            )
            syncDeviceLocation(WeatherViewModel.DEFAULT_LAT, WeatherViewModel.DEFAULT_LON)
        }
        hasAttemptedLocation = true
    }

    // ── Khi màn hình lần đầu hiển thị ───────────────────────────
    LaunchedEffect(Unit) {
        if (!hasAttemptedLocation) {
            val hasFinePermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val hasCoarsePermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (hasFinePermission || hasCoarsePermission) {
                // Đã có quyền → lấy vị trí ngay
                getLastKnownLocation(context) { lat, lon ->
                    viewModel.fetchWeatherByGps(lat, lon)
                    syncDeviceLocation(lat, lon)
                }
                hasAttemptedLocation = true
            } else {
                // Chưa có quyền → yêu cầu
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A237E), // Xanh đậm
                        Color(0xFF4A90D9)  // Xanh nhạt
                    )
                )
            )
    ) {
        when {
            isLoading && weatherData == null -> {
                // Loading lần đầu
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Đang cập nhật thời tiết...", color = Color.White)
                }
            }

            errorMessage != null && weatherData == null -> {
                // Lỗi lần đầu, chưa có dữ liệu
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "⚠️",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorMessage ?: "Lỗi không xác định",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        viewModel.fetchWeather(
                            WeatherViewModel.DEFAULT_LAT,
                            WeatherViewModel.DEFAULT_LON
                        )
                    }) {
                        Text("Thử lại")
                    }
                }
            }

            weatherData != null -> {
                WeatherContent(
                    locationName = locationName,
                    weatherData = weatherData!!,
                    weatherAnalysis = weatherAnalysis,
                    isLoading = isLoading,
                    onRefresh = { viewModel.refresh() },
                    accountMenu = accountMenu
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// 📡 Hàm tiện ích: Lấy vị trí GPS cuối cùng từ LocationManager
// ═════════════════════════════════════════════════════════════════

/**
 * Lấy toạ độ GPS gần nhất (nếu có), fallback về Hà Nội nếu không lấy được.
 *
 * ⚠️ Hàm này chỉ được gọi sau khi đã kiểm tra quyền vị trí ở composable.
 * Dùng @SuppressLint("MissingPermission") để bỏ qua cảnh báo vì
 * quyền đã được kiểm tra ở tầng UI trước khi gọi hàm này.
 */
@SuppressLint("MissingPermission")
private fun getLastKnownLocation(
    context: Context,
    onResult: (lat: Double, lon: Double) -> Unit
) {
    try {
        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Thử lấy location từ tất cả provider đang bật
        var bestLocation: Location? = null
        val providers = locationManager.getProviders(true)

        for (provider in providers) {
            try {
                val location = locationManager.getLastKnownLocation(provider)
                if (location != null &&
                    (bestLocation == null || location.accuracy < bestLocation.accuracy)
                ) {
                    bestLocation = location
                }
            } catch (_: Exception) {
                // Bỏ qua provider lỗi, thử provider tiếp theo
            }
        }

        if (bestLocation != null) {
            onResult(bestLocation.latitude, bestLocation.longitude)
            return
        }

        // Không lấy được → dùng Hà Nội
        onResult(
            WeatherViewModel.DEFAULT_LAT,
            WeatherViewModel.DEFAULT_LON
        )
    } catch (_: Exception) {
        // Lỗi → dùng Hà Nội
        onResult(
            WeatherViewModel.DEFAULT_LAT,
            WeatherViewModel.DEFAULT_LON
        )
    }
}

@Composable
private fun WeatherContent(
    locationName: String,
    weatherData: WeatherApiResponse,
    weatherAnalysis: WeatherAnalysis?,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    accountMenu: @Composable () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        // ── Dòng vị trí ─────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = "Vị trí",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = locationName,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
        }

        // ── Header + Refresh ────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Thời tiết hôm nay",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRefresh, enabled = !isLoading) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Làm mới",
                        tint = Color.White
                    )
                }
                accountMenu()
            }
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(8.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Card: Thời tiết hiện tại ──
        CurrentWeatherCard(weatherData)

        Spacer(modifier = Modifier.height(12.dp))

        // ── Phân tích thời tiết (cho Chatbot context) ──
        if (weatherAnalysis != null) {
            WeatherAnalysisCard(weatherAnalysis)
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ── Dự báo 3 ngày ──
        Text(
            text = "Dự báo 3 ngày tới",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        ForecastRow(weatherData.forecast.forecastDay)
    }
}

@Composable
private fun CurrentWeatherCard(weatherData: WeatherApiResponse) {
    val current = weatherData.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Nhiệt độ chính
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${current.tempC.toInt()}",
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "°C",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            // Trạng thái
            Text(
                text = current.condition.text,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Các chỉ số phụ (hàng ngang)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                WeatherInfoItem(
                    icon = Icons.Filled.Water,
                    label = "Độ ẩm",
                    value = "${current.humidity}%"
                )
                WeatherInfoItem(
                    icon = Icons.Filled.Air,
                    label = "Cảm giác",
                    value = "${current.feelsLikeC.toInt()}°"
                )
                WeatherInfoItem(
                    icon = Icons.Filled.WbSunny,
                    label = "UV",
                    value = "${current.uv.toInt()}"
                )
            }
        }
    }
}

@Composable
private fun WeatherInfoItem(icon: ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun WeatherAnalysisCard(analysis: WeatherAnalysis) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2E7D32).copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when (analysis.label) {
                    "VeryCold" -> "🥶"
                    "Cold" -> "❄️"
                    "Cool" -> "🌤️"
                    "Warm" -> "☀️"
                    "Hot" -> "🔥"
                    else -> "🌡️"
                },
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Phân tích: ${analysis.label}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = analysis.suggestion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun ForecastRow(forecastDays: List<ForecastDay>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(forecastDays) { day ->
            ForecastDayCard(day)
        }
    }
}

@Composable
private fun ForecastDayCard(forecastDay: ForecastDay) {
    val displayDate = try {
        // Parse date string "2024-01-15" bằng SimpleDateFormat (tương thích API 25)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = sdf.parse(forecastDay.date)
        val cal = Calendar.getInstance()
        cal.time = date!!
        when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "T2"
            Calendar.TUESDAY -> "T3"
            Calendar.WEDNESDAY -> "T4"
            Calendar.THURSDAY -> "T5"
            Calendar.FRIDAY -> "T6"
            Calendar.SATURDAY -> "T7"
            Calendar.SUNDAY -> "CN"
            else -> forecastDay.date.takeLast(5)
        }
    } catch (e: Exception) {
        forecastDay.date.takeLast(5) // fallback: "01-15"
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.12f)
        ),
        modifier = Modifier.width(120.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = displayDate,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))

            val emoji = when {
                forecastDay.day.condition.text.contains("Sunny", ignoreCase = true) -> "☀️"
                forecastDay.day.condition.text.contains("Cloud", ignoreCase = true) -> "☁️"
                forecastDay.day.condition.text.contains("Rain", ignoreCase = true) -> "🌧️"
                forecastDay.day.condition.text.contains("Snow", ignoreCase = true) -> "❄️"
                forecastDay.day.condition.text.contains("Clear", ignoreCase = true) -> "🌙"
                else -> "🌤️"
            }
            Text(text = emoji, fontSize = 28.sp)

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = forecastDay.day.condition.text,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row {
                Text(
                    text = "${forecastDay.day.maxTempC.toInt()}°",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "/${forecastDay.day.minTempC.toInt()}°",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}


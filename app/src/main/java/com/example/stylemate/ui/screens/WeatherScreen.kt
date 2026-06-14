package com.example.stylemate.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Water
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stylemate.R
import com.example.stylemate.StyleMateApp
import com.example.stylemate.model.weather.ForecastDay
import com.example.stylemate.model.weather.WeatherAnalysis
import com.example.stylemate.model.weather.WeatherApiResponse
import com.example.stylemate.notification.fetchFcmToken
import com.example.stylemate.repository.WeatherRepository
import com.example.stylemate.ui.common.PermissionRationaleDialog
import com.example.stylemate.ui.common.PermissionSettingsRedirectDialog
import com.example.stylemate.ui.common.AppPermissions
import com.example.stylemate.ui.common.rememberAnyPermissionGranted
import com.example.stylemate.viewmodel.WeatherViewModel
import com.example.stylemate.viewmodel.WeatherViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel = viewModel(
        factory = WeatherViewModelFactory(WeatherRepository())
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

    // ── Permission Rationale State ─────────────────────────────────
    var showLocationRationale by remember { mutableStateOf(false) }
    var showLocationSettingsRedirect by remember { mutableStateOf(false) }
    var showGpsDialog by remember { mutableStateOf(false) }

    val hasLocationPermission = rememberAnyPermissionGranted(AppPermissions.LOCATION)
    var deniedCount by remember { mutableStateOf(0) }

    fun isGpsEnabled(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    fun syncDeviceLocation(lat: Double, lon: Double) {
        scope.launch {
            val token = fetchFcmToken()
            app.fcmRepository.syncFcmToken(token, lat, lon)
        }
    }

    fun fetchWeatherData() {
        getLastKnownLocation(context) { lat, lon ->
            viewModel.fetchWeatherByGps(lat, lon)
            syncDeviceLocation(lat, lon)
        }
    }

    val locationFallbackMessage = remember { context.getString(R.string.location_fallback) }

    fun useDefaultLocation() {
        Toast.makeText(context, locationFallbackMessage, Toast.LENGTH_SHORT).show()
        viewModel.fetchWeather(WeatherViewModel.DEFAULT_LAT, WeatherViewModel.DEFAULT_LON)
        syncDeviceLocation(WeatherViewModel.DEFAULT_LAT, WeatherViewModel.DEFAULT_LON)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            deniedCount = 0
            // Đã có quyền → kiểm tra GPS có bật không?
            if (!isGpsEnabled()) {
                showGpsDialog = true
            } else {
                fetchWeatherData()
            }
        } else {
            deniedCount++
            showLocationSettingsRedirect = true
            useDefaultLocation()
        }
    }

    val shouldShowRationale = remember {
        val activity = context as Activity
        ActivityCompat.shouldShowRequestPermissionRationale(
            activity, Manifest.permission.ACCESS_FINE_LOCATION
        ) || ActivityCompat.shouldShowRequestPermissionRationale(
            activity, Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            deniedCount = 0
            // Đã có quyền → luôn kiểm tra GPS
            if (!isGpsEnabled()) {
                showGpsDialog = true
            } else {
                fetchWeatherData()
            }
        } else if (deniedCount == 0) {
            if (shouldShowRationale) {
                showLocationRationale = true
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    // ── GPS Dialog ─────────────────────────────────────────────────
    if (showGpsDialog) {
        PermissionSettingsRedirectDialog(
            title = stringResource(R.string.location_permission_rationale_title),
            message = stringResource(R.string.gps_required_message),
            icon = Icons.Default.GpsFixed,
            onGoToSettings = {
                showGpsDialog = false
                context.startActivity(
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                )
            },
            onDismiss = {
                showGpsDialog = false
                useDefaultLocation()
            }
        )
    }

    // ── UI ──────────────────────────────────────────────────────────
    val isDarkTheme = isSystemInDarkTheme()
    val weatherBgColors = if (isDarkTheme) {
        listOf(Color(0xFF0D1B2A), Color(0xFF1B3A5C), Color(0xFF1A237E))
    } else {
        listOf(Color(0xFF1A237E), Color(0xFF4A90D9), Color(0xFF87CEEB))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(colors = weatherBgColors)
            )
    ) {
        when {
            isLoading && weatherData == null -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.weather_updating),
                        color = Color.White
                    )
                }
            }

            errorMessage != null && weatherData == null -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "\u274C",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorMessage ?: stringResource(R.string.weather_error_unknown),
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
                        Text(stringResource(R.string.retry_button_weather))
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

    // ── Permission Dialogs (hiển thị ở top-level composable) ──────
    if (showLocationRationale) {
        PermissionRationaleDialog(
            title = stringResource(R.string.location_permission_rationale_title),
            message = stringResource(R.string.location_permission_rationale),
            icon = Icons.Default.GpsFixed,
            onGrant = {
                showLocationRationale = false
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            },
            onDeny = {
                showLocationRationale = false
                useDefaultLocation()
            }
        )
    }

    if (showLocationSettingsRedirect) {
        PermissionSettingsRedirectDialog(
            title = stringResource(R.string.location_permission_rationale_title),
            message = stringResource(R.string.permission_settings_redirect),
            icon = Icons.Default.GpsFixed,
            onGoToSettings = {
                showLocationSettingsRedirect = false
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.fromParts("package", context.packageName, null)
                    }
                )
            },
            onDismiss = { showLocationSettingsRedirect = false }
        )
    }
}

// ═════════════════════════════════════════════════════════════════
// 📡 Lấy vị trí GPS — chỉ gọi sau khi đã xác minh quyền ở trên
// ═════════════════════════════════════════════════════════════════

private fun getLastKnownLocation(
    context: Context,
    onResult: (lat: Double, lon: Double) -> Unit
) {
    try {
        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            onResult(WeatherViewModel.DEFAULT_LAT, WeatherViewModel.DEFAULT_LON)
            return
        }

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
            } catch (_: Exception) { }
        }

        if (bestLocation != null) {
            onResult(bestLocation.latitude, bestLocation.longitude)
            return
        }

        onResult(WeatherViewModel.DEFAULT_LAT, WeatherViewModel.DEFAULT_LON)
    } catch (_: Exception) {
        onResult(WeatherViewModel.DEFAULT_LAT, WeatherViewModel.DEFAULT_LON)
    }
}

// ── UI Components (unchanged) ─────────────────────────────────────

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
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
            Icon(Icons.Filled.LocationOn, contentDescription = stringResource(R.string.location_content_desc),
                tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = locationName, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = stringResource(R.string.weather_today), style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold, color = Color.White)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRefresh, enabled = !isLoading) {
                    Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.refresh_content_desc), tint = Color.White)
                }
                accountMenu()
            }
        }
        if (isLoading) CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(8.dp), color = Color.White, strokeWidth = 2.dp)
        Spacer(modifier = Modifier.height(16.dp))
        CurrentWeatherCard(weatherData)
        Spacer(modifier = Modifier.height(12.dp))
        if (weatherAnalysis != null) { WeatherAnalysisCard(weatherAnalysis); Spacer(modifier = Modifier.height(12.dp)) }
        Text(text = stringResource(R.string.weather_forecast_3_days), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        ForecastRow(weatherData.forecast.forecastDay)
    }
}

@Composable
private fun CurrentWeatherCard(weatherData: WeatherApiResponse) {
    val current = weatherData.current
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f))) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "${current.tempC.toInt()}", fontSize = 72.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = "\u00B0C", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(top = 16.dp))
            }
            Text(text = current.condition.text, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                WeatherInfoItem(Icons.Filled.Water, stringResource(R.string.humidity_label), "${current.humidity}%")
                WeatherInfoItem(Icons.Filled.Air, stringResource(R.string.feels_like_label), "${current.feelsLikeC.toInt()}\u00B0")
                WeatherInfoItem(Icons.Filled.WbSunny, stringResource(R.string.uv_label), "${current.uv.toInt()}")
            }
        }
    }
}

@Composable
private fun WeatherInfoItem(icon: ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = label, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
    }
}

@Composable
private fun WeatherAnalysisCard(analysis: WeatherAnalysis) {
    val context = LocalContext.current
    val suggestionString = remember(analysis.label) {
        val resourceKey = WeatherRepository.getSuggestionResourceKey(analysis.label)
        val resId = context.resources.getIdentifier(resourceKey, "string", context.packageName)
        if (resId != 0) context.getString(resId) else analysis.suggestion
    }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32).copy(alpha = 0.3f))) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = stringResource(id = getWeatherAnalysisEmojiResId(analysis.label)), fontSize = 32.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = stringResource(R.string.weather_analysis_label_format, analysis.label),
                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = suggestionString, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
            }
        }
    }
}

private fun getWeatherAnalysisEmojiResId(label: String): Int = when (label) {
    "VeryCold" -> R.string.weather_emoji_very_cold; "Cold" -> R.string.weather_emoji_cold
    "Cool" -> R.string.weather_emoji_cool; "Warm" -> R.string.weather_emoji_warm; "Hot" -> R.string.weather_emoji_hot
    else -> R.string.weather_emoji_thermometer
}

@Composable
private fun ForecastRow(forecastDays: List<ForecastDay>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
        items(forecastDays) { day -> ForecastDayCard(day) }
    }
}

@Composable
private fun ForecastDayCard(forecastDay: ForecastDay) {
    val displayDate = try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = sdf.parse(forecastDay.date)
        val cal = Calendar.getInstance(); cal.time = date!!
        when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "T2"; Calendar.TUESDAY -> "T3"; Calendar.WEDNESDAY -> "T4"
            Calendar.THURSDAY -> "T5"; Calendar.FRIDAY -> "T6"; Calendar.SATURDAY -> "T7"
            Calendar.SUNDAY -> "CN"; else -> forecastDay.date.takeLast(5)
        }
    } catch (_: Exception) { forecastDay.date.takeLast(5) }

    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
        modifier = Modifier.width(120.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = displayDate, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Icon(painter = painterResource(id = getForecastEmojiResId(forecastDay.day.condition.text)),
                contentDescription = forecastDay.day.condition.text, tint = Color.White, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = forecastDay.day.condition.text, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f), maxLines = 1)
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                Text(text = "${forecastDay.day.maxTempC.toInt()}\u00B0", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = "/${forecastDay.day.minTempC.toInt()}\u00B0", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f))
            }
        }
    }
}

private fun getForecastEmojiResId(conditionText: String): Int = when {
    conditionText.contains("Sunny", ignoreCase = true) -> R.drawable.ic_weather_sunny
    conditionText.contains("Cloud", ignoreCase = true) -> R.drawable.ic_weather_cloudy
    conditionText.contains("Rain", ignoreCase = true) -> R.drawable.ic_weather_rainy
    conditionText.contains("Snow", ignoreCase = true) -> R.drawable.ic_weather_snow
    conditionText.contains("Clear", ignoreCase = true) -> R.drawable.ic_weather_clear_night
    else -> R.drawable.ic_weather_cloudy
}
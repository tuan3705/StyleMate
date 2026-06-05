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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.stylemate.network.RetrofitClient.STYLEMATE_BASE_URL
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stylemate.viewmodel.AIStylistViewModel
import com.example.stylemate.viewmodel.AIStylistUiState
import com.example.stylemate.viewmodel.WeatherViewModel

@Composable
fun AIStylistScreen(
    viewModel: AIStylistViewModel = viewModel(),
    accountMenu: @Composable () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // ── Trạng thái: đã thử lấy vị trí từ GPS chưa? ──────────────
    var hasAttemptedLocation by remember { mutableStateOf(false) }

    // ── Hàm tiện ích gọi refresh với tọa độ GPS ─────────────────
    val refreshWithLocation = {
        getLastKnownLocation(context) { lat, lon ->
            viewModel.refreshWeatherAndRecommendation(lat, lon)
        }
    }

    // ── Yêu cầu quyền vị trí ────────────────────────────────────
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            refreshWithLocation()
        } else {
            Toast.makeText(context, "Dùng vị trí mặc định", Toast.LENGTH_SHORT).show()
            viewModel.refreshWeatherAndRecommendation()
        }
        hasAttemptedLocation = true
    }

    // ── Lần đầu vào màn hình ────────────────────────────────────
    LaunchedEffect(Unit) {
        if (!hasAttemptedLocation) {
            val hasFinePermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val hasCoarsePermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (hasFinePermission || hasCoarsePermission) {
                refreshWithLocation()
                hasAttemptedLocation = true
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

    Scaffold(
        topBar = {
            AIStylistHeader(accountMenu = accountMenu)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                WeatherRecommendationSection(
                    uiState = uiState,
                    onRefresh = { refreshWithLocation() }
                )
            }

            item {
                PopularFeaturesSection()
            }

            item {
                AIDesignerSection()
            }

            item {
                BrandImportSection()
            }

            item {
                RecentlyAddedSection()
            }

            item {
                OutfitCalendarSection()
            }

            item {
                MagazineSection()
            }

            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                    OutlinedButton(
                        onClick = { /* TODO */ },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Chỉnh sửa trang chủ", color = Color.Black)
                    }
                }
            }

            item {
                Text(
                    "Thêm và sắp xếp các mục theo ý muốn",
                    modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun AIStylistHeader(accountMenu: @Composable () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Xin chào, HungBu",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { /* TODO */ }) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = "Calendar")
            }
            IconButton(onClick = { /* TODO */ }) {
                Icon(Icons.Outlined.Notifications, contentDescription = "Notifications")
            }
            accountMenu()
        }
    }
}

@Composable
fun WeatherRecommendationSection(
    uiState: AIStylistUiState,
    onRefresh: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = uiState.recommendationText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Color.Gray)
                } else {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = "Refresh",
                        tint = Color.Gray
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WeatherInfoItem(Icons.Default.CalendarToday, uiState.dateText)
            WeatherInfoItem(Icons.Default.LocationOn, uiState.locationText)
            WeatherInfoItem(Icons.Default.Cloud, uiState.tempText)
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            // Outfits from AI
            if (uiState.suggestedOutfits.isNotEmpty()) {
                items(uiState.suggestedOutfits) { outfit ->
                    Card(
                        modifier = Modifier
                            .width(160.dp)
                            .height(200.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            // Display top and bottom if available, or just the first image
                            val imageUrl = outfit.image_urls?.values?.firstOrNull()
                            if (imageUrl != null) {
                                val fullUrl = if (imageUrl.startsWith("http")) imageUrl else "${STYLEMATE_BASE_URL.removeSuffix("/")}${imageUrl}"
                                AsyncImage(
                                    model = fullUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().padding(16.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Icon(
                                    Icons.Default.Checkroom,
                                    contentDescription = null,
                                    modifier = Modifier.size(80.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }
                }
            } else {
                // Placeholder cards if no AI outfits yet
                items(2) {
                    Card(
                        modifier = Modifier
                            .width(160.dp)
                            .height(200.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Checkroom,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }

            // Card 3: Ask Personal Stylist
            item {
                StylistActionCard()
            }

            // Card 4: See More
            item {
                SeeMoreCard()
            }
        }
    }
}

@Composable
fun StylistActionCard() {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(200.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF0F0F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                Icons.Default.Face, // Replacing custom icon with a suitable Material one
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = Color.Black
            )
            Text(
                text = "Hỏi nhà tạo mẫu cá nhân",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Button(
                onClick = { /* TODO */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF212121)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                Text("Thử ngay", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun SeeMoreCard() {
    Column(
        modifier = Modifier
            .width(100.dp)
            .height(200.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White)
                .padding(1.dp)
                .clip(CircleShape)
                .background(Color.White)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = { /* TODO */ },
                modifier = Modifier
                    .size(40.dp)
                    .border(1.dp, Color.LightGray, CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "See More")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Xem thêm", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}

@Composable
fun WeatherInfoItem(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}

// 📡 Hàm tiện ích: Lấy vị trí GPS cuối cùng
@SuppressLint("MissingPermission")
private fun getLastKnownLocation(
    context: Context,
    onResult: (lat: Double, lon: Double) -> Unit
) {
    try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var bestLocation: Location? = null
        val providers = locationManager.getProviders(true)

        for (provider in providers) {
            try {
                val location = locationManager.getLastKnownLocation(provider)
                if (location != null && (bestLocation == null || location.accuracy < bestLocation.accuracy)) {
                    bestLocation = location
                }
            } catch (_: Exception) {}
        }

        if (bestLocation != null) {
            onResult(bestLocation.latitude, bestLocation.longitude)
        } else {
            onResult(WeatherViewModel.DEFAULT_LAT, WeatherViewModel.DEFAULT_LON)
        }
    } catch (_: Exception) {
        onResult(WeatherViewModel.DEFAULT_LAT, WeatherViewModel.DEFAULT_LON)
    }
}

@Composable
fun PopularFeaturesSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tính năng phổ biến",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = { /* TODO */ }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Xem tất cả", color = Color.Gray)
                    Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureCard(
                modifier = Modifier.weight(1f),
                title = "Thêm món đồ",
                icon = Icons.Default.AddCircle,
                iconColor = Color(0xFF4A90E2)
            )
            FeatureCard(
                modifier = Modifier.weight(1f),
                title = "Tạo trang phục",
                icon = Icons.Default.AccessibilityNew,
                iconColor = Color.Gray
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureCardSmall(modifier = Modifier.weight(1f), title = "Lịch", icon = Icons.Default.CalendarMonth)
            FeatureCardSmall(modifier = Modifier.weight(1f), title = "Tân trang", icon = Icons.Default.AutoAwesome)
            FeatureCardSmall(modifier = Modifier.weight(1f), title = "Thống kê phong cách", icon = Icons.Default.BarChart)
        }
    }
}

@Composable
fun FeatureCard(modifier: Modifier = Modifier, title: String, icon: ImageVector, iconColor: Color) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7FB))
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Text(
                text = title,
                modifier = Modifier.align(Alignment.BottomStart),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp).align(Alignment.TopEnd),
                tint = iconColor
            )
        }
    }
}

@Composable
fun FeatureCardSmall(modifier: Modifier = Modifier, title: String, icon: ImageVector) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7FB))
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Text(
                text = title,
                modifier = Modifier.align(Alignment.BottomStart),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                lineHeight = 14.sp
            )
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp).align(Alignment.TopEnd),
                tint = Color.Gray.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun AIDesignerSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Nhà tạo mẫu AI",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AIDesignerLargeCard(modifier = Modifier.weight(1.2f), title = "Gợi ý trang phục", hasUpdate = true)
            Column(modifier = Modifier.weight(2f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AIDesignerMediumCard(title = "Mua sắm", icon = Icons.Default.ShoppingCart)
                AIDesignerMediumCard(title = "Trò chuyện phong cách", icon = Icons.Default.ChatBubble)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AIIconButton(modifier = Modifier.weight(1f), title = "Tìm màu sắc của tôi", icon = Icons.Default.Palette)
            AIIconButton(modifier = Modifier.weight(1f), title = "Tìm fit của tôi", icon = Icons.Default.PersonOutline)
            AIIconButton(modifier = Modifier.weight(1f), title = "Đánh giá style", icon = Icons.Default.Star)
            AIIconButton(modifier = Modifier.weight(1f), title = "Thử đồ ảo", icon = Icons.Default.ViewInAr)
        }
    }
}

@Composable
fun AIDesignerLargeCard(modifier: Modifier, title: String, hasUpdate: Boolean) {
    Card(
        modifier = modifier.height(160.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7FB))
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            if (hasUpdate) {
                Surface(
                    color = Color.White.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        "Update",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
            Text(
                title,
                modifier = Modifier.align(Alignment.BottomStart),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Icon(
                Icons.Default.Checkroom,
                contentDescription = null,
                modifier = Modifier.size(60.dp).align(Alignment.Center).offset(y = (-10).dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun AIDesignerMediumCard(title: String, icon: ImageVector) {
    Card(
        modifier = Modifier.fillMaxWidth().height(74.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7FB))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp), tint = Color.LightGray)
        }
    }
}

@Composable
fun AIIconButton(modifier: Modifier, title: String, icon: ImageVector) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFF5F7FB)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Text(
            title,
            style = MaterialTheme.typography.labelSmall,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 12.sp,
            maxLines = 2
        )
    }
}

@Composable
fun BrandImportSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Đăng nhập và nhập món đồ từ",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            item { BrandCard(icon = Icons.Default.Email, title = "email") }
            item { BrandLogoCard("GAP") }
            item { BrandLogoCard("SHEIN") }
            item { BrandLogoCard("ZARA") }
            item {
                Box(
                    modifier = Modifier.size(70.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFF5F7FB)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }
        }
    }
}

@Composable
fun BrandCard(icon: ImageVector, title: String) {
    Card(
        modifier = Modifier.size(70.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF0F0F0))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Text(title, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun BrandLogoCard(name: String) {
    Card(
        modifier = Modifier.size(70.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (name == "GAP") Color(0xFF00245D) else Color.White),
        border = if (name != "GAP") androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF0F0F0)) else null
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                name,
                fontWeight = FontWeight.Bold,
                color = if (name == "GAP") Color.White else Color.Black,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun RecentlyAddedSection() {
    // ... (no changes to RecentlyAddedSection body)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Các món đồ đã thêm gần đây",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF5F7FB)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Gray)
                }
            }
            items(5) {
                Card(
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF0F0F0))
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Checkroom, contentDescription = null, tint = Color.LightGray)
                    }
                }
            }
        }
    }
}

@Composable
fun OutfitCalendarSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Lịch trang phục",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = { /* TODO */ }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Xem lịch", color = Color.Gray)
                    Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                }
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val days = listOf("Thứ 2", "Hôm nay", "Thứ 4", "Thứ 5")
            val dates = listOf("25 thg 5", "26 thg 5", "27 thg 5", "28 thg 5")
            val temps = listOf("39° 28°", "41° 28°", "40° 29°", "38° 27°")

            items(days.size) { index ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (days[index] == "Hôm nay") {
                        Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color.Black))
                    } else {
                        Spacer(modifier = Modifier.size(4.dp))
                    }
                    Text(days[index], fontWeight = if (days[index] == "Hôm nay") FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp)
                    Text(dates[index], color = Color.Gray, fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                        Text(temps[index], fontSize = 11.sp, color = Color.Gray)
                    }
                    Card(
                        modifier = Modifier.size(100.dp, 130.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7FB))
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = Color.Gray.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MagazineSection() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Tạp chí",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                MagazineCard(
                    title = "Xây dựng Tủ đồ Capsule từ đồ Second-hand: Bước khởi đầu khó...",
                    date = "25 tháng 5, 2026",
                    isNew = true
                )
            }
            item {
                MagazineCard(
                    title = "Chỉ vì một chiếc áo thun: Cẩm nang về hình tượng...",
                    date = "18 tháng 5, 2026",
                    isNew = false
                )
            }
        }
    }
}

@Composable
fun MagazineCard(title: String, date: String, isNew: Boolean) {
    Column(modifier = Modifier.width(260.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth().height(160.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.LightGray)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.White)
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(date, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            if (isNew) {
                Surface(
                    color = Color(0xFFE3F2FD),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "MỚI",
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF1976D2),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}


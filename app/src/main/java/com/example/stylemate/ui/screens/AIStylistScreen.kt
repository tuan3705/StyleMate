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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.stylemate.R
import com.example.stylemate.model.CalendarEventEntity
import com.example.stylemate.model.ClothingItemEntity
import com.example.stylemate.model.OutfitWithClothingItems
import com.example.stylemate.network.RetrofitClient
import com.example.stylemate.network.RetrofitClient.STYLEMATE_BASE_URL
import com.example.stylemate.repository.CalendarRepository
import com.example.stylemate.repository.OutfitRepository
import com.example.stylemate.ui.components.CategoryIconImage
import com.example.stylemate.ui.components.rememberItemImageModel
import com.example.stylemate.viewmodel.AIStylistViewModel
import com.example.stylemate.viewmodel.AIStylistUiState
import com.example.stylemate.viewmodel.OutfitViewModel
import com.example.stylemate.viewmodel.WeatherViewModel
import kotlinx.coroutines.flow.combine
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.roundToInt

@Composable
fun AIStylistScreen(
    onNavigateToChat: () -> Unit = {},
    onNavigateToPersonalStylist: () -> Unit = {},
    onNavigateToVirtualTryOn: () -> Unit = {},
    onNavigateToAddItem: () -> Unit = {},
    onNavigateToCreateOutfit: () -> Unit = {},
    onNavigateToCalendar: () -> Unit = {},
    onNavigateToEditOutfit: ((String) -> Unit)? = null,
    onNavigateToCalendarDay: ((Long) -> Unit)? = null,
    accountMenu: @Composable () -> Unit = {}
) {
    val app = LocalContext.current.applicationContext as com.example.stylemate.StyleMateApp
    val viewModel: com.example.stylemate.viewmodel.AIStylistViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return com.example.stylemate.viewmodel.AIStylistViewModel(app.authStorage) as T
            }
        }
    )
    val uiState by viewModel.uiState.collectAsState()
    val userName by app.authStorage.userNameFlow.collectAsState(initial = null)
    val userEmail by app.authStorage.userEmailFlow.collectAsState(initial = null)
    val context = LocalContext.current

    var hasAttemptedLocation by remember { mutableStateOf(false) }

    val refreshWithLocation = {
        getLastKnownLocation(context) { lat, lon ->
            viewModel.refreshWeatherAndRecommendation(lat, lon, forceRefresh = true)
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) { refreshWithLocation() }
        else { Toast.makeText(context, context.getString(R.string.ai_stylist_location_default), Toast.LENGTH_SHORT).show(); viewModel.refreshWeatherAndRecommendation() }
        hasAttemptedLocation = true
    }

    LaunchedEffect(Unit) {
        if (!hasAttemptedLocation) {
            val hasFinePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasCoarsePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (hasFinePermission || hasCoarsePermission) { refreshWithLocation(); hasAttemptedLocation = true }
            else { locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AIStylistHeader(userName = userName, userEmail = userEmail, onNavigateToCalendar = onNavigateToCalendar, accountMenu = accountMenu) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item(key = "weather_recommendation") {
                WeatherRecommendationSection(uiState = uiState, onRefresh = { refreshWithLocation() },
                    onNavigateToChat = onNavigateToChat, onNavigateToPersonalStylist = onNavigateToPersonalStylist, onNavigateToEditOutfit = onNavigateToEditOutfit)
            }
            item(key = "popular_features") { PopularFeaturesSection(onAddItem = onNavigateToAddItem, onCreateOutfit = onNavigateToCreateOutfit, onCalendar = onNavigateToCalendar) }
            item(key = "ai_tools") { AIToolsSection(onNavigateToChat = onNavigateToChat, onNavigateToVirtualTryOn = onNavigateToVirtualTryOn) }
            item(key = "outfit_calendar") { BackendOutfitCalendarSection(onNavigateToCalendar = onNavigateToCalendar, onNavigateToOutfit = onNavigateToEditOutfit, onNavigateToCalendarDay = onNavigateToCalendarDay) }
            item(key = "bottom_spacer") { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun AIStylistHeader(userName: String?, userEmail: String?, onNavigateToCalendar: () -> Unit = {}, accountMenu: @Composable () -> Unit = {}) {
    val displayName = userName?.takeIf { it.isNotBlank() } ?: userEmail?.substringBefore('@')?.takeIf { it.isNotBlank() } ?: stringResource(R.string.default_user_name)
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = stringResource(R.string.ai_stylist_greeting, displayName), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateToCalendar) { Icon(Icons.Outlined.CalendarMonth, contentDescription = stringResource(R.string.ai_stylist_calendar_desc)) }
            IconButton(onClick = { }) { Icon(Icons.Outlined.Notifications, contentDescription = stringResource(R.string.ai_stylist_notification_desc)) }
            accountMenu()
        }
    }
}

@Composable
fun WeatherRecommendationSection(uiState: AIStylistUiState, onRefresh: () -> Unit, onNavigateToChat: () -> Unit, onNavigateToPersonalStylist: () -> Unit, onNavigateToEditOutfit: ((String) -> Unit)? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Text(text = uiState.headline, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = onRefresh, modifier = Modifier.padding(start = 8.dp)) {
                if (uiState.isLoading) { CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary) }
                else { Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.ai_stylist_refresh_desc), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WeatherInfoItem(Icons.Default.CalendarToday, uiState.dateText)
            WeatherInfoItem(Icons.Default.LocationOn, uiState.locationText)
            WeatherInfoItem(Icons.Default.Cloud, uiState.tempText)
        }
        if (uiState.recommendationText.isNotBlank()) { Text(text = uiState.recommendationText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth()) }
        Spacer(modifier = Modifier.height(4.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 8.dp)) {
            item(key = "ask_stylist") { StylistActionCard(onNavigate = onNavigateToChat) }
            if (uiState.suggestedOutfits.isNotEmpty()) {
                items(uiState.suggestedOutfits, key = { it.hashCode() }) { outfit ->
                    Card(modifier = Modifier.width(160.dp).height(200.dp), shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))) {
                        val urls = outfit.image_urls?.entries?.sortedBy { it.key }?.map { it.value } ?: emptyList()
                        if (urls.isNotEmpty()) {
                            Row(modifier = Modifier.fillMaxSize().padding(4.dp), horizontalArrangement = Arrangement.Start) {
                                urls.forEach { url ->
                                    val fullUrl = if (url.startsWith("http")) url else "${STYLEMATE_BASE_URL.removeSuffix("/")}${url}"
                                    AsyncImage(model = fullUrl, contentDescription = null, modifier = Modifier.weight(1f).fillMaxHeight().padding(2.dp), contentScale = ContentScale.Fit)
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Checkroom, null, modifier = Modifier.size(60.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StylistActionCard(onNavigate: () -> Unit) {
    Card(onClick = onNavigate, modifier = Modifier.width(180.dp).height(200.dp), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.Face, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = stringResource(R.string.ai_stylist_ask_stylist), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onNavigate, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.ai_stylist_try_now), fontSize = 12.sp) }
        }
    }
}

@Composable
fun SeeMoreCard(onNavigate: () -> Unit) {
    Column(modifier = Modifier.width(100.dp).height(200.dp).clickable { onNavigate() }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.ai_stylist_see_more_desc), modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(stringResource(R.string.ai_stylist_see_more), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun WeatherInfoItem(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@SuppressLint("MissingPermission")
private fun getLastKnownLocation(context: Context, onResult: (lat: Double, lon: Double) -> Unit) {
    try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var bestLocation: Location? = null
        for (provider in locationManager.getProviders(true)) {
            try { val location = locationManager.getLastKnownLocation(provider); if (location != null && (bestLocation == null || location.accuracy < bestLocation.accuracy)) bestLocation = location } catch (_: Exception) {}
        }
        if (bestLocation != null) onResult(bestLocation.latitude, bestLocation.longitude) else onResult(WeatherViewModel.DEFAULT_LAT, WeatherViewModel.DEFAULT_LON)
    } catch (_: Exception) { onResult(WeatherViewModel.DEFAULT_LAT, WeatherViewModel.DEFAULT_LON) }
}

@Composable
fun PopularFeaturesSection(onAddItem: () -> Unit = {}, onCreateOutfit: () -> Unit = {}, onCalendar: () -> Unit = {}) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = stringResource(R.string.ai_stylist_popular_features), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureCard(modifier = Modifier.weight(1f), title = stringResource(R.string.ai_stylist_add_item), icon = Icons.Default.AddCircle, iconColor = Color(0xFF4A90E2), onClick = onAddItem)
            FeatureCard(modifier = Modifier.weight(1f), title = stringResource(R.string.ai_stylist_create_outfit), icon = Icons.Default.AccessibilityNew, iconColor = MaterialTheme.colorScheme.onSurfaceVariant, onClick = onCreateOutfit)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { FeatureCardSmall(modifier = Modifier.weight(1f), title = stringResource(R.string.ai_stylist_calendar), icon = Icons.Default.CalendarMonth, onClick = onCalendar) }
    }
}

@Composable
fun FeatureCard(modifier: Modifier = Modifier, title: String, icon: ImageVector, iconColor: Color, onClick: () -> Unit = {}) {
    Card(modifier = modifier.height(100.dp).clickable { onClick() }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Text(text = title, modifier = Modifier.align(Alignment.BottomStart), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Icon(icon, contentDescription = null, modifier = Modifier.size(40.dp).align(Alignment.TopEnd), tint = iconColor)
        }
    }
}

@Composable
fun FeatureCardSmall(modifier: Modifier = Modifier, title: String, icon: ImageVector, onClick: () -> Unit = {}) {
    Card(modifier = modifier.height(100.dp).clickable { onClick() }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Text(text = title, modifier = Modifier.align(Alignment.BottomStart), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, lineHeight = 14.sp)
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp).align(Alignment.TopEnd), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun AIToolsSection(onNavigateToChat: () -> Unit, onNavigateToVirtualTryOn: () -> Unit = {}) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = stringResource(R.string.ai_stylist_ai_designer), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AIToolsLargeCard(modifier = Modifier.weight(1.2f), title = stringResource(R.string.ai_stylist_style_chat), icon = Icons.Default.ChatBubble, onClick = onNavigateToChat)
            AIToolsLargeCard(modifier = Modifier.weight(1f), title = stringResource(R.string.ai_stylist_virtual_tryon), icon = Icons.Default.ViewInAr, onClick = onNavigateToVirtualTryOn)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIToolsLargeCard(modifier: Modifier, title: String, icon: ImageVector, onClick: () -> Unit = {}) {
    Card(onClick = onClick, modifier = modifier.height(120.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Text(title, modifier = Modifier.align(Alignment.BottomStart), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Icon(icon, contentDescription = null, modifier = Modifier.size(48.dp).align(Alignment.TopEnd), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        }
    }
}

// ─── Calendar Section ───
private data class AIStylistCalendarDay(val date: Long, val event: CalendarEventEntity?, val outfit: OutfitWithClothingItems?)

@Composable
fun BackendOutfitCalendarSection(
    onNavigateToCalendar: () -> Unit = {},
    onNavigateToOutfit: ((String) -> Unit)? = null,
    onNavigateToCalendarDay: ((Long) -> Unit)? = null
) {
    val apiService = RetrofitClient.stylemateApiService
    val calendarRepository = remember(apiService) { CalendarRepository(apiService) }
    val outfitRepository = remember(apiService) { OutfitRepository(apiService) }
    val todayEpoch = remember { aiStylistTodayEpochMidnight() }
    val dates = remember(todayEpoch) { (0 until 4).map { addAiStylistDays(todayEpoch, it) } }

    // ⚡ Force refresh mỗi khi composable được recompose (khi quay lại tab AI Stylist)
    var previewDays by remember { mutableStateOf(dates.map { AIStylistCalendarDay(it, null, null) }) }
    LaunchedEffect(Unit) {
        combine(
            calendarRepository.getEventsBetween(dates.first(), dates.last()),
            outfitRepository.getAllOutfitsWithItems()
        ) { events, outfits ->
            val eventByDate = events.associateBy { it.date }
            val outfitById = outfits.associateBy { it.outfit.id }
            dates.map { date -> AIStylistCalendarDay(date, eventByDate[date], eventByDate[date]?.let { outfitById[it.outfitId] }) }
        }.collect { result -> previewDays = result }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = stringResource(R.string.ai_stylist_outfit_calendar), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            TextButton(onClick = onNavigateToCalendar) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.ai_stylist_view_calendar), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(previewDays.size, key = { "cal_day_${previewDays[it].date}" }) { index ->
                val previewDay = previewDays[index]
                val isToday = isSameAiStylistDay(previewDay.date, todayEpoch)
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isToday) Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    else Spacer(modifier = Modifier.size(4.dp))
                    Text(text = aiStylistDayLabel(previewDay.date, todayEpoch), fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp)
                    Text(aiStylistDateLabel(previewDay.date), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    OutfitCalendarPreviewCard(previewDay = previewDay, onNavigateToOutfit = onNavigateToOutfit, onNavigateToCalendarDay = onNavigateToCalendarDay)
                }
            }
        }
    }
}

@Composable
private fun OutfitCalendarPreviewCard(previewDay: AIStylistCalendarDay, onNavigateToOutfit: ((String) -> Unit)? = null, onNavigateToCalendarDay: ((Long) -> Unit)? = null) {
    Card(modifier = Modifier.size(100.dp, 130.dp).clickable { onNavigateToCalendarDay?.invoke(previewDay.date) },
        shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        val outfit = previewDay.outfit
        if (outfit == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    val placements = remember(outfit) { outfit.clothingItems.toAiStylistPlacements() }
                    if (placements.isEmpty()) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Checkroom, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) } }
                    else {
                        val itemSize = 36.dp; val itemSizePx = with(androidx.compose.ui.platform.LocalDensity.current) { itemSize.toPx() }
                        placements.take(4).forEach { placement ->
                            val maxX = (constraints.maxWidth - itemSizePx).coerceAtLeast(1f)
                            val maxY = (constraints.maxHeight - itemSizePx).coerceAtLeast(1f)
                            Box(modifier = Modifier.offset { IntOffset((placement.posX * maxX).roundToInt(), (placement.posY * maxY).roundToInt()) }.size(itemSize)) { OutfitCalendarItemImage(placement.item) }
                        }
                    }
                }
                Text(text = outfit.outfit.name, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun OutfitCalendarItemImage(item: ClothingItemEntity) {
    val imageModel = rememberItemImageModel(item)
    if (imageModel != null) { SubcomposeAsyncImage(model = imageModel, contentDescription = item.name.ifBlank { item.category }, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit, error = { OutfitCalendarItemFallback(item) }) }
    else { OutfitCalendarItemFallback(item) }
}

@Composable
private fun OutfitCalendarItemFallback(item: ClothingItemEntity) {
    Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) { CategoryIconImage(category = item.category, fontSize = 18.sp) }
}

private fun List<ClothingItemEntity>.toAiStylistPlacements(): List<OutfitViewModel.OutfitItemPlacement> {
    return mapIndexed { index, item ->
        val (x, y) = aiStylistDefaultGridPosition(index)
        OutfitViewModel.OutfitItemPlacement(item, posX = item.canvasPosX.takeIf { it != 0f } ?: x, posY = item.canvasPosY.takeIf { it != 0f } ?: y, scale = 1f)
    }
}

private fun aiStylistDefaultGridPosition(index: Int): Pair<Float, Float> {
    val col = index % 2; val row = index / 2
    return (if (col == 0) 0.08f else 0.58f) to minOf(0.08f + row * 0.42f, 0.72f)
}

private fun aiStylistTodayEpochMidnight(): Long {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun addAiStylistDays(epochMillis: Long, days: Int): Long {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = epochMillis }
    cal.add(Calendar.DAY_OF_MONTH, days); return cal.timeInMillis
}

private fun isSameAiStylistDay(first: Long, second: Long): Boolean {
    val cal1 = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = first }
    val cal2 = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = second }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

@Composable
private fun aiStylistDayLabel(epochMillis: Long, todayEpoch: Long): String {
    if (isSameAiStylistDay(epochMillis, todayEpoch)) return stringResource(R.string.ai_stylist_today)
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = epochMillis }
    val dayNames = stringArrayResource(R.array.day_of_week_names)
    return when (cal.get(Calendar.DAY_OF_WEEK)) { Calendar.MONDAY -> dayNames[0]; Calendar.TUESDAY -> dayNames[1]; Calendar.WEDNESDAY -> dayNames[2]; Calendar.THURSDAY -> dayNames[3]; Calendar.FRIDAY -> dayNames[4]; Calendar.SATURDAY -> dayNames[5]; Calendar.SUNDAY -> dayNames[6]; else -> "" }
}

@Composable
private fun aiStylistDateLabel(epochMillis: Long): String {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = epochMillis }
    val monthNames = stringArrayResource(R.array.month_names)
    return "${cal.get(Calendar.DAY_OF_MONTH)} ${monthNames[cal.get(Calendar.MONTH)]}"
}
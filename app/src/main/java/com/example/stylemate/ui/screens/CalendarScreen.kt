package com.example.stylemate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import com.example.stylemate.ui.components.CategoryIconImage
import com.example.stylemate.ui.components.OutfitCanvasPreview
import com.example.stylemate.viewmodel.OutfitViewModel
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stylemate.R
import com.example.stylemate.model.OutfitWithClothingItems
import com.example.stylemate.repository.CalendarRepository
import com.example.stylemate.repository.OutfitRepository
import com.example.stylemate.viewmodel.CalendarViewModel
import com.example.stylemate.viewmodel.CalendarViewModelFactory
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

// ══════════════════════════════════════════════════════════════════════
// 📅 CalendarScreen — Màn hình Lịch gán bộ đồ vào ngày
// ══════════════════════════════════════════════════════════════════════
//
// 🎯 Cấu trúc:
//   Nửa trên: Horizontal Calendar (thanh vuốt ngang 7 ngày trong tuần)
//   Nửa dưới: Nếu có outfit → Hiển thị Card outfit
//             Nếu chưa có → Nút "Gán bộ đồ" mở BottomSheet
// ══════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    backStackEntry: androidx.navigation.NavBackStackEntry? = null,
    accountMenu: @Composable () -> Unit = {}
) {
    val apiService = com.example.stylemate.network.RetrofitClient.stylemateApiService
    val calendarRepo = CalendarRepository(apiService)
    val outfitRepo = OutfitRepository(apiService)
    val viewModel: CalendarViewModel = viewModel(
        factory = CalendarViewModelFactory(calendarRepo, outfitRepo)
    )

    val uiState by viewModel.uiState.collectAsState()
    val selectedDateMillis = uiState.selectedDate
    val assignedOutfit = uiState.assignedOutfit
    val allOutfits = uiState.allOutfits
    val isLoading = uiState.isLoading

    var showBottomSheet by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val currentCal = remember(selectedDateMillis) { epochToCalendar(selectedDateMillis) }

    // Lắng nghe signal calendar_selected_date từ AI Stylist -> chọn ngày tương ứng
    val dateSignal = backStackEntry?.savedStateHandle?.getStateFlow<Long?>("calendar_selected_date", null)
    val pendingCalendarDate by dateSignal?.collectAsState() ?: remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(pendingCalendarDate) {
        pendingCalendarDate?.let { date ->
            viewModel.selectDate(date)
            backStackEntry?.savedStateHandle?.set("calendar_selected_date", null)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshCalendarData()
    }

    LaunchedEffect(currentCal.get(Calendar.MONTH), currentCal.get(Calendar.YEAR)) {
        viewModel.loadEventsInMonth(currentCal.timeInMillis)
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.calendar_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { showMonthPicker = true }) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = stringResource(R.string.select_month_content_desc), tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { viewModel.selectDate(todayEpochMidnight()) }) {
                    Icon(Icons.Filled.DateRange, contentDescription = stringResource(R.string.today_content_desc), tint = MaterialTheme.colorScheme.primary)
                }
                accountMenu()
            }
        }

        WeekCalendarStrip(
            selectedDateMillis = selectedDateMillis,
            eventsInMonth = uiState.eventsInMonth,
            onDateSelected = { viewModel.selectDate(it) }
        )

        Spacer(Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            when {
                isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                assignedOutfit != null -> AssignedOutfitCard(
                    assignedOutfit,
                    onRemove = { viewModel.removeOutfitFromSelectedDate() },
                    onSelectAnother = {
                        viewModel.refreshOutfits()
                        showBottomSheet = true
                    }
                )
                else -> NoOutfitPlaceholder(onAssign = {
                    viewModel.refreshOutfits()
                    showBottomSheet = true
                })
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { scope.launch { sheetState.hide(); showBottomSheet = false } },
            sheetState = sheetState
        ) {
            OutfitSelectionSheetContent(
                outfits = allOutfits,
                selectedOutfitId = assignedOutfit?.outfit?.id,
                onOutfitSelected = { id ->
                    viewModel.assignOutfitToSelectedDate(id)
                    scope.launch { sheetState.hide(); showBottomSheet = false }
                },
                onDismiss = { scope.launch { sheetState.hide(); showBottomSheet = false } }
            )
        }
    }

    if (showMonthPicker) {
        ModalBottomSheet(onDismissRequest = { showMonthPicker = false }) {
            MonthPickerSheet(
                selectedDateMillis = selectedDateMillis,
                onDaySelected = { epoch ->
                    viewModel.selectDate(epoch)
                    showMonthPicker = false
                },
                onDismiss = { showMonthPicker = false }
            )
        }
    }
}

/**
 * Helper: Tạo Calendar từ epoch midnight (UTC)
 */
private fun epochToCalendar(epochMillis: Long): Calendar {
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = epochMillis
    }
}

/**
 * Helper: Lấy epoch midnight (UTC) hôm nay
 */
private fun todayEpochMidnight(): Long {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

/**
 * Helper: Lấy epoch midnight của thứ Hai trong tuần chứa [epochMillis]
 */
private fun getStartOfWeek(epochMillis: Long): Long {
    val cal = epochToCalendar(epochMillis)
    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

    val diff = when (dayOfWeek) {
        Calendar.SUNDAY -> -6
        else -> 2 - dayOfWeek
    }
    cal.add(Calendar.DAY_OF_MONTH, diff)
    return cal.timeInMillis
}


@Composable
private fun formatMonthYear(epochMillis: Long): String {
    val cal = epochToCalendar(epochMillis)
    val monthNames = com.example.stylemate.R.array.month_names
    val names = androidx.compose.ui.res.stringArrayResource(monthNames)
    return "${names[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.YEAR)}"
}


@Composable
private fun getDayOfWeekName(epochMillis: Long): String {
    val cal = epochToCalendar(epochMillis)
    val dayNames = androidx.compose.ui.res.stringArrayResource(com.example.stylemate.R.array.day_of_week_names)
    return when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> dayNames[0]
        Calendar.TUESDAY -> dayNames[1]
        Calendar.WEDNESDAY -> dayNames[2]
        Calendar.THURSDAY -> dayNames[3]
        Calendar.FRIDAY -> dayNames[4]
        Calendar.SATURDAY -> dayNames[5]
        Calendar.SUNDAY -> dayNames[6]
        else -> ""
    }
}


private fun getDayOfMonth(epochMillis: Long): Int {
    return epochToCalendar(epochMillis).get(Calendar.DAY_OF_MONTH)
}


private fun addDays(epochMillis: Long, days: Int): Long {
    val cal = epochToCalendar(epochMillis)
    cal.add(Calendar.DAY_OF_MONTH, days)
    return cal.timeInMillis
}


private fun isSameDay(epoch1: Long, epoch2: Long): Boolean {
    val cal1 = epochToCalendar(epoch1)
    val cal2 = epochToCalendar(epoch2)
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

@Composable
private fun WeekCalendarStrip(
    selectedDateMillis: Long,
    eventsInMonth: Set<Long> = emptySet(),
    onDateSelected: (Long) -> Unit
) {
    val todayEpoch = remember { todayEpochMidnight() }

    val startOfWeek = remember(selectedDateMillis) {
        getStartOfWeek(selectedDateMillis)
    }

    val weekDays = remember(startOfWeek) {
        (0..6).map { addDays(startOfWeek, it) }
    }

    Column {
        Text(
            text = formatMonthYear(selectedDateMillis),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            weekDays.forEach { dayEpoch ->
                val isSelected = isSameDay(dayEpoch, selectedDateMillis)
                val isToday = isSameDay(dayEpoch, todayEpoch)
                val hasOutfit = eventsInMonth.contains(dayEpoch)

                DayCell(
                    dayEpoch = dayEpoch,
                    isSelected = isSelected,
                    isToday = isToday,
                    hasOutfit = hasOutfit,
                    onClick = { onDateSelected(dayEpoch) }
                )
            }
        }
    }
}

@Composable
private fun DayCell(
    dayEpoch: Long,
    isSelected: Boolean,
    isToday: Boolean,
    hasOutfit: Boolean = false,
    onClick: () -> Unit
) {
    val dayName = getDayOfWeekName(dayEpoch)
    val dayNumber = getDayOfMonth(dayEpoch)

    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = dayName,
            style = MaterialTheme.typography.labelSmall,
            color = textColor.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$dayNumber",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
        Spacer(modifier = Modifier.height(2.dp))
        if (hasOutfit) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.primary
                    )
            )
        } else {
            Spacer(modifier = Modifier.size(5.dp))
        }
    }
}



private fun itemsToPlacements(items: List<com.example.stylemate.model.ClothingItemEntity>): List<OutfitViewModel.OutfitItemPlacement> {
    return items.mapIndexed { index, item ->
        OutfitViewModel.OutfitItemPlacement(
            item = item,
            posX = item.canvasPosX.takeIf { it != 0f } ?: defaultGridPosition(index).first,
            posY = item.canvasPosY.takeIf { it != 0f } ?: defaultGridPosition(index).second,
            scale = 1f
        )
    }
}

/** Vị trí grid mặc định cho items (giống OutfitViewModel.defaultGridPosition) */
private fun defaultGridPosition(index: Int): Pair<Float, Float> {
    val col = index % 2
    val row = index / 2
    val x = if (col == 0) 0.1f else 0.55f
    val y = minOf(0.1f + row * 0.25f, 0.8f)
    return x to y
}


@Composable
private fun AssignedOutfitCard(
    outfitWithItems: OutfitWithClothingItems,
    onRemove: () -> Unit,
    onSelectAnother: () -> Unit
) {
    val outfit = outfitWithItems.outfit
    val items = outfitWithItems.clothingItems
    val placements = remember(items) { itemsToPlacements(items) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header: Icon + Tên outfit + Nút xoá
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar icon
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CategoryIconImage(category = items.firstOrNull()?.category ?: "Other", fontSize = 24.sp)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = outfit.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                Text(
                        text = stringResource(R.string.outfit_items_count, items.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.delete_outfit_button),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                        text = stringResource(R.string.items_in_outfit_label),
                        style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutfitCanvasPreview(
                    items = placements,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                    onClick = onSelectAnother,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.choose_other_outfit_button))
                }
        }
    }
}

@Composable
private fun OutfitItemMiniCard(
    item: com.example.stylemate.model.ClothingItemEntity,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = when (item.category) {
            "Tops" -> Color(0xFF42A5F5).copy(alpha = 0.15f)
            "Bottoms" -> Color(0xFF66BB6A).copy(alpha = 0.15f)
            "Dresses" -> Color(0xFFEC407A).copy(alpha = 0.15f)
            "Footwear" -> Color(0xFF8D6E63).copy(alpha = 0.15f)
            else -> Color(0xFFBDBDBD).copy(alpha = 0.15f)
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CategoryIconImage(category = item.category, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.name.ifBlank { item.category },
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}



@Composable
private fun NoOutfitPlaceholder(onAssign: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            //Text(text = "📅", fontSize = 56.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                        text = stringResource(R.string.no_outfit_assigned),
                        style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                        text = stringResource(R.string.assign_outfit_hint),
                        style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onAssign,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.assign_outfit_button), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}


@Composable
private fun OutfitSelectionSheetContent(
    outfits: List<OutfitWithClothingItems>,
    selectedOutfitId: String?,
    onOutfitSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                        text = stringResource(R.string.select_outfit_title),
                        style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close_content_desc_calendar))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (outfits.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "🧥", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.no_outfits_in_closet),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val context = LocalContext.current
                    Text(
                        text = context.getString(R.string.create_outfit_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(outfits, key = { it.outfit.id }) { outfitWithItems ->
                    val isSelected = outfitWithItems.outfit.id == selectedOutfitId
                    OutfitSelectionItem(
                        outfitWithItems = outfitWithItems,
                        isSelected = isSelected,
                        onClick = { onOutfitSelected(outfitWithItems.outfit.id) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun OutfitSelectionItem(
    outfitWithItems: OutfitWithClothingItems,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val outfit = outfitWithItems.outfit
    val items = outfitWithItems.clothingItems

    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val borderWidth = if (isSelected) 2.dp else 0.dp
    val placements = remember(items) { itemsToPlacements(items) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CategoryIconImage(category = items.firstOrNull()?.category ?: "Other", fontSize = 20.sp)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = outfit.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.outfit_items_count, items.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isSelected) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = stringResource(R.string.selected_outfit_content_desc),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                OutfitCanvasPreview(
                    items = placements,
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
            }
        }
    }
}


@Composable
private fun MonthPickerSheet(
    selectedDateMillis: Long,
    onDaySelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var year by remember { mutableStateOf(epochToCalendar(selectedDateMillis).get(Calendar.YEAR)) }
    var month by remember { mutableStateOf(epochToCalendar(selectedDateMillis).get(Calendar.MONTH)) }
    val today = remember { todayEpochMidnight() }
    val daysInMonth = remember(year, month) { generateDaysInMonth(year, month) }

    val monthNames = com.example.stylemate.R.array.month_names
    val rawMonthNames = androidx.compose.ui.res.stringArrayResource(monthNames)
    val rawDayHeader = androidx.compose.ui.res.stringArrayResource(com.example.stylemate.R.array.day_of_week_names)
    val dayHeader = rawDayHeader.toList()

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        // Header: thang + nam + nut dong
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${rawMonthNames[month]} $year", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close_content_desc_calendar)) }
        }

        // Dieu huong thang
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { month--; if (month < 0) { month = 11; year-- } }) {
                Icon(Icons.Filled.ChevronLeft, stringResource(R.string.prev_month_content_desc), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            TextButton(onClick = {
                val t = epochToCalendar(today)
                year = t.get(Calendar.YEAR)
                month = t.get(Calendar.MONTH)
                }) { Text(stringResource(R.string.today_button), color = MaterialTheme.colorScheme.primary) }
            Spacer(Modifier.width(12.dp))
            IconButton(onClick = { month++; if (month > 11) { month = 0; year++ } }) {
                Icon(Icons.Filled.ChevronRight, stringResource(R.string.next_month_content_desc), tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Hang thu
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            dayHeader.forEach { d ->
                Text(d, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium,
                    color = Color.Gray, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
            }
        }

        Spacer(Modifier.height(4.dp))

        // Luoi ngay
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.heightIn(max = 350.dp)
        ) {
            items(daysInMonth) { dayEpoch ->
                val num = getDayOfMonth(dayEpoch)
                val isT = isSameDay(dayEpoch, today)
                val isS = isSameDay(dayEpoch, selectedDateMillis)

                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                        .background(when { isS -> MaterialTheme.colorScheme.primary; isT -> MaterialTheme.colorScheme.primaryContainer; else -> Color.Transparent })
                        .clickable { onDaySelected(dayEpoch) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("$num", style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isS || isT) FontWeight.Bold else FontWeight.Normal,
                        color = when { isS -> MaterialTheme.colorScheme.onPrimary; isT -> MaterialTheme.colorScheme.onPrimaryContainer; else -> Color.DarkGray },
                        textAlign = TextAlign.Center)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

private fun generateDaysInMonth(year: Int, month: Int): List<Long> {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val diff = when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.SUNDAY -> 6
        else -> cal.get(Calendar.DAY_OF_WEEK) - 2
    }
    val temp = cal.clone() as Calendar
    temp.add(Calendar.DAY_OF_MONTH, -diff)
    return (0 until 42).map { val t = temp.timeInMillis; temp.add(Calendar.DAY_OF_MONTH, 1); t }
}

// --- Previews ---

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CalendarScreenPreview() {
    CalendarScreen()
}

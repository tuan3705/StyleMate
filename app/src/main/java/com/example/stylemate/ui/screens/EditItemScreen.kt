package com.example.stylemate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.stylemate.R
import com.example.stylemate.model.ClothingItemEntity
import com.example.stylemate.model.OutfitWithClothingItems
import com.example.stylemate.repository.ClothingRepository
import com.example.stylemate.repository.ImageProcessingRepository
import com.example.stylemate.repository.OutfitRepository
import com.example.stylemate.ui.common.ImagePickerSection
import com.example.stylemate.ui.common.rememberImagePickerState
import com.example.stylemate.ui.common.resolveImageData
import com.example.stylemate.ui.components.CategoryIconImage
import com.example.stylemate.ui.components.getCategoryColor
import com.example.stylemate.viewmodel.ImageProcessingViewModel
import com.example.stylemate.viewmodel.ImageProcessingViewModelFactory
import com.example.stylemate.viewmodel.ItemEditViewModel
import com.example.stylemate.viewmodel.ItemEditViewModelFactory
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditItemScreen(
    navController: NavController,
    itemId: String
) {
    val context = LocalContext.current
    val apiService = com.example.stylemate.network.RetrofitClient.stylemateApiService
    val clothingRepo = ClothingRepository(apiService, context)
    val outfitRepo = OutfitRepository(apiService)
    val viewModel: ItemEditViewModel = viewModel(
        factory = ItemEditViewModelFactory(clothingRepo, outfitRepo)
    )

    val imageProcessingRepository = ImageProcessingRepository(apiService, context)
    val imageProcessingViewModel: ImageProcessingViewModel = viewModel(
        factory = ImageProcessingViewModelFactory(imageProcessingRepository)
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val relevantOutfits by viewModel.relevantOutfits.collectAsStateWithLifecycle()
    val saveSuccess by viewModel.saveSuccess.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val relevantCardWidth = (LocalConfiguration.current.screenWidthDp * (2f / 3f)).dp

    val imagePickerState = rememberImagePickerState(
        onRemoveBackground = {},
        onError = { message ->
            scope.launch { snackbarHostState.showSnackbar(message) }
        }
    )
    val pickedImagePath by imagePickerState.imagePath
    val removeBgState by imageProcessingViewModel.removeBgState.collectAsStateWithLifecycle()
    val aiFillState by imageProcessingViewModel.aiFillState.collectAsStateWithLifecycle()
    val autoTaggingState by imageProcessingViewModel.autoTaggingState.collectAsStateWithLifecycle()

    val savedNoBg = uiState.imageNoBg.takeIf { it.isNotBlank() }
    val displayImagePath = pickedImagePath
        ?: savedNoBg
        ?: uiState.imageOriginal
    // Path da xoa nen trong phien hien tai (sau khi bam Remove BG)
    var sessionNoBgPath by remember { mutableStateOf<String?>(null) }
    val isBgRemoved = displayImagePath.isNotBlank() &&
        (displayImagePath == savedNoBg || displayImagePath == sessionNoBgPath)
    val canRemoveBackground = displayImagePath.isNotBlank() && !isBgRemoved


    // Pre-resolve strings for non-composable usage
    val strSelectImageError = remember { context.getString(R.string.please_select_image_error) }
    val strTagging = remember { context.getString(R.string.tagging_label) }
    val strAutoTagging = remember { context.getString(R.string.auto_tagging_label) }
    val strFilling = remember { context.getString(R.string.filling_label) }
    val strFillWithAi = remember { context.getString(R.string.fill_with_ai_label) }
    val strSaving = remember { context.getString(R.string.saving_label) }
    val strSaveChanges = remember { context.getString(R.string.save_changes) }
    val strChon = remember { context.getString(R.string.select_date_button) }
    val strHuy = remember { context.getString(R.string.cancel_button) }
    val strEnterColorHint = remember { context.getString(R.string.enter_color_hint) }
    val strEnterItemNameHint = remember { context.getString(R.string.enter_item_name_hint) }
    val strEnterBrandHint = remember { context.getString(R.string.enter_brand_hint) }
    val strEnterPriceHint = remember { context.getString(R.string.enter_price_hint) }
    val strSelectCategoryHint = remember { context.getString(R.string.select_category_hint) }
    val strTops = remember { context.getString(R.string.category_tops) }
    val strBottoms = remember { context.getString(R.string.category_bottoms) }
    val strDresses = remember { context.getString(R.string.category_dresses) }
    val strFootwear = remember { context.getString(R.string.category_footwear) }
    val strBags = remember { context.getString(R.string.category_bags) }
    val strAccessories = remember { context.getString(R.string.category_accessories) }
    val strJewelry = remember { context.getString(R.string.category_jewelry) }
    val strSpring = remember { context.getString(R.string.season_spring) }
    val strSummer = remember { context.getString(R.string.season_summer) }
    val strAutumn = remember { context.getString(R.string.season_autumn) }
    val strWinter = remember { context.getString(R.string.season_winter) }
    val strCasual = remember { context.getString(R.string.occasion_casual) }
    val strWork = remember { context.getString(R.string.occasion_work) }
    val strSports = remember { context.getString(R.string.occasion_sports) }
    val strFormal = remember { context.getString(R.string.occasion_formal) }

    val categories = listOf(strTops, strBottoms, strDresses, strFootwear, strBags, strAccessories, strJewelry)
    val seasons = listOf(strSpring, strSummer, strAutumn, strWinter)
    val occasions = listOf(strCasual, strWork, strSports, strFormal)

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val purchaseDateText = remember(uiState.purchaseDate) {
        dateFormat.format(Date(uiState.purchaseDate))
    }

    val onRemoveBackgroundClick = {
        val currentPath = pickedImagePath ?: uiState.imageOriginal
        if (currentPath.isBlank()) {
            scope.launch { snackbarHostState.showSnackbar(strSelectImageError) }
        } else {
            imageProcessingViewModel.removeBackground(currentPath)
        }
        Unit
    }

    LaunchedEffect(itemId) { viewModel.loadItem(itemId) }
    LaunchedEffect(pickedImagePath) {
        if (pickedImagePath != null) {
            viewModel.updateImagePath(pickedImagePath)
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }
    LaunchedEffect(removeBgState.resultPath) {
        val newPath = removeBgState.resultPath
        if (!newPath.isNullOrBlank()) {
            imagePickerState.setImagePath(newPath)
            imageProcessingViewModel.clearResult()
            viewModel.saveRemovedBackground(newPath)
            sessionNoBgPath = newPath
        }
    }
    LaunchedEffect(removeBgState.errorMessage) {
        val message = removeBgState.errorMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
            imageProcessingViewModel.clearResult()
        }
    }
    LaunchedEffect(aiFillState.suggestion) {
        val suggestion = aiFillState.suggestion
        if (suggestion != null) {
            suggestion.category?.let { viewModel.updateCategory(it) }
            suggestion.color?.let { viewModel.updateColor(it) }
            suggestion.name?.let { viewModel.updateName(it) }
            imageProcessingViewModel.clearAiFillResult()
        }
    }
    LaunchedEffect(aiFillState.errorMessage) {
        val message = aiFillState.errorMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
            imageProcessingViewModel.clearAiFillResult()
        }
    }
    LaunchedEffect(autoTaggingState.suggestion) {
        val suggestion = autoTaggingState.suggestion
        if (suggestion != null) {
            suggestion.season?.let { viewModel.updateSeason(it) }
            suggestion.occasion?.let { viewModel.updateOccasion(it) }
            imageProcessingViewModel.clearAutoTaggingResult()
        }
    }
    LaunchedEffect(autoTaggingState.errorMessage) {
        val message = autoTaggingState.errorMessage
        if (!message.isNullOrBlank()) {
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
            imageProcessingViewModel.clearAutoTaggingResult()
        }
    }
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            navController.previousBackStackEntry?.savedStateHandle?.set("refresh_items", true)
            viewModel.clearSaveSuccess()
            navController.popBackStack()
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var expandedMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.edit_item_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_content_desc))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) { CircularProgressIndicator() }
                return@Column
            }

            ImagePickerSection(
                title = stringResource(R.string.item_image_label),
                imagePath = displayImagePath,
                onCameraClick = imagePickerState.onCameraClick,
                onGalleryClick = imagePickerState.onGalleryClick,
                onRemoveBgClick = onRemoveBackgroundClick,
                isProcessing = removeBgState.isProcessing,
                canRemoveBg = canRemoveBackground
            )

            Text(stringResource(R.string.relevant_outfits_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (relevantOutfits.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_outfit_using_item),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(relevantOutfits, key = { it.outfit.id }) { outfit ->
                        RelevantOutfitPreviewCard(
                            outfitWithItems = outfit,
                            modifier = Modifier.width(relevantCardWidth)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    stringResource(R.string.item_details_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = {
                            val currentPath = pickedImagePath ?: uiState.imageOriginal
                            if (currentPath.isBlank()) {
                                scope.launch { snackbarHostState.showSnackbar(strSelectImageError) }
                            } else {
                                imageProcessingViewModel.autoTagging(currentPath)
                            }
                        },
                        enabled = !autoTaggingState.isProcessing,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        if (autoTaggingState.isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(strTagging)
                        } else {
                            Text(strAutoTagging, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            val currentPath = pickedImagePath ?: uiState.imageOriginal
                            if (currentPath.isBlank()) {
                                scope.launch { snackbarHostState.showSnackbar(strSelectImageError) }
                            } else {
                                imageProcessingViewModel.fillWithAi(currentPath)
                            }
                        },
                        enabled = !aiFillState.isProcessing,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        if (aiFillState.isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(strFilling)
                        } else {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(strFillWithAi, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            Text(stringResource(R.string.category_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            ExposedDropdownMenuBox(expanded = expandedMenu, onExpandedChange = { expandedMenu = !expandedMenu }) {
                OutlinedTextField(
                    value = uiState.category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(strSelectCategoryHint) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMenu) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    shape = MaterialTheme.shapes.small
                )
                ExposedDropdownMenu(expanded = expandedMenu, onDismissRequest = { expandedMenu = false }) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = { viewModel.updateCategory(cat); expandedMenu = false }
                        )
                    }
                }
            }

            Text(stringResource(R.string.color_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = uiState.color,
                onValueChange = viewModel::updateColor,
                label = { Text(strEnterColorHint) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = MaterialTheme.shapes.small
            )

            HorizontalDivider()

            Text(stringResource(R.string.item_name_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::updateName,
                label = { Text(strEnterItemNameHint) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = MaterialTheme.shapes.small
            )

            Text(stringResource(R.string.brand_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = uiState.brand,
                onValueChange = viewModel::updateBrand,
                label = { Text(strEnterBrandHint) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = MaterialTheme.shapes.small
            )

            Text(stringResource(R.string.price_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = uiState.price,
                onValueChange = viewModel::updatePrice,
                label = { Text(strEnterPriceHint) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                shape = MaterialTheme.shapes.small
            )

            Text(stringResource(R.string.season_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                seasons.forEach { season ->
                    FilterChip(
                        selected = uiState.season == season,
                        onClick = { viewModel.updateSeason(if (uiState.season == season) "" else season) },
                        label = { Text(season) },
                        shape = MaterialTheme.shapes.small
                    )
                }
            }

            Text(stringResource(R.string.occasion_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                occasions.forEach { occasion ->
                    FilterChip(
                        selected = uiState.occasion == occasion,
                        onClick = { viewModel.updateOccasion(if (uiState.occasion == occasion) "" else occasion) },
                        label = { Text(occasion) },
                        shape = MaterialTheme.shapes.small
                    )
                }
            }

            Text(stringResource(R.string.purchase_date_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedCard(modifier = Modifier.fillMaxWidth(), onClick = { showDatePicker = true }, shape = MaterialTheme.shapes.small) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(text = stringResource(R.string.tap_to_select_date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = purchaseDateText, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Button(
                onClick = { viewModel.saveChanges() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !uiState.isSaving && uiState.category.isNotBlank() && uiState.color.isNotBlank(),
                shape = MaterialTheme.shapes.small
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(strSaving)
                } else {
                    Text(strSaveChanges, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.purchaseDate,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= System.currentTimeMillis()
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.updatePurchaseDate(it) }
                    showDatePicker = false
                }) { Text(strChon) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(strHuy) }
            }
        ) { DatePicker(state = datePickerState) }
    }
}

private data class OutfitPreviewItem(
    val item: ClothingItemEntity,
    val posX: Float,
    val posY: Float,
    val scale: Float,
    val rotation: Float = 0f,
    val flipX: Boolean = false
)

@Composable
private fun RelevantOutfitPreviewCard(
    outfitWithItems: OutfitWithClothingItems,
    modifier: Modifier = Modifier
) {
    val outfit = outfitWithItems.outfit
    val items = outfitWithItems.clothingItems
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val formattedDate = remember(outfit.createdAt) { dateFormat.format(Date(outfit.createdAt)) }
    // ⚡ Dùng vị trí/scale có sẵn trong clothingItems (canvasPosX/Y/Scale) — KHÔNG gọi API riêng
    // cho từng card (tránh N+1 + độ trễ load). Dữ liệu đã được load cùng danh sách relevant outfits.
    val previewPlacements = remember(items) { mapClothingItemsToPreviewPositions(items) }

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                    CategoryIconImage(category = items.firstOrNull()?.category ?: "Other", fontSize = 20.sp)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = outfit.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = "\uD83D\uDCC5 $formattedDate \u00B7 ${items.size} items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
            RelevantOutfitCanvasPreview(items = previewPlacements, modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun mapClothingItemsToPreviewPositions(items: List<ClothingItemEntity>): List<OutfitPreviewItem> {
    if (items.isEmpty()) return emptyList()
    val hasCustomPos = items.any { it.canvasPosX != 0f || it.canvasPosY != 0f }
    return if (hasCustomPos) {
        items.map { OutfitPreviewItem(it, it.canvasPosX, it.canvasPosY, it.canvasScale, it.canvasRotation, it.canvasFlipX) }
    } else {
        items.mapIndexed { index, item ->
            val (x, y) = defaultOutfitGridPosition(index)
            OutfitPreviewItem(item, x, y, item.canvasScale, item.canvasRotation, item.canvasFlipX)
        }
    }
}

private fun defaultOutfitGridPosition(index: Int): Pair<Float, Float> {
    val col = index % 2; val row = index / 2
    val x = if (col == 0) 0.1f else 0.55f
    val y = (0.1f + row * 0.25f).coerceAtMost(0.8f)
    return x to y
}

@Composable
private fun RelevantOutfitCanvasPreview(items: List<OutfitPreviewItem>, modifier: Modifier = Modifier) {
    val density = LocalDensity.current

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F2EA))
    ) {
        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                Text(text = stringResource(R.string.no_items_in_outfit), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Card
        }
        // Đồng dạng với editor & preview tab Outfit: cap chiều cao, canvas con giữ aspect ratio
        // OUTFIT_CANVAS_ASPECT_RATIO + item = OUTFIT_ITEM_SIZE_FRACTION * bề rộng canvas.
        Box(
            modifier = Modifier.fillMaxWidth().height(160.dp),
            contentAlignment = Alignment.Center
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxHeight().aspectRatio(OUTFIT_CANVAS_ASPECT_RATIO)) {
                val canvasWidth = constraints.maxWidth.toFloat()
                val canvasHeight = constraints.maxHeight.toFloat()
                val itemSizePx = canvasWidth * OUTFIT_ITEM_SIZE_FRACTION
                items.forEach { placement ->
                    val imageRequest = rememberOutfitItemImageRequest(placement.item)
                    val scaledSizePx = itemSizePx * placement.scale
                    val itemSize = with(density) { scaledSizePx.toDp() }
                    val maxX = (canvasWidth - scaledSizePx).coerceAtLeast(1f)
                    val maxY = (canvasHeight - scaledSizePx).coerceAtLeast(1f)
                    val offsetX = (placement.posX * maxX).roundToInt()
                    val offsetY = (placement.posY * maxY).roundToInt()
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(offsetX, offsetY) }
                            .size(itemSize)
                            .graphicsLayer {
                                rotationZ = placement.rotation
                                scaleX = if (placement.flipX) -1f else 1f
                            }
                    ) {
                        if (imageRequest != null) {
                            AsyncImage(model = imageRequest, contentDescription = placement.item.name.ifBlank { placement.item.category }, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                        } else {
                            Box(modifier = Modifier.fillMaxSize().background(getCategoryColor(placement.item.category).copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                                CategoryIconImage(category = placement.item.category, fontSize = 24.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberOutfitItemImageRequest(item: ClothingItemEntity): ImageRequest? {
    val context = LocalContext.current
    return remember(item.imageOriginal, item.imageNoBg) {
        val data = resolveImageData(context, item.imageNoBg) ?: resolveImageData(context, item.imageOriginal)
        data?.let { ImageRequest.Builder(context).data(it).crossfade(true).build() }
    }
}

private fun getCategoryColor(category: String): Color = when (category) {
    "Tops" -> Color(0xFF42A5F5)
    "Bottoms" -> Color(0xFF66BB6A)
    "Dresses" -> Color(0xFFEC407A)
    "Footwear" -> Color(0xFF8D6E63)
    "Bags" -> Color(0xFFAB47BC)
    "Accessories" -> Color(0xFFFFA726)
    "Jewelry" -> Color(0xFFD4E157)
    else -> Color(0xFFBDBDBD)
}

private fun getCategoryIcon(category: String): String = when (category) {
    "Tops" -> "\uD83D\uDC55"  // 👕
    "Bottoms" -> "\uD83D\uDC56" // 👖
    "Dresses" -> "\uD83D\uDC57" // 👗
    "Footwear" -> "\uD83D\uDC5F" // 👟
    "Bags" -> "\uD83D\uDC5C"   // 👜
    "Accessories" -> "\u231A"  // ⌚
    "Jewelry" -> "\uD83D\uDC8D" // 💍
    else -> "\uD83E\uDDE5"    // 🧥
}

// --- Previews ---

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun EditItemScreenPreview() {
    EditItemScreen(navController = rememberNavController(), itemId = "preview_item_id")
}

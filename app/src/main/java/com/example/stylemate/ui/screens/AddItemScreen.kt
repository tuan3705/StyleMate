package com.example.stylemate.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.stylemate.R
import com.example.stylemate.repository.ClothingRepository
import com.example.stylemate.repository.ImageProcessingRepository
import com.example.stylemate.ui.common.ImagePickerSection
import com.example.stylemate.ui.common.rememberImagePickerState
import com.example.stylemate.viewmodel.ClothingViewModel
import com.example.stylemate.viewmodel.ClothingViewModelFactory
import com.example.stylemate.viewmodel.ImageProcessingViewModel
import com.example.stylemate.viewmodel.ImageProcessingViewModelFactory
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(navController: NavController) {
    val context = LocalContext.current
    val apiService = com.example.stylemate.network.RetrofitClient.stylemateApiService
    val repository = ClothingRepository(apiService, context)
    val viewModel: ClothingViewModel = viewModel(
        factory = ClothingViewModelFactory(repository)
    )

    val imageProcessingRepository = ImageProcessingRepository(apiService, context)
    val imageProcessingViewModel: ImageProcessingViewModel = viewModel(
        factory = ImageProcessingViewModelFactory(imageProcessingRepository)
    )

    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val aiFillState by imageProcessingViewModel.aiFillState.collectAsState()
    val autoTaggingState by imageProcessingViewModel.autoTaggingState.collectAsState()

    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val imagePickerState = rememberImagePickerState(
        onRemoveBackground = {},
        onError = { message ->
            scope.launch { snackbarHostState.showSnackbar(message) }
        }
    )
    val imagePath by imagePickerState.imagePath
    val removeBgState by imageProcessingViewModel.removeBgState.collectAsState()
    var lastRemoveBgPath by remember { mutableStateOf<String?>(null) }

    // Pre-resolve strings for non-composable usage (MUST be before any lambda that uses them)
    val strSelectImageError = remember { context.getString(R.string.please_select_image_error) }
    val strSelectCategoryError = remember { context.getString(R.string.please_select_category_error) }
    val strEnterColorError = remember { context.getString(R.string.please_enter_color_error) }
    val strEnterItemNameError = remember { context.getString(R.string.please_enter_item_name_error) }
    val strCategoryTops = remember { context.getString(R.string.category_tops) }
    val strCategoryBottoms = remember { context.getString(R.string.category_bottoms) }
    val strCategoryDresses = remember { context.getString(R.string.category_dresses) }
    val strCategoryFootwear = remember { context.getString(R.string.category_footwear) }
    val strCategoryBags = remember { context.getString(R.string.category_bags) }
    val strCategoryAccessories = remember { context.getString(R.string.category_accessories) }
    val strCategoryJewelry = remember { context.getString(R.string.category_jewelry) }
    val strSeasonSpring = remember { context.getString(R.string.season_spring) }
    val strSeasonSummer = remember { context.getString(R.string.season_summer) }
    val strSeasonAutumn = remember { context.getString(R.string.season_autumn) }
    val strSeasonWinter = remember { context.getString(R.string.season_winter) }
    val strOccasionCasual = remember { context.getString(R.string.occasion_casual) }
    val strOccasionWork = remember { context.getString(R.string.occasion_work) }
    val strOccasionSports = remember { context.getString(R.string.occasion_sports) }
    val strOccasionFormal = remember { context.getString(R.string.occasion_formal) }

    val categories = remember { listOf(strCategoryTops, strCategoryBottoms, strCategoryDresses, strCategoryFootwear, strCategoryBags, strCategoryAccessories, strCategoryJewelry) }
    val seasons = remember { listOf(strSeasonSpring, strSeasonSummer, strSeasonAutumn, strSeasonWinter) }
    val occasions = remember { listOf(strOccasionCasual, strOccasionWork, strOccasionSports, strOccasionFormal) }

    val canRemoveBackground = imagePath?.let { current ->
        lastRemoveBgPath == null || current != lastRemoveBgPath
    } ?: false

    val onRemoveBackgroundClick = {
        val currentPath = imagePath
        if (currentPath.isNullOrBlank()) {
            scope.launch { snackbarHostState.showSnackbar(strSelectImageError) }
        } else {
            imageProcessingViewModel.removeBackground(currentPath)
        }
        Unit
    }

    var category by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var itemName by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var selectedSeason by remember { mutableStateOf("") }
    var selectedOccasion by remember { mutableStateOf("") }
    var purchaseDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    var purchaseDateText by remember { mutableStateOf(dateFormat.format(Date())) }
    var showDatePicker by remember { mutableStateOf(false) }
    var expandedMenu by remember { mutableStateOf(false) }

    // ... (LaunchedEffects kept same but using stringResource for messages)
    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(message = msg, duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }

    LaunchedEffect(removeBgState.resultPath) {
        val newPath = removeBgState.resultPath
        if (!newPath.isNullOrBlank()) {
            imagePickerState.setImagePath(newPath)
            lastRemoveBgPath = newPath
            imageProcessingViewModel.clearResult()
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
            suggestion.category?.let { category = it }
            suggestion.color?.let { color = it }
            suggestion.name?.let { itemName = it }
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
            suggestion.season?.let { selectedSeason = it }
            suggestion.occasion?.let { selectedOccasion = it }
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

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.add_item_title), fontWeight = FontWeight.Bold) },
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
            ImagePickerSection(
                title = stringResource(R.string.item_image_label),
                imagePath = imagePath,
                onCameraClick = imagePickerState.onCameraClick,
                onGalleryClick = imagePickerState.onGalleryClick,
                onRemoveBgClick = onRemoveBackgroundClick,
                isProcessing = removeBgState.isProcessing,
                canRemoveBg = canRemoveBackground
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        val currentPath = imagePath
                        if (currentPath.isNullOrBlank()) {
                            scope.launch { snackbarHostState.showSnackbar(strSelectImageError) }
                        } else {
                            imageProcessingViewModel.autoTagging(currentPath)
                        }
                    },
                    enabled = !autoTaggingState.isProcessing,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    if (autoTaggingState.isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.tagging_label))
                    } else {
                        Text(stringResource(R.string.auto_tagging_label), style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        val currentPath = imagePath
                        if (currentPath.isNullOrBlank()) {
                            scope.launch { snackbarHostState.showSnackbar(strSelectImageError) }
                        } else {
                            imageProcessingViewModel.fillWithAi(currentPath)
                        }
                    },
                    enabled = !aiFillState.isProcessing,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    if (aiFillState.isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.filling_label))
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.fill_with_ai_label), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            HorizontalDivider()

            Text(stringResource(R.string.category_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            ExposedDropdownMenuBox(
                expanded = expandedMenu,
                onExpandedChange = { expandedMenu = !expandedMenu }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.select_category_hint)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMenu) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(
                        type = MenuAnchorType.PrimaryNotEditable,
                        enabled = true
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                )
                ExposedDropdownMenu(
                    expanded = expandedMenu,
                    onDismissRequest = { expandedMenu = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                category = cat
                                expandedMenu = false
                                focusManager.moveFocus(FocusDirection.Down)
                            }
                        )
                    }
                }
            }

            Text(stringResource(R.string.color_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = color,
                onValueChange = { color = it },
                label = { Text(stringResource(R.string.enter_color_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )

            HorizontalDivider()

            Text(stringResource(R.string.item_name_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = itemName,
                onValueChange = { itemName = it },
                label = { Text(stringResource(R.string.enter_item_name_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.item_name_placeholder)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )

            Text(stringResource(R.string.brand_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = brand,
                onValueChange = { brand = it },
                label = { Text(stringResource(R.string.enter_brand_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.brand_placeholder)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )

            Text(stringResource(R.string.price_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text(stringResource(R.string.enter_price_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.price_placeholder)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                prefix = { Text("\u20AB ") }
            )

            Text(stringResource(R.string.season_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                seasons.forEach { season ->
                    FilterChip(
                        selected = selectedSeason == season,
                        onClick = { selectedSeason = if (selectedSeason == season) "" else season },
                        label = { Text(season) },
                        shape = MaterialTheme.shapes.small
                    )
                }
            }

            Text(stringResource(R.string.occasion_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                occasions.forEach { occasion ->
                    FilterChip(
                        selected = selectedOccasion == occasion,
                        onClick = { selectedOccasion = if (selectedOccasion == occasion) "" else occasion },
                        label = { Text(occasion) },
                        shape = MaterialTheme.shapes.small
                    )
                }
            }

            Text(stringResource(R.string.purchase_date_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showDatePicker = true },
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.tap_to_select_date),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = purchaseDateText,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            HorizontalDivider()

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    if (imagePath == null) {
                        scope.launch { snackbarHostState.showSnackbar(strSelectImageError) }; return@Button
                    }
                    if (category.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar(strSelectCategoryError) }; return@Button
                    }
                    if (color.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar(strEnterColorError) }; return@Button
                    }
                    if (itemName.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar(strEnterItemNameError) }; return@Button
                    }

                    val parsedPrice = price.toDoubleOrNull() ?: 0.0

                    viewModel.addClothingItem(
                        imageFile = File(imagePath!!),
                        category = category,
                        color = color,
                        name = itemName,
                        season = selectedSeason,
                        occasion = selectedOccasion,
                        brand = brand,
                        purchaseDate = purchaseDate,
                        price = parsedPrice
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !isLoading,
                shape = MaterialTheme.shapes.small
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.adding_to_closet))
                } else {
                    Text(stringResource(R.string.add_to_closet_button), style = MaterialTheme.typography.titleMedium)
                }
            }

            if (isLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = stringResource(R.string.uploading_item_progress, itemName),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = purchaseDate,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= System.currentTimeMillis()
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedMillis ->
                        purchaseDate = selectedMillis
                        purchaseDateText = dateFormat.format(Date(selectedMillis))
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.select_date_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel_button))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
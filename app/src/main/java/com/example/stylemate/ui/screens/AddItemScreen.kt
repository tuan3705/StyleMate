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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
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

/**
 * 📸 Màn hình Thêm đồ mới (AddItemScreen).
 */
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

    val canRemoveBackground = imagePath?.let { current ->
        lastRemoveBgPath == null || current != lastRemoveBgPath
    } ?: false

    val onRemoveBackgroundClick = {
        val currentPath = imagePath
        if (currentPath.isNullOrBlank()) {
            scope.launch { snackbarHostState.showSnackbar("Please select an image first") }
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
    var purchaseDateText by remember {
        mutableStateOf(dateFormat.format(Date()))
    }
    var showDatePicker by remember { mutableStateOf(false) }

    val categories = listOf("Tops", "Bottoms", "Dresses", "Footwear", "Bags", "Accessories", "Jewelry")
    val seasons = listOf("Spring", "Summer", "Autumn", "Winter")
    val occasions = listOf("Casual", "Work", "Sports", "Formal")
    var expandedMenu by remember { mutableStateOf(false) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
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

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Add New Item", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                title = "Item Image",
                imagePath = imagePath,
                onCameraClick = imagePickerState.onCameraClick,
                onGalleryClick = imagePickerState.onGalleryClick,
                onRemoveBgClick = onRemoveBackgroundClick,
                isProcessing = removeBgState.isProcessing,
                canRemoveBg = canRemoveBackground
            )

            HorizontalDivider()

            Text("Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            ExposedDropdownMenuBox(
                expanded = expandedMenu,
                onExpandedChange = { expandedMenu = !expandedMenu }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select category") },
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

            Text("Color", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = color,
                onValueChange = { color = it },
                label = { Text("Enter color (e.g. Red, Blue)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )

            HorizontalDivider()

            Text("Item Name", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = itemName,
                onValueChange = { itemName = it },
                label = { Text("Enter item name (e.g. White Shirt)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Áo sơ mi trắng") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )

            Text("Brand", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = brand,
                onValueChange = { brand = it },
                label = { Text("Enter brand (e.g. Nike, Uniqlo)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Uniqlo, Nike, Adidas") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
            )

            Text("Price", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text("Enter price") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. 250000") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                prefix = { Text("₫ ") }
            )

            Text("Season", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                seasons.forEach { season ->
                    FilterChip(
                        selected = selectedSeason == season,
                        onClick = {
                            selectedSeason = if (selectedSeason == season) "" else season
                        },
                        label = { Text(season) }
                    )
                }
            }

            Text("Occasion", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                occasions.forEach { occasion ->
                    FilterChip(
                        selected = selectedOccasion == occasion,
                        onClick = {
                            selectedOccasion = if (selectedOccasion == occasion) "" else occasion
                        },
                        label = { Text(occasion) }
                    )
                }
            }

            Text("Purchase Date", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showDatePicker = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                            text = "Tap to select date",
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
                        scope.launch { snackbarHostState.showSnackbar("Please select an image first") }; return@Button
                    }
                    if (category.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar("Please select a category") }; return@Button
                    }
                    if (color.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar("Please enter a color") }; return@Button
                    }
                    if (itemName.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar("Please enter item name") }; return@Button
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
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Adding to Closet...")
                } else {
                    Text("Add to Closet", style = MaterialTheme.typography.titleMedium)
                }
            }

            if (isLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Text(
                        text = "⏳ Đang tải ảnh lên hệ thống cho \"$itemName\"...",
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
                    Text("Chọn")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Huỷ")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

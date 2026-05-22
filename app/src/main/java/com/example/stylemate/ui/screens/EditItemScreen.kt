package com.example.stylemate.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.stylemate.repository.ClothingRepository
import com.example.stylemate.repository.OutfitRepository
import com.example.stylemate.ui.common.ImagePickerSection
import com.example.stylemate.ui.common.rememberImagePickerState
import com.example.stylemate.viewmodel.ItemEditViewModel
import com.example.stylemate.viewmodel.ItemEditViewModelFactory
import kotlinx.coroutines.launch
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

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val relevantOutfits by viewModel.relevantOutfits.collectAsStateWithLifecycle()
    val saveSuccess by viewModel.saveSuccess.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val imagePickerState = rememberImagePickerState(
        onError = { message ->
            scope.launch { snackbarHostState.showSnackbar(message) }
        }
    )
    val pickedImagePath by imagePickerState.imagePath

    var showDatePicker by remember { mutableStateOf(false) }
    var expandedMenu by remember { mutableStateOf(false) }

    val categories = listOf("Tops", "Bottoms", "Dresses", "Footwear", "Bags", "Accessories", "Jewelry")
    val seasons = listOf("Spring", "Summer", "Autumn", "Winter")
    val occasions = listOf("Casual", "Work", "Sports", "Formal")

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val purchaseDateText = remember(uiState.purchaseDate) {
        dateFormat.format(Date(uiState.purchaseDate))
    }

    LaunchedEffect(itemId) {
        viewModel.loadItem(itemId)
    }

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

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            navController.previousBackStackEntry?.savedStateHandle?.set("refresh_items", true)
            viewModel.clearSaveSuccess()
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Edit Item", fontWeight = FontWeight.Bold) },
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
            if (uiState.isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            Text("Relevant Outfits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (relevantOutfits.isEmpty()) {
                Text(
                    text = "Chưa có outfit nào dùng item này",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(relevantOutfits, key = { it.outfit.id }) { outfit ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = outfit.outfit.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${outfit.clothingItems.size} items",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            Text("Item Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            ImagePickerSection(
                title = "Item Image",
                imagePath = pickedImagePath ?: uiState.imageOriginal,
                onCameraClick = imagePickerState.onCameraClick,
                onGalleryClick = imagePickerState.onGalleryClick
            )

            Text("Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            ExposedDropdownMenuBox(
                expanded = expandedMenu,
                onExpandedChange = { expandedMenu = !expandedMenu }
            ) {
                OutlinedTextField(
                    value = uiState.category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMenu) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                ExposedDropdownMenu(
                    expanded = expandedMenu,
                    onDismissRequest = { expandedMenu = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                viewModel.updateCategory(cat)
                                expandedMenu = false
                            }
                        )
                    }
                }
            }

            Text("Color", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = uiState.color,
                onValueChange = viewModel::updateColor,
                label = { Text("Enter color (e.g. Red, Blue)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            HorizontalDivider()

            Text("Item Name", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::updateName,
                label = { Text("Enter item name (e.g. White Shirt)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            Text("Brand", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = uiState.brand,
                onValueChange = viewModel::updateBrand,
                label = { Text("Enter brand (e.g. Nike, Uniqlo)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            Text("Price", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = uiState.price,
                onValueChange = viewModel::updatePrice,
                label = { Text("Enter price") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                )
            )

            Text("Season", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                seasons.forEach { season ->
                    FilterChip(
                        selected = uiState.season == season,
                        onClick = {
                            viewModel.updateSeason(
                                if (uiState.season == season) "" else season
                            )
                        },
                        label = { Text(season) }
                    )
                }
            }

            Text("Occasion", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                occasions.forEach { occasion ->
                    FilterChip(
                        selected = uiState.occasion == occasion,
                        onClick = {
                            viewModel.updateOccasion(
                                if (uiState.occasion == occasion) "" else occasion
                            )
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

            Button(
                onClick = { viewModel.saveChanges() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !uiState.isSaving &&
                    uiState.category.isNotBlank() &&
                    uiState.color.isNotBlank()
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Saving...")
                } else {
                    Text("Save Changes")
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.purchaseDate,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis <= System.currentTimeMillis()
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedMillis ->
                        viewModel.updatePurchaseDate(selectedMillis)
                    }
                    showDatePicker = false
                }) { Text("Chọn") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Huỷ") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

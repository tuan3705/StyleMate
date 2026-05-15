package com.example.stylemate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stylemate.model.AppDatabase
import com.example.stylemate.model.Categories
import com.example.stylemate.model.Item
import com.example.stylemate.model.ItemViewModel
import com.example.stylemate.model.ItemViewModelFactory
import com.example.stylemate.model.Season
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClosetScreen() {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val viewModel: ItemViewModel = viewModel(
        factory = ItemViewModelFactory(database.itemDao())
    )

    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val items by viewModel.items.collectAsState()
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }

    val allCategories = listOf(Categories.ALL) + Categories.list

    Scaffold(
        floatingActionButton = {
            AddItemFab(onClick = { showBottomSheet = true })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Closet",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { /* Filter action */ }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Filter Buttons
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allCategories) { category ->
                    val itemCount by viewModel.getItemCount(category).collectAsState(initial = 0)
                    CategoryChip(
                        category = category,
                        count = itemCount,
                        isSelected = selectedCategory == category,
                        onClick = { viewModel.selectCategory(category) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content Area (Items List)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (items.isEmpty()) {
                    Text(
                        text = "No items in ${selectedCategory.takeIf { it != Categories.ALL } ?: "your closet"}",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                } else {
                    // Here you would implement a Grid or List to display the items
                    Text(text = "Displaying ${items.size} items")
                }
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showBottomSheet = false
            },
            sheetState = sheetState
        ) {
            // Sheet content
            AddItemSheet(
                onAddItem = { item ->
                    scope.launch {
                        viewModel.addItem(item)
                        sheetState.hide()
                    }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            showBottomSheet = false
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun AddItemSheet(onAddItem: (Item) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(Categories.list.first()) }
    var color by remember { mutableStateOf("") }
    var selectedSeason by remember { mutableStateOf(Season.Spring) }
    var brand by remember { mutableStateOf("") }
    var purchaseDate by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    val occasions = listOf("Casual", "Work", "Sports", "Formal")
    var selectedOccasion by remember { mutableStateOf(occasions.first()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Text(
            "Item Details",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Form
        DetailInputRow(label = "Name", value = name, onValueChange = { name = it }, placeholder = "Enter item name")
        DetailClickableRow(label = "Category", value = selectedCategory) {
            // In a real app, this would open a selection screen or dialog
        }
        ChipSelectionRow(
            label = "Category",
            items = Categories.list,
            selectedItem = selectedCategory,
            onItemSelected = { selectedCategory = it }
        )
        DetailInputRow(label = "Color", value = color, onValueChange = { color = it }, placeholder = "Enter color(s)")
        ChipSelectionRow(
            label = "Season",
            items = Season.entries.map { it.name },
            selectedItem = selectedSeason.name,
            onItemSelected = { selectedSeason = Season.valueOf(it) }
        )
        ChipSelectionRow(
            label = "Occasion",
            items = occasions,
            selectedItem = selectedOccasion,
            onItemSelected = { selectedOccasion = it }
        )
        DetailInputRow(label = "Brand", value = brand, onValueChange = { brand = it }, placeholder = "Enter brand")
        DetailInputRow(label = "Purchase Date", value = purchaseDate, onValueChange = { purchaseDate = it }, placeholder = "Select Date")
        DetailInputRow(label = "Price", value = price, onValueChange = { price = it }, placeholder = "Enter price")


        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                val newItem = Item(
                    name = name,
                    category = selectedCategory,
                    imageUri = null,
                    color = color,
                    season = selectedSeason.name,
                    occasion = selectedOccasion,
                    brand = brand,
                    purchaseDate = purchaseDate,
                    price = price.toDoubleOrNull() ?: 0.0
                )
                onAddItem(newItem)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Item")
        }
    }
}

@Composable
fun DetailInputRow(label: String, value: String, onValueChange: (String) -> Unit, placeholder: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                textAlign = TextAlign.End
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    if (value.isEmpty()) {
                        Text(text = placeholder, color = Color.Gray, fontSize = 16.sp)
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
fun DetailClickableRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                modifier = Modifier.padding(end = 8.dp)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "Action",
                modifier = Modifier.size(16.dp),
                tint = Color.Gray
            )
        }
    }
}

@Composable
fun ChipSelectionRow(label: String, items: List<String>, selectedItem: String, onItemSelected: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(0.4f))
        LazyRow(
            modifier = Modifier.weight(0.6f),
            horizontalArrangement = Arrangement.End
        ) {
            items(items) { item ->
                SuggestionChip(
                    onClick = { onItemSelected(item) },
                    label = { Text(item) },
                    modifier = Modifier.padding(start = 8.dp),
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = if (selectedItem == item) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = if (selectedItem == item) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = null
                )
            }
        }
    }
}

@Composable
fun CategoryChip(
    category: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Color(0xFFFFD54F) else Color(0xFFF5F5F5), // Yellowish or light gray
        contentColor = if (isSelected) Color.Black else Color.Gray
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = category,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 16.sp
            )
            // Show count for all categories in the filter row
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = count.toString(),
                fontSize = 12.sp,
                color = if (isSelected) Color.Black.copy(alpha = 0.7f) else Color.Gray
            )
        }
    }
}

@Composable
fun AddItemFab(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add Item"
        )
    }
}

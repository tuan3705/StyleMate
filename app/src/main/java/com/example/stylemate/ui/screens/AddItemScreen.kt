package com.example.stylemate.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.stylemate.model.AppDatabase
import com.example.stylemate.repository.ClothingRepository
import com.example.stylemate.viewmodel.ClothingViewModel
import com.example.stylemate.viewmodel.ClothingViewModelFactory
import kotlinx.coroutines.launch
import java.io.File

/**
 * 📸 Màn hình Thêm đồ mới (AddItemScreen).
 *
 * 📐 Luồng dữ liệu:
 *   UI ← collect StateFlow (isLoading, errorMessage) ← ClothingViewModel
 *   UI → gọi addClothingItem() → ClothingViewModel → ClothingRepository → Room
 *
 * 🔐 Xử lý loading: Khi isLoading == true, nút bị vô hiệu hoá + hiển thị spinner.
 * 🔐 Xử lý lỗi: errorMessage được show qua Snackbar, tự động clear.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemScreen(navController: NavController) {
    // ── Khởi tạo ViewModel ───────────────────────────────────────
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val repository = ClothingRepository(database.clothingDao())
    val viewModel: ClothingViewModel = viewModel(
        factory = ClothingViewModelFactory(repository)
    )

    // ── Collect StateFlow từ ViewModel ───────────────────────────
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // ── Local state cho form ─────────────────────────────────────
    var imagePath by remember { mutableStateOf<String?>(null) }
    var category by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }

    val categories = listOf("Tops", "Bottoms", "Dresses", "Footwear", "Bags", "Accessories", "Jewelry")
    var expandedMenu by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // ── Hiển thị Snackbar khi có lỗi ─────────────────────────────
    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
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
            // ── Chọn ảnh (mock) ────────────────────────────────────
            Text("Item Image", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(
                    onClick = {
                        // 📸 Mock: giả lập chụp ảnh bằng camera
                        imagePath = "/tmp/mock_camera_${System.currentTimeMillis()}.jpg"
                    },
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Take Photo")
                }
                FilledTonalButton(
                    onClick = {
                        // 🖼️ Mock: giả lập chọn từ gallery
                        imagePath = "/tmp/mock_gallery_${System.currentTimeMillis()}.jpg"
                    },
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("From Gallery")
                }
            }

            // Preview đường dẫn ảnh đã chọn
            if (imagePath != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text(
                        text = "✅ Image selected: ${imagePath?.substringAfterLast("/")}",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            HorizontalDivider()

            // ── Category (Dropdown) ────────────────────────────────
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
                    )
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
                            }
                        )
                    }
                }
            }

            // ── Color ──────────────────────────────────────────────
            Text("Color", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = color,
                onValueChange = { color = it },
                label = { Text("Enter color (e.g. Red, Blue)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            // ── Nút Lưu (Loading-aware) ───────────────────────────
            Button(
                onClick = {
                    // Validate
                    if (imagePath == null) {
                        scope.launch { snackbarHostState.showSnackbar("Please select an image first") }; return@Button
                    }
                    if (category.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar("Please select a category") }; return@Button
                    }
                    if (color.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar("Please enter a color") }; return@Button
                    }

                    // ⚡ Gọi ViewModel — xử lý bất đồng bộ (mock tách nền + ghi DB)
                    viewModel.addClothingItem(
                        imageFile = File(imagePath!!),
                        category = category,
                        color = color
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !isLoading  // ⛔ Vô hiệu hoá khi đang xử lý
            ) {
                if (isLoading) {
                    // 🔄 Spinner + text
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Processing image...")
                } else {
                    Text("Add to Closet", style = MaterialTheme.typography.titleMedium)
                }
            }

            // Loading info card
            if (isLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Text(
                        text = "⏳ Removing background & analyzing image...",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

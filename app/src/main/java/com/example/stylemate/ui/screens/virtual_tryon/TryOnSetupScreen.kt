package com.example.stylemate.ui.screens.virtual_tryon

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.stylemate.R
import com.example.stylemate.data.models.JobStatus
import com.example.stylemate.data.models.ProcessingJob
import com.example.stylemate.model.ClothingItemEntity
import com.example.stylemate.model.OutfitWithClothingItems
import com.example.stylemate.ui.common.PermissionRationaleDialog
import com.example.stylemate.ui.common.PermissionSettingsRedirectDialog
import com.example.stylemate.ui.common.getStoragePermission
import com.example.stylemate.ui.common.hasStoragePermission
import com.example.stylemate.ui.common.rememberImagePickerState
import com.example.stylemate.ui.common.saveImageToGallery
import com.example.stylemate.ui.components.StylistButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TryOnSetupScreen(
    viewModel: TryOnViewModel = viewModel(),
    onBack: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    val jobState by viewModel.jobState.collectAsState()
    val selectedItem by viewModel.selectedItem.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Save to gallery state
    var isSavingToGallery by remember { mutableStateOf(false) }
    var showStorageRationale by remember { mutableStateOf(false) }
    var showStorageSettingsRedirect by remember { mutableStateOf(false) }
    // ⚡ Track whether we've ever requested storage permission before
    // to differentiate "first time ask" from "permanently denied"
    var hasRequestedStorageBefore by remember { mutableStateOf(false) }

    // Storage permission launcher
    val requestStoragePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            isSavingToGallery = true
            scope.launch {
                saveCurrentResultToGallery(context, jobState, snackbarHostState)
                isSavingToGallery = false
            }
        } else {
            val activity = context as? androidx.activity.ComponentActivity
            if (activity != null && androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                    activity, getStoragePermission()
                )
            ) {
                // ⚡ Từ chối nhưng chưa vĩnh viễn → show rationale để giải thích
                showStorageRationale = true
            } else {
                // ⚡ Từ chối vĩnh viễn (có check "Never ask again") → redirect settings
                showStorageSettingsRedirect = true
            }
        }
    }

    // Body image picker
    var bodyError by remember { mutableStateOf<String?>(null) }
    val bodyPickerState = rememberImagePickerState(
        context = context,
        onError = { bodyError = it }
    )

    // Load all clothing items (không dùng outfit)
    val clothes by viewModel.clothingRepository.getAllItems()
        .collectAsState(initial = emptyList())

    val canStart = bodyPickerState.imagePath.value != null &&
            selectedItem != null &&
            (jobState == null || jobState?.status != JobStatus.IN_PROGRESS)

    fun onSaveToGalleryClicked() {
        if (hasStoragePermission(context)) {
            isSavingToGallery = true
            scope.launch {
                saveCurrentResultToGallery(context, jobState, snackbarHostState)
                isSavingToGallery = false
            }
        } else {
            val activity = context as? androidx.activity.ComponentActivity
            if (activity != null && androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                    activity, getStoragePermission()
                )
            ) {
                // ⚡ Đã từ chối 1 lần (không vĩnh viễn) → show dialog giải thích
                showStorageRationale = true
            } else if (activity != null && !hasRequestedStorageBefore) {
                // ⚡ CHƯA BAO GIỜ hỏi quyền này → request trực tiếp (KHÔNG show rationale hay settings redirect)
                hasRequestedStorageBefore = true
                requestStoragePermissionLauncher.launch(getStoragePermission())
            } else if (activity != null && hasRequestedStorageBefore) {
                // ⚡ Đã request rồi và bị từ chối vĩnh viễn → redirect settings
                showStorageSettingsRedirect = true
            } else {
                // Trong bottom sheet / non-Activity context → fallback: redirect trực tiếp
                showStorageSettingsRedirect = true
            }
        }
    }

    // Permission dialog
    if (showStorageRationale) {
        PermissionRationaleDialog(
            title = stringResource(R.string.permission_storage_title),
            message = stringResource(R.string.permission_storage_save_tryon),
            icon = Icons.Default.PhotoLibrary,
            onGrant = {
                showStorageRationale = false
                requestStoragePermissionLauncher.launch(getStoragePermission())
            },
            onDeny = { showStorageRationale = false }
        )
    }

    // Settings redirect dialog
    if (showStorageSettingsRedirect) {
        PermissionSettingsRedirectDialog(
            title = stringResource(R.string.permission_storage_title),
            message = stringResource(R.string.permission_storage_settings_redirect),
            icon = Icons.Default.PhotoLibrary,
            onGoToSettings = {
                showStorageSettingsRedirect = false
                com.example.stylemate.ui.common.openAppSettings(context)
            },
            onDismiss = { showStorageSettingsRedirect = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.virtual_tryon_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_content_desc))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            // Hiển thị form setup khi: chưa có job, job thất bại/hủy, hoặc đang QUEUED (chờ xử lý)
            if (jobState == null ||
                jobState?.status == JobStatus.FAILED ||
                jobState?.status == JobStatus.CANCELLED ||
                jobState?.status == JobStatus.QUEUED ||
                (jobState?.status == JobStatus.IN_PROGRESS && jobState?.progress ?: 0 < 5)) {

                Text(
                    text = stringResource(R.string.tryon_setup_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                bodyError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                // ── Body Image Card ────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = stringResource(R.string.tryon_body_label),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FilledTonalButton(
                                onClick = bodyPickerState.onCameraClick,
                                modifier = Modifier.height(48.dp).weight(1f)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.take_photo_label))
                            }
                            FilledTonalButton(
                                onClick = bodyPickerState.onGalleryClick,
                                modifier = Modifier.height(48.dp).weight(1f)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.from_gallery_label))
                            }
                        }
                        val bodyPath = bodyPickerState.imagePath.value
                        if (!bodyPath.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            AsyncImage(
                                model = File(bodyPath),
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Chọn Item để thử (dạng grid, không phải outfit) ──
                Text(
                    text = stringResource(R.string.tryon_select_outfit),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (clothes.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7FB))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(stringResource(R.string.no_outfits_yet), style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.heightIn(max = 360.dp)
                    ) {
                        items(clothes, key = { it.id }) { item ->
                            val isSelected = selectedItem?.id == item.id
                            val imageUrl = if (item.imageNoBg.isNotBlank()) item.imageNoBg else item.imageOriginal
                            val fullUrl = if (imageUrl.startsWith("http")) imageUrl
                                else "${com.example.stylemate.network.RetrofitClient.STYLEMATE_BASE_URL.trimEnd('/')}$imageUrl"

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .then(if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)) else Modifier)
                                    .clickable { viewModel.selectItem(if (isSelected) null else item) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AsyncImage(
                                        model = fullUrl,
                                        contentDescription = item.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Column(
                                        modifier = Modifier.fillMaxWidth()
                                            .align(Alignment.BottomCenter)
                                            .background(Color.Black.copy(alpha = 0.5f))
                                            .padding(4.dp)
                                    ) {
                                        Text(
                                            text = item.name.ifBlank { item.category },
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                StylistButton(
                    text = stringResource(R.string.start_try_on),
                    onClick = {
                        val bodyPath = bodyPickerState.imagePath.value
                        if (bodyPath != null) viewModel.startTryOn(Uri.fromFile(File(bodyPath)))
                    },
                    enabled = canStart,
                    modifier = Modifier.fillMaxWidth()
                )

            } else if (jobState?.status == JobStatus.IN_PROGRESS) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    CircularProgressIndicator(progress = { (jobState?.progress ?: 0) / 100f }, modifier = Modifier.size(120.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = stringResource(R.string.tryon_processing_label, jobState?.progress ?: 0), style = MaterialTheme.typography.titleMedium)
                    Text(text = stringResource(R.string.tryon_status_label, jobState?.status.toString() ?: ""), style = MaterialTheme.typography.labelSmall)
                }

            } else if (jobState?.status == JobStatus.COMPLETED) {
                Text(text = stringResource(R.string.tryon_result_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 12.dp))
                AsyncImage(
                    model = jobState?.resultUrls?.firstOrNull(),
                    contentDescription = stringResource(R.string.tryon_result_content_desc),
                    modifier = Modifier.fillMaxWidth().height(450.dp).clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.height(16.dp))

                StylistButton(
                    text = stringResource(R.string.tryon_save_to_gallery),
                    onClick = { onSaveToGalleryClicked() },
                    enabled = !isSavingToGallery,
                    modifier = Modifier.fillMaxWidth()
                )
                if (isSavingToGallery) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { viewModel.reset() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.try_another_button))
                }

            } else if (jobState?.status == JobStatus.FAILED) {
                Text(text = stringResource(R.string.tryon_failed_label, jobState?.error ?: ""), color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(16.dp))
                StylistButton(text = stringResource(R.string.retry_button), onClick = { viewModel.reset() })
            }
        }
    }
}

private suspend fun saveCurrentResultToGallery(
    context: Context,
    job: ProcessingJob?,
    snackbarHostState: SnackbarHostState
) {
    val currentJob = job ?: return
    val imageUrl = currentJob.resultUrls.firstOrNull() ?: return

    try {
        val file = withContext(Dispatchers.IO) {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder()
                .url(imageUrl)
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
            val body = response.body ?: throw Exception("Empty response")
            val ext = imageUrl.substringAfterLast('.', "png").take(4)
            val tempFile = File(context.cacheDir, "tryon_gallery_${System.currentTimeMillis()}.$ext")
            FileOutputStream(tempFile).use { out -> body.byteStream().copyTo(out) }
            tempFile
        }

        val result = withContext(Dispatchers.IO) {
            saveImageToGallery(
                context = context,
                imageFile = file,
                folderName = "StyleMate/TryOn"
            )
        }

        result.fold(
            onSuccess = {
                snackbarHostState.showSnackbar(
                    message = context.getString(R.string.tryon_save_gallery_success),
                    duration = SnackbarDuration.Short
                )
            },
            onFailure = { error ->
                snackbarHostState.showSnackbar(
                    message = context.getString(R.string.tryon_save_gallery_error, error.message ?: ""),
                    duration = SnackbarDuration.Short
                )
            }
        )
    } catch (e: Exception) {
        snackbarHostState.showSnackbar(
            message = context.getString(R.string.tryon_save_gallery_error, e.message ?: ""),
            duration = SnackbarDuration.Short
        )
    }
}
package com.example.stylemate.ui.common

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.stylemate.R
import com.example.stylemate.data.local.ImageStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

class ImagePickerState internal constructor(
    val imagePath: State<String?>,
    val onCameraClick: () -> Unit,
    val onGalleryClick: () -> Unit,
    val onRemoveBackgroundClick: () -> Unit,
    val setImagePath: (String?) -> Unit
)

private fun getGalleryPermission(): String {
    return if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
}

@Composable
fun rememberImagePickerState(
    context: Context = LocalContext.current,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    onRemoveBackground: () -> Unit = {},
    onError: (String) -> Unit
): ImagePickerState {
    val imagePathState = remember { mutableStateOf<String?>(null) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

    val cameraDeniedMessage = remember { context.getString(R.string.camera_permission_denied) }
    val galleryPermName = getGalleryPermission()
    val galleryDeniedMessage = remember { context.getString(R.string.gallery_permission_denied) }

    var showCameraRationale by remember { mutableStateOf(false) }
    var showCameraSettingsRedirect by remember { mutableStateOf(false) }
    var showGalleryRationale by remember { mutableStateOf(false) }
    var showGallerySettingsRedirect by remember { mutableStateOf(false) }

    // ── Track whether we've ever requested the permission before ──
    // This is needed because shouldShowRequestPermissionRationale()
    // returns FALSE in two cases:
    //   1. Permission has NEVER been requested yet (first time)
    //   2. Permission was PERMANENTLY denied (never ask again)
    // We use these flags to differentiate the two cases.
    var hasRequestedCameraBefore by remember { mutableStateOf(false) }
    var hasRequestedGalleryBefore by remember { mutableStateOf(false) }

    // ── Take Picture ──────────────────────────────────────────────
    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val file = pendingCameraFile
        if (success && file != null) {
            imagePathState.value = file.absolutePath
        } else {
            file?.delete()
        }
        pendingCameraFile = null
    }

    fun handlePickedUri(uri: Uri) {
        coroutineScope.launch {
            try {
                val file = ImageStorage.copyUriToInternalStorage(context, uri, prefix = "gallery_")
                imagePathState.value = file.absolutePath
            } catch (e: Exception) {
                onError("Unable to load image: ${e.message}")
            }
        }
    }

    // 📸 Photo Picker Launchers (không cần permission trên Android 13+)
    val pickMediaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { handlePickedUri(it) }
    }

    val getContentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { handlePickedUri(it) }
    }

    // ── Camera Permission Launcher ────────────────────────────────
    val requestCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val file = ImageStorage.createImageFile(context, prefix = "camera_")
            val uri = ImageStorage.createImageUri(context, file)
            pendingCameraFile = file
            takePictureLauncher.launch(uri)
        } else {
            // ⚡ Vừa bị từ chối → kiểm tra xem có bị vĩnh viễn không
            val activity = context as? Activity
            if (activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)) {
                // Từ chối nhưng chưa vĩnh viễn → show rationale để giải thích
                showCameraRationale = true
            } else {
                // Từ chối vĩnh viễn (có check "Never ask again") → redirect settings
                showCameraSettingsRedirect = true
            }
        }
    }

    // ── Gallery Permission Launcher ───────────────────────────────
    val requestGalleryPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            openPhotoPicker(context, pickMediaLauncher, getContentLauncher)
        } else {
            val activity = context as? Activity
            if (activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, galleryPermName)) {
                // Từ chối nhưng chưa vĩnh viễn → show rationale
                showGalleryRationale = true
            } else {
                // Từ chối vĩnh viễn → redirect settings
                showGallerySettingsRedirect = true
            }
        }
    }

    // ── Camera Click ──────────────────────────────────────────────
    val onCameraClick = {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            val file = ImageStorage.createImageFile(context, prefix = "camera_")
            val uri = ImageStorage.createImageUri(context, file)
            pendingCameraFile = file
            takePictureLauncher.launch(uri)
        } else {
            val activity = context as? Activity
            if (activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)) {
                // ⚡ Đã từ chối 1 lần (không vĩnh viễn) → show dialog giải thích
                showCameraRationale = true
            } else if (activity != null && !hasRequestedCameraBefore) {
                // ⚡ CHƯA BAO GIỜ hỏi quyền này → request trực tiếp (KHÔNG show settings redirect)
                hasRequestedCameraBefore = true
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            } else if (activity != null && hasRequestedCameraBefore) {
                // ⚡ Đã request rồi và bị từ chối vĩnh viễn → redirect settings
                showCameraSettingsRedirect = true
            } else {
                // Trong bottom sheet, context không phải Activity → fallback: redirect trực tiếp
                showCameraSettingsRedirect = true
            }
        }
    }

    // ── Gallery Click ─────────────────────────────────────────────
    val onGalleryClick = {
        val hasPermission = ContextCompat.checkSelfPermission(context, galleryPermName) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            openPhotoPicker(context, pickMediaLauncher, getContentLauncher)
        } else {
            val activity = context as? Activity
            if (activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, galleryPermName)) {
                // ⚡ Đã từ chối 1 lần → show dialog giải thích
                showGalleryRationale = true
            } else if (activity != null && !hasRequestedGalleryBefore) {
                // ⚡ CHƯA BAO GIỜ hỏi quyền này → request trực tiếp
                hasRequestedGalleryBefore = true
                requestGalleryPermissionLauncher.launch(galleryPermName)
            } else if (activity != null && hasRequestedGalleryBefore) {
                // ⚡ Đã từ chối vĩnh viễn → redirect settings
                showGallerySettingsRedirect = true
            } else {
                // Trong bottom sheet, context không phải Activity → fallback: redirect trực tiếp
                showGallerySettingsRedirect = true
            }
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────
    if (showCameraRationale) {
        PermissionRationaleDialog(
            title = stringResource(R.string.camera_permission_rationale_title),
            message = stringResource(R.string.camera_permission_rationale),
            icon = Icons.Default.PhotoCamera,
            onGrant = {
                showCameraRationale = false
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            },
            onDeny = {
                showCameraRationale = false
                onError(cameraDeniedMessage)
            }
        )
    }

    if (showCameraSettingsRedirect) {
        PermissionSettingsRedirectDialog(
            title = stringResource(R.string.camera_permission_rationale_title),
            message = stringResource(R.string.permission_settings_redirect),
            icon = Icons.Default.PhotoCamera,
            onGoToSettings = {
                showCameraSettingsRedirect = false
                // ⚡ FIX: Dùng Intent.FLAG_ACTIVITY_NEW_TASK để hoạt động trong bottom sheet context
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            },
            onDismiss = { showCameraSettingsRedirect = false }
        )
    }

    if (showGalleryRationale) {
        PermissionRationaleDialog(
            title = stringResource(R.string.gallery_permission_rationale_title),
            message = stringResource(R.string.gallery_permission_rationale),
            icon = Icons.Default.PhotoLibrary,
            onGrant = {
                showGalleryRationale = false
                requestGalleryPermissionLauncher.launch(galleryPermName)
            },
            onDeny = {
                showGalleryRationale = false
                onError(galleryDeniedMessage)
            }
        )
    }

    if (showGallerySettingsRedirect) {
        PermissionSettingsRedirectDialog(
            title = stringResource(R.string.gallery_permission_rationale_title),
            message = stringResource(R.string.permission_settings_redirect),
            icon = Icons.Default.PhotoLibrary,
            onGoToSettings = {
                showGallerySettingsRedirect = false
                // ⚡ FIX: Dùng Intent.FLAG_ACTIVITY_NEW_TASK để hoạt động trong bottom sheet context
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            },
            onDismiss = { showGallerySettingsRedirect = false }
        )
    }

    return ImagePickerState(
        imagePath = imagePathState,
        onCameraClick = onCameraClick,
        onGalleryClick = onGalleryClick,
        onRemoveBackgroundClick = onRemoveBackground,
        setImagePath = { newPath -> imagePathState.value = newPath }
    )
}

private fun openPhotoPicker(
    context: Context,
    pickMediaLauncher: androidx.activity.result.ActivityResultLauncher<PickVisualMediaRequest>,
    getContentLauncher: androidx.activity.result.ActivityResultLauncher<String>
) {
    val isPhotoPickerAvailable = ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context)
    if (isPhotoPickerAvailable) {
        pickMediaLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    } else {
        getContentLauncher.launch("image/*")
    }
}

@Composable
fun ImagePickerSection(
    title: String,
    imagePath: String?,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onRemoveBgClick: () -> Unit,
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = MaterialTheme.typography.titleMedium,
    showPreview: Boolean = true,
    isProcessing: Boolean = false,
    canRemoveBg: Boolean = true
) {
    Text(title, style = titleStyle, fontWeight = FontWeight.SemiBold)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FilledTonalButton(
            onClick = onCameraClick,
            modifier = Modifier.height(48.dp).weight(1f)
        ) {
            Icon(Icons.Default.PhotoCamera, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.take_photo_label))
        }
        FilledTonalButton(
            onClick = onGalleryClick,
            modifier = Modifier.height(48.dp).weight(1f)
        ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.from_gallery_label))
        }
    }

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp
    val minPreviewSize = (screenHeightDp * 0.3f).dp
    val maxPreviewSize = (screenHeightDp * 0.4f).dp

    val isImageSelected = !imagePath.isNullOrBlank()

    val imageRequest = remember(imagePath, context) {
        imagePath
            ?.takeIf { it.isNotBlank() }
            ?.let { path -> resolveImageData(context, path) }
            ?.let { data ->
                ImageRequest.Builder(context)
                    .data(data)
                    .crossfade(false)
                    .build()
            }
    }

    if (showPreview && imageRequest != null) {
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = stringResource(R.string.selected_image_content_desc),
                modifier = Modifier.sizeIn(
                    minWidth = minPreviewSize,
                    minHeight = minPreviewSize,
                    maxWidth = maxPreviewSize,
                    maxHeight = maxPreviewSize
                ),
                contentScale = ContentScale.Fit
            )
        }
    }

    Spacer(Modifier.height(8.dp))

    FilledTonalButton(
        onClick = onRemoveBgClick,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        enabled = isImageSelected && !isProcessing && canRemoveBg
    ) {
        Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null
        )
        Spacer(Modifier.width(8.dp))
        Text(if (isProcessing) stringResource(R.string.removing_label) else stringResource(R.string.remove_bg_label))
    }
}
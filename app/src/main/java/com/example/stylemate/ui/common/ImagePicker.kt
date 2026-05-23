package com.example.stylemate.ui.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.stylemate.data.local.ImageStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

class ImagePickerState internal constructor(
    val imagePath: State<String?>,
    val onCameraClick: () -> Unit,
    val onGalleryClick: () -> Unit
)

@Composable
fun rememberImagePickerState(
    context: Context = LocalContext.current,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    onError: (String) -> Unit
): ImagePickerState {
    val imagePathState = remember { mutableStateOf<String?>(null) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

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

    val pickMediaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { handlePickedUri(it) }
    }

    val getContentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { handlePickedUri(it) }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val file = ImageStorage.createImageFile(context, prefix = "camera_")
            val uri = ImageStorage.createImageUri(context, file)
            pendingCameraFile = file
            takePictureLauncher.launch(uri)
        } else {
            onError("Camera permission is required to take a photo")
        }
    }

    val onCameraClick = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            val file = ImageStorage.createImageFile(context, prefix = "camera_")
            val uri = ImageStorage.createImageUri(context, file)
            pendingCameraFile = file
            takePictureLauncher.launch(uri)
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val onGalleryClick = {
        val isPhotoPickerAvailable = ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context)
        if (isPhotoPickerAvailable) {
            pickMediaLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } else {
            getContentLauncher.launch("image/*")
        }
    }

    return ImagePickerState(
        imagePath = imagePathState,
        onCameraClick = onCameraClick,
        onGalleryClick = onGalleryClick
    )
}

@Composable
fun ImagePickerSection(
    title: String,
    imagePath: String?,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = MaterialTheme.typography.titleMedium,
    showPreview: Boolean = true
) {
    Text(title, style = titleStyle, fontWeight = FontWeight.SemiBold)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FilledTonalButton(
            onClick = onCameraClick,
            modifier = Modifier.height(48.dp)
        ) {
            Icon(Icons.Default.PhotoCamera, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Take Photo")
        }
        FilledTonalButton(
            onClick = onGalleryClick,
            modifier = Modifier.height(48.dp)
        ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("From Gallery")
        }
    }

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp
    val minPreviewSize = (screenHeightDp * 0.3f).dp
    val maxPreviewSize = (screenHeightDp * 0.4f).dp
    val imageRequest = remember(imagePath, context) {
        imagePath
            ?.takeIf { it.isNotBlank() }
            ?.let { path -> resolveImageData(context, path) }
            ?.let { data ->
                ImageRequest.Builder(context)
                    .data(data)
                    .crossfade(true)
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
                contentDescription = "Selected image",
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
}

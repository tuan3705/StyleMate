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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * ─────────────────────────────────────────────────────────────────
 * PermissionManager — Quản lý tập trung tất cả quyền runtime.
 * ─────────────────────────────────────────────────────────────────
 *
 * Vấn đề đã fix:
 * - "Only this time" (Android 14+): Permission bị thu hồi khi app ra
 *   background, nhưng code cũ chỉ check 1 lần qua LaunchedEffect(Unit)
 *   → Giờ dùng LifecycleObserver để re-check mỗi khi app resume.
 *
 * - Gallery không cần permission: Photo Picker (Android 13+) là system
 *   picker, KHÔNG cần quyền. Fallback GetContent() cũng handle bởi
 *   system. Đây là behavior đúng của Android, không phải bug.
 *
 * - READ_MEDIA_IMAGES: Đã có trong manifest nhưng chưa request. Giờ
 *   sẽ request khi cần đọc gallery qua đường dẫn trực tiếp.
 *
 * - Camera/Location: Handle all 3 states:
 *     1. Chưa hỏi → request trực tiếp
 *     2. Đã từ chối (rationale) → show dialog giải thích
 *     3. Từ chối vĩnh viễn → redirect settings
 * ─────────────────────────────────────────────────────────────────
 */

/**
 * Kiểm tra quyền hiện tại — luôn trả về kết quả thực tế từ OS.
 */
fun checkPermission(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

/**
 * Kiểm tra nếu user đã từ chối vĩnh viễn (Deny + Never Ask Again)
 */
fun isPermissionPermanentlyDenied(activity: Activity, permission: String): Boolean {
    return !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
}

/**
 * Mở Settings app để user bật quyền thủ công
 */
fun openAppPermissionSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

// ────────── Permissions được app sử dụng ─────────────────────────
object AppPermissions {
    // Camera — cần khi chụp ảnh
    const val CAMERA = Manifest.permission.CAMERA

    // Location — cần cho Weather
    val LOCATION = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    // Storage (đọc gallery) — cần trên API <33 khi Photo Picker không có sẵn
    val STORAGE_READ: String get() {
        return if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    // Notifications — cần trên Android 13+
    const val POST_NOTIFICATIONS = Manifest.permission.POST_NOTIFICATIONS
}

/**
 * Composable helper: tự động re-check permission mỗi khi app resume.
 *
 * Dùng LifecycleObserver thay vì LaunchedEffect(Unit) để handle
 * "Only this time" (quyền bị thu hồi khi app vào background).
 *
 * @param permission Tên permission cần theo dõi
 * @return true nếu đã cấp, false nếu chưa
 */
@Composable
fun rememberPermissionGranted(permission: String): Boolean {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(checkPermission(context, permission)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // ⚡ Re-check mỗi khi app resume → handle "Only this time"
                granted = checkPermission(context, permission)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return granted
}

/**
 * Composable helper: theo dõi trạng thái nhiều permissions cùng lúc.
 * Trả về true nếu ÍT NHẤT 1 permission trong danh sách đã được cấp.
 */
@Composable
fun rememberAnyPermissionGranted(permissions: Array<String>): Boolean {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(permissions.any { checkPermission(context, it) })
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = permissions.any { checkPermission(context, it) }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return granted
}
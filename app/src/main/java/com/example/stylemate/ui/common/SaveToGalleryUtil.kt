package com.example.stylemate.ui.common

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream

/**
 * Lưu ảnh vào gallery điện thoại.
 * Yêu cầu quyền runtime tương ứng với Android version.
 *
 * Permission cần:
 * - Android 9 (API 28) trở xuống: WRITE_EXTERNAL_STORAGE
 * - Android 10+ (API 29+): Không cần WRITE_EXTERNAL_STORAGE nếu dùng MediaStore
 * - Android 13+ (API 33+): Cần READ_MEDIA_IMAGES nếu muốn đọc từ gallery sau khi lưu
 *
 * @param context Context
 * @param imageFile File ảnh cần lưu (đường dẫn local)
 * @param folderName Tên thư mục con trong Pictures (VD: "StyleMate")
 * @return Result<String> Đường dẫn ảnh đã lưu hoặc lỗi
 */
fun saveImageToGallery(
    context: Context,
    imageFile: File,
    folderName: String = "StyleMate"
): Result<String> {
    return try {
        if (!imageFile.exists()) {
            return Result.failure(Exception("File ảnh không tồn tại: ${imageFile.path}"))
        }

        val mimeType = when (imageFile.extension.lowercase()) {
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            else -> "image/jpeg"
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, imageFile.name)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$folderName")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: return Result.failure(Exception("Không thể tạo MediaStore entry"))

        // Copy file vào MediaStore
        var outputStream: OutputStream? = null
        var inputStream: FileInputStream? = null
        try {
            outputStream = resolver.openOutputStream(uri)
            inputStream = FileInputStream(imageFile)
            inputStream.copyTo(outputStream!!)
        } finally {
            outputStream?.close()
            inputStream?.close()
        }

        // Đánh dấu là không còn pending
        contentValues.clear()
        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, contentValues, null, null)

        Result.success("Đã lưu vào thư mục $folderName trong thư viện ảnh")
    } catch (e: Exception) {
        Result.failure(Exception("Lỗi khi lưu ảnh: ${e.message}"))
    }
}

/**
 * Kiểm tra xem có quyền lưu vào gallery không.
 * Trên Android 10+ (API 29+), không cần quyền ghi nếu dùng MediaStore.
 * Trên Android 13+ (API 33+), nếu cần đọc lại ảnh thì cần READ_MEDIA_IMAGES.
 */
fun hasStoragePermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= 29) {
        // Android 10+ không cần WRITE_EXTERNAL_STORAGE với MediaStore
        true
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}

/**
 * Lấy permission cần request để lưu vào gallery.
 */
fun getStoragePermission(): String {
    return if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else if (Build.VERSION.SDK_INT >= 29) {
        // API 29-32: không cần permission để ghi qua MediaStore
        // Nhưng nếu muốn đọc gallery, cần READ_EXTERNAL_STORAGE
        Manifest.permission.READ_EXTERNAL_STORAGE
    } else {
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    }
}

/**
 * Kiểm tra nếu user đã từ chối vĩnh viễn (không show rationale nữa)
 */
fun isStoragePermissionPermanentlyDenied(activity: Activity): Boolean {
    val permission = getStoragePermission()
    return !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
}

/**
 * Mở cài đặt app để user bật quyền thủ công
 */
fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = android.net.Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
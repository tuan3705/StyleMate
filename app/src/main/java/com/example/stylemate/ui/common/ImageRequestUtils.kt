package com.example.stylemate.ui.common

import android.content.Context
import android.net.Uri
import com.example.stylemate.network.RetrofitClient
import java.io.File

internal fun resolveImageData(context: Context, path: String): Any? {
    if (path.isBlank()) return null

    // URL http/https → dùng trực tiếp
    if (path.startsWith("http://") || path.startsWith("https://")) {
        return path
    }

    // Đường dẫn content:// hoặc file://
    if (path.startsWith("content://") || path.startsWith("file://")) {
        return Uri.parse(path)
    }

    // Đường dẫn tương đối của backend (vd: /uploads/xxx.jpg)
    // → ghép với BASE_URL hiện tại
    if (path.startsWith("/uploads/")) {
        val baseUrl = RetrofitClient.STYLEMATE_BASE_URL.trimEnd('/')
        return "$baseUrl$path"
    }

    // File local (tuyệt đối)
    val file = File(path)
    if (file.isAbsolute && file.exists()) return file

    // File trong filesDir
    val internalFile = File(context.filesDir, path)
    if (internalFile.exists()) return internalFile

    // File trong filesDir/images/
    val imagesDir = File(context.filesDir, "images")
    val byName = File(imagesDir, File(path).name)
    if (byName.exists()) return byName

    // Fallback: trả về path gốc
    return path
}
package com.example.stylemate.ui.common

import android.content.Context
import android.net.Uri
import com.example.stylemate.network.RetrofitClient
import java.io.File

/**
 * ⚡ PERFORMANCE: Cache các kết quả resolveImageData để tránh gọi File.exists()
 * nhiều lần trên Main Thread khi scroll. resolveImageData được gọi trong
 * remember(item.imageOriginal, item.imageNoBg), nhưng cache giúp giảm I/O
 * khi cùng một path được resolve cho nhiều items khác nhau.
 */
private val resolveCache = mutableMapOf<String, Any?>()

internal fun resolveImageData(context: Context, path: String): Any? {
    if (path.isBlank()) return null

    // ⚡ Cache hit → trả về ngay, tránh File I/O trên Main Thread
    resolveCache[path]?.let { return it }

    // URL http/https → dùng trực tiếp
    if (path.startsWith("http://") || path.startsWith("https://")) {
        resolveCache[path] = path
        return path
    }

    // Đường dẫn content:// hoặc file://
    if (path.startsWith("content://") || path.startsWith("file://")) {
        resolveCache[path] = Uri.parse(path)
        return Uri.parse(path)
    }

    // Đường dẫn tương đối của backend (vd: /uploads/xxx.jpg)
    // → ghép với BASE_URL hiện tại
    if (path.startsWith("/uploads/")) {
        val baseUrl = RetrofitClient.STYLEMATE_BASE_URL.trimEnd('/')
        val result = "$baseUrl$path"
        resolveCache[path] = result
        return result
    }

    // File local (tuyệt đối)
    val file = File(path)
    if (file.isAbsolute && file.exists()) {
        resolveCache[path] = file
        return file
    }

    // File trong filesDir
    val internalFile = File(context.filesDir, path)
    if (internalFile.exists()) {
        resolveCache[path] = internalFile
        return internalFile
    }

    // File trong filesDir/images/
    val imagesDir = File(context.filesDir, "images")
    val byName = File(imagesDir, File(path).name)
    if (byName.exists()) {
        resolveCache[path] = byName
        return byName
    }

    // Fallback: trả về path gốc
    resolveCache[path] = path
    return path
}

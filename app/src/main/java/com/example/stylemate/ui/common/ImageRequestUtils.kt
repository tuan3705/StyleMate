package com.example.stylemate.ui.common

import android.content.Context
import android.net.Uri
import java.io.File

internal fun resolveImageData(context: Context, path: String): Any? {
    if (path.isBlank()) return null

    if (path.startsWith("http://") || path.startsWith("https://")) {
        return path
    }

    if (path.startsWith("content://") || path.startsWith("file://")) {
        return Uri.parse(path)
    }

    val file = File(path)
    if (file.isAbsolute && file.exists()) return file

    val internalFile = File(context.filesDir, path)
    if (internalFile.exists()) return internalFile

    val imagesDir = File(context.filesDir, "images")
    val byName = File(imagesDir, File(path).name)
    if (byName.exists()) return byName

    return path
}

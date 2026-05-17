package com.example.stylemate.data.local

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ImageStorage {
    private const val IMAGE_DIR = "images"

    fun createImageFile(context: Context, prefix: String = "image_"): File {
        val directory = File(context.filesDir, IMAGE_DIR).apply { mkdirs() }
        return File(directory, "${prefix}${System.currentTimeMillis()}.jpg")
    }

    fun createImageUri(context: Context, imageFile: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    }

    suspend fun copyUriToInternalStorage(
        context: Context,
        sourceUri: Uri,
        prefix: String = "gallery_"
    ): File = withContext(Dispatchers.IO) {
        val destination = createImageFile(context, prefix)
        val inputStream = context.contentResolver.openInputStream(sourceUri)
            ?: throw IOException("Cannot open input stream for selected image")
        inputStream.use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
            }
        }
        destination
    }
}

package com.example.tradeup.utils

import android.content.Context
import android.net.Uri
import java.io.*

object FileUtils {
    fun getFilePathFromUri(context: Context, uri: Uri): String? {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = File.createTempFile("upload_", ".jpg", context.cacheDir)
        tempFile.outputStream().use { fileOut ->
            inputStream.copyTo(fileOut)
        }
        return tempFile.absolutePath
    }
}

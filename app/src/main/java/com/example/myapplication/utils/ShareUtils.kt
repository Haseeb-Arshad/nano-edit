package com.example.myapplication.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

fun shareImage(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share image"))
}

fun shareBitmapViaFileProvider(context: Context, bitmap: Bitmap, displayName: String = "share_${System.currentTimeMillis()}.jpg") {
    val cacheDir = File(context.cacheDir, "shared")
    if (!cacheDir.exists()) cacheDir.mkdirs()
    val outFile = File(cacheDir, displayName)
    FileOutputStream(outFile).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
    }
    val authority = context.packageName + ".fileprovider"
    val contentUri = FileProvider.getUriForFile(context, authority, outFile)
    shareImage(context, contentUri)
}

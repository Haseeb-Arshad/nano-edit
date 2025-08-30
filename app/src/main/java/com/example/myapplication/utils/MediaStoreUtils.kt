package com.example.myapplication.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import java.io.IOException

fun saveBitmapToGallery(context: Context, bmp: Bitmap, displayName: String): Uri? {
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AI Camera")
    }
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    try {
        uri?.let { outUri ->
            context.contentResolver.openOutputStream(outUri)?.use { os ->
                bmp.compress(Bitmap.CompressFormat.JPEG, 95, os)
            }
        }
    } catch (e: IOException) {
        return null
    }
    return uri
}


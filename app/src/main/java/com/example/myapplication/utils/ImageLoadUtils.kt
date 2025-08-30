package com.example.myapplication.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult

suspend fun loadBitmap(context: Context, data: Any): Bitmap? {
    val loader = ImageLoader.Builder(context)
        .build()
    val req = ImageRequest.Builder(context)
        .data(data)
        .allowHardware(false)
        .diskCachePolicy(CachePolicy.ENABLED)
        .build()
    val result = loader.execute(req)
    val drawable: Drawable = (result as? SuccessResult)?.drawable ?: return null
    return (drawable as? BitmapDrawable)?.bitmap
}


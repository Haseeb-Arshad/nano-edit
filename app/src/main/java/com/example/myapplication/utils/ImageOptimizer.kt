package com.example.myapplication.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min

/**
 * Image optimization utilities for better performance and memory management
 */
object ImageOptimizer {
    
    /**
     * Optimizes bitmap for display by scaling down if necessary
     */
    fun optimizeForDisplay(bitmap: Bitmap, maxWidth: Int = 1080, maxHeight: Int = 1920): Bitmap {
        if (bitmap.width <= maxWidth && bitmap.height <= maxHeight) {
            return bitmap
        }
        
        val ratio = min(
            maxWidth.toFloat() / bitmap.width,
            maxHeight.toFloat() / bitmap.height
        )
        
        val newWidth = (bitmap.width * ratio).toInt()
        val newHeight = (bitmap.height * ratio).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true).also {
            // Recycle original if it's different
            if (it != bitmap) {
                bitmap.recycle()
            }
        }
    }
    
    /**
     * Creates a thumbnail version of the bitmap for preview purposes
     */
    fun createThumbnail(bitmap: Bitmap, size: Int = 120): Bitmap {
        val aspectRatio = bitmap.height.toFloat() / bitmap.width.toFloat()
        val thumbnailHeight = (size * aspectRatio).toInt().coerceAtLeast(size)
        
        return Bitmap.createScaledBitmap(bitmap, size, thumbnailHeight, true)
    }
    
    /**
     * Loads bitmap from file path with memory optimization
     */
    suspend fun loadOptimizedBitmap(
        filePath: String,
        maxWidth: Int = 1080,
        maxHeight: Int = 1920
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            // First, get image dimensions without loading the full bitmap
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(filePath, options)
            
            // Calculate sample size to reduce memory usage
            val sampleSize = calculateSampleSize(options.outWidth, options.outHeight, maxWidth, maxHeight)
            
            // Load the bitmap with sample size
            val loadOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inJustDecodeBounds = false
                inPreferredConfig = Bitmap.Config.RGB_565 // Use less memory
            }
            
            BitmapFactory.decodeFile(filePath, loadOptions)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Calculates optimal sample size for bitmap loading
     */
    private fun calculateSampleSize(
        width: Int,
        height: Int,
        maxWidth: Int,
        maxHeight: Int
    ): Int {
        var sampleSize = 1
        
        if (height > maxHeight || width > maxWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            
            while ((halfHeight / sampleSize) >= maxHeight && (halfWidth / sampleSize) >= maxWidth) {
                sampleSize *= 2
            }
        }
        
        return sampleSize
    }
    
    /**
     * Rotates bitmap if needed based on EXIF data
     */
    fun rotateBitmapIfNeeded(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        if (rotationDegrees == 0) return bitmap
        
        val matrix = Matrix().apply {
            postRotate(rotationDegrees.toFloat())
        }
        
        return Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        ).also {
            if (it != bitmap) {
                bitmap.recycle()
            }
        }
    }
    
    /**
     * Safely recycles bitmap if it's not null and not recycled
     */
    fun safeBitmapRecycle(bitmap: Bitmap?) {
        bitmap?.let {
            if (!it.isRecycled) {
                it.recycle()
            }
        }
    }
    
    /**
     * Estimates memory usage of a bitmap
     */
    fun estimateMemoryUsage(width: Int, height: Int, config: Bitmap.Config = Bitmap.Config.ARGB_8888): Long {
        val bytesPerPixel = when (config) {
            Bitmap.Config.ARGB_8888 -> 4
            Bitmap.Config.RGB_565 -> 2
            Bitmap.Config.ARGB_4444 -> 2
            Bitmap.Config.ALPHA_8 -> 1
            else -> 4
        }
        return (width * height * bytesPerPixel).toLong()
    }
}
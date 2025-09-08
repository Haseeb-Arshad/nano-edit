package com.example.myapplication.utils

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis

/**
 * Performance monitoring utilities for debugging and optimization
 */
object PerformanceMonitor {
    
    private const val TAG = "PerformanceMonitor"
    private var isEnabled = false // Set to true for debugging
    
    /**
     * Measures execution time of a suspend function
     */
    suspend fun <T> measureSuspend(
        operation: String,
        block: suspend () -> T
    ): T {
        return if (isEnabled) {
            var result: T
            val time = measureTimeMillis {
                result = block()
            }
            Log.d(TAG, "$operation took ${time}ms")
            result
        } else {
            block()
        }
    }
    
    /**
     * Measures execution time of a regular function
     */
    fun <T> measure(
        operation: String,
        block: () -> T
    ): T {
        return if (isEnabled) {
            var result: T
            val time = measureTimeMillis {
                result = block()
            }
            Log.d(TAG, "$operation took ${time}ms")
            result
        } else {
            block()
        }
    }
    
    /**
     * Logs memory usage
     */
    fun logMemoryUsage(context: String) {
        if (!isEnabled) return
        
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val availableMemory = maxMemory - usedMemory
        
        Log.d(TAG, "$context - Memory: ${usedMemory / 1024 / 1024}MB used, ${availableMemory / 1024 / 1024}MB available")
    }
    
    /**
     * Tracks bitmap memory usage
     */
    fun trackBitmapMemory(operation: String, width: Int, height: Int) {
        if (!isEnabled) return
        
        val memoryUsage = ImageOptimizer.estimateMemoryUsage(width, height)
        Log.d(TAG, "$operation - Bitmap memory: ${memoryUsage / 1024 / 1024}MB (${width}x${height})")
    }
    
    /**
     * Enables performance monitoring (for debugging builds)
     */
    fun enable() {
        isEnabled = true
        Log.d(TAG, "Performance monitoring enabled")
    }
    
    /**
     * Disables performance monitoring
     */
    fun disable() {
        isEnabled = false
    }
    
    /**
     * Runs a performance test on image processing
     */
    fun runImageProcessingBenchmark(scope: CoroutineScope) {
        if (!isEnabled) return
        
        scope.launch(Dispatchers.Default) {
            Log.d(TAG, "Starting image processing benchmark...")
            
            // Test bitmap creation
            measure("Bitmap creation (1080x1920)") {
                val bitmap = android.graphics.Bitmap.createBitmap(1080, 1920, android.graphics.Bitmap.Config.ARGB_8888)
                bitmap.recycle()
            }
            
            // Test bitmap scaling
            val testBitmap = android.graphics.Bitmap.createBitmap(2160, 3840, android.graphics.Bitmap.Config.ARGB_8888)
            measure("Bitmap scaling (2160x3840 -> 1080x1920)") {
                val scaled = android.graphics.Bitmap.createScaledBitmap(testBitmap, 1080, 1920, true)
                scaled.recycle()
            }
            testBitmap.recycle()
            
            Log.d(TAG, "Image processing benchmark completed")
        }
    }
}
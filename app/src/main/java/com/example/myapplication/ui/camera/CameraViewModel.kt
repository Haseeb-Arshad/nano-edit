package com.example.myapplication.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.ai.AIImageProcessor
import com.example.myapplication.ai.MockAIImageProcessor
import com.example.myapplication.data.ImageAnalysisResult
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors

class CameraViewModel : ViewModel() {
    private val aiProcessor: AIImageProcessor = MockAIImageProcessor()
    
    var isAnalyzing by mutableStateOf(false)
        private set
    
    var lastAnalysisResult by mutableStateOf<ImageAnalysisResult?>(null)
        private set
    
    fun captureImage(
        imageCapture: ImageCapture,
        context: Context,
        onImageCaptured: (Bitmap) -> Unit
    ) {
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(
            File(context.cacheDir, "captured_image_${System.currentTimeMillis()}.jpg")
        ).build()
        
        imageCapture.takePicture(
            outputFileOptions,
            Executors.newSingleThreadExecutor(),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val bitmap = BitmapFactory.decodeFile(output.savedUri?.path)
                    bitmap?.let { 
                        onImageCaptured(it)
                        analyzeImage(it)
                    }
                }
                
                override fun onError(exception: ImageCaptureException) {
                    // Handle capture error
                }
            }
        )
    }
    
    private fun analyzeImage(bitmap: Bitmap) {
        viewModelScope.launch {
            isAnalyzing = true
            try {
                lastAnalysisResult = aiProcessor.analyzeImage(bitmap)
            } catch (e: Exception) {
                // Handle analysis error
            } finally {
                isAnalyzing = false
            }
        }
    }
    
    fun applyFilter(bitmap: Bitmap, filterName: String, onResult: (Bitmap) -> Unit) {
        viewModelScope.launch {
            try {
                val filteredBitmap = aiProcessor.applyFilter(bitmap, filterName)
                onResult(filteredBitmap)
            } catch (e: Exception) {
                // Handle filter error
            }
        }
    }
    
    fun enhanceImage(bitmap: Bitmap, prompt: String, onResult: (Bitmap) -> Unit) {
        viewModelScope.launch {
            try {
                val enhancedBitmap = aiProcessor.enhanceImage(bitmap, prompt)
                onResult(enhancedBitmap)
            } catch (e: Exception) {
                // Handle enhancement error
            }
        }
    }
}
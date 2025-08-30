package com.example.myapplication.ui.editor

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.ai.AIImageProcessor
import com.example.myapplication.ai.MockAIImageProcessor
import com.example.myapplication.data.ImageAnalysisResult
import kotlinx.coroutines.launch

class ImageEditorViewModel : ViewModel() {
    private val aiProcessor: AIImageProcessor = MockAIImageProcessor()
    
    var analysisResult by mutableStateOf<ImageAnalysisResult?>(null)
        private set
    
    var isProcessing by mutableStateOf(false)
        private set
    
    fun analyzeImage(bitmap: Bitmap) {
        viewModelScope.launch {
            isProcessing = true
            try {
                analysisResult = aiProcessor.analyzeImage(bitmap)
            } catch (e: Exception) {
                // Handle analysis error
            } finally {
                isProcessing = false
            }
        }
    }
    
    fun applyFilter(bitmap: Bitmap, filterName: String, onResult: (Bitmap) -> Unit) {
        viewModelScope.launch {
            isProcessing = true
            try {
                val filteredBitmap = aiProcessor.applyFilter(bitmap, filterName)
                onResult(filteredBitmap)
            } catch (e: Exception) {
                // Handle filter error
            } finally {
                isProcessing = false
            }
        }
    }
    
    fun enhanceImage(bitmap: Bitmap, prompt: String, onResult: (Bitmap) -> Unit) {
        viewModelScope.launch {
            isProcessing = true
            try {
                val enhancedBitmap = aiProcessor.enhanceImage(bitmap, prompt)
                onResult(enhancedBitmap)
            } catch (e: Exception) {
                // Handle enhancement error
            } finally {
                isProcessing = false
            }
        }
    }
}
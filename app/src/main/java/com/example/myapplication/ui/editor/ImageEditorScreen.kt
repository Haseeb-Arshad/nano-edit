package com.example.myapplication.ui.editor

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.controller.EditController
import androidx.compose.runtime.rememberCoroutineScope
import com.example.myapplication.data.FilterSuggestion
import com.example.myapplication.ai.MockAIImageProcessor
import kotlinx.coroutines.launch

@Composable
fun ImageEditorScreen(
    originalBitmap: Bitmap,
    onSaveImage: (Bitmap) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val aiProcessor = remember { MockAIImageProcessor() }
    var currentBitmap by remember { mutableStateOf(originalBitmap) }
    var showEnhancementDialog by remember { mutableStateOf(false) }
    
    var analysisResult by remember { mutableStateOf<com.example.myapplication.data.ImageAnalysisResult?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    
    LaunchedEffect(originalBitmap) {
        isProcessing = true
        analysisResult = aiProcessor.analyzeImage(originalBitmap)
        isProcessing = false
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("Cancel")
            }
            
            Button(
                onClick = { onSaveImage(currentBitmap) }
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save")
            }
        }
        
        // Image preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = currentBitmap.asImageBitmap(),
                contentDescription = "Edited image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            
            if (isProcessing) {
                Card(
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Processing...")
                    }
                }
            }
        }
        
        // AI Suggestions
        analysisResult?.let { result ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "AI Suggestions",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(result.suggestedFilters) { filter ->
                            SuggestionChip(filter) {
                                scope.launch {
                                    isProcessing = true
                                    currentBitmap = aiProcessor.applyFilter(currentBitmap, filter.name)
                                    isProcessing = false
                                }
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Enhancement controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { showEnhancementDialog = true }
            ) {
                Icon(Icons.Default.Star, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Smart Enhance")
            }
        }
    }
    
    if (showEnhancementDialog) {
        EnhancementDialog(
            onDismiss = { showEnhancementDialog = false },
            onEnhance = { prompt ->
                showEnhancementDialog = false
                scope.launch {
                    isProcessing = true
                    currentBitmap = aiProcessor.enhanceImage(currentBitmap, prompt)
                    isProcessing = false
                }
            }
        )
    }
}

@Composable
private fun SuggestionChip(
    filter: FilterSuggestion,
    onClick: () -> Unit
) {
    FilterChip(
        onClick = onClick,
        label = { Text(filter.name) },
        selected = false
    )
}

@Composable
private fun EnhancementDialog(
    onDismiss: () -> Unit,
    onEnhance: (String) -> Unit
) {
    var prompt by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI Enhancement") },
        text = {
            Column {
                Text("Describe how you want to enhance this image:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    placeholder = { Text("e.g., make it brighter, add dramatic lighting...") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onEnhance(prompt) },
                enabled = prompt.isNotBlank()
            ) {
                Text("Enhance")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

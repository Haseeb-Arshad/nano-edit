package com.example.myapplication.ui.editor

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Save
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

@Composable
fun ImageEditorScreen(
    originalBitmap: Bitmap,
    onSaveImage: (Bitmap) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val controller = remember { EditController(scope) }
    var currentBitmap by remember { mutableStateOf(originalBitmap) }
    var showEnhancementDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(originalBitmap) {
        controller.analyzeImage(originalBitmap)
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
                Icon(Icons.Default.Save, contentDescription = null)
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
            
            if (controller.isProcessing) {
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
        controller.analysisResult?.let { result ->
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
                                controller.applyFilter(currentBitmap, filter.name) { newBitmap ->
                                    currentBitmap = newBitmap
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
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Enhance")
            }
        }
    }
    
    if (showEnhancementDialog) {
        EnhancementDialog(
            onDismiss = { showEnhancementDialog = false },
            onEnhance = { prompt ->
                showEnhancementDialog = false
                controller.enhanceImage(currentBitmap, prompt) { newBitmap ->
                    currentBitmap = newBitmap
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

package com.example.myapplication.ui.screens

import android.net.Uri
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.myapplication.controller.EditController
import com.example.myapplication.ui.components.GlassButton
import com.example.myapplication.ui.components.GlassCard
import com.example.myapplication.ui.components.GlassSurface
import com.example.myapplication.ui.filters.FilterPresets
import com.example.myapplication.ui.filters.FilterPreset
import com.example.myapplication.ui.filters.applyPreset
import com.example.myapplication.utils.loadBitmap
import com.example.myapplication.utils.saveBitmapToGallery
import com.example.myapplication.utils.quickEnhance
import kotlinx.coroutines.launch

@Composable
fun EditorScreen(
    src: Uri?,
    controller: EditController
) {
    val ctx = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val state = controller.state.collectAsState(initial = com.example.myapplication.data.EditUiState()).value
    var original by remember { mutableStateOf<Bitmap?>(null) }
    var working by remember { mutableStateOf<Bitmap?>(null) }
    var comparing by remember { mutableStateOf(false) }
    var intensity by remember { mutableStateOf(0.6f) }
    var activePreset by remember { mutableStateOf<FilterPreset?>(null) }
    var thumbnails by remember { mutableStateOf<Map<String, Bitmap>>(emptyMap()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(src) {
        src?.let {
            controller.setSource(it)
            original = loadBitmap(ctx, it.toString())
            working = original?.let { bmp -> quickEnhance(bmp, 0.5f) }
            // Precompute thumbnails for presets
            original?.let { srcBmp ->
                thumbnails = withContext(Dispatchers.Default) {
                    val base = Bitmap.createScaledBitmap(srcBmp, 160, (160f * srcBmp.height / srcBmp.width).toInt().coerceAtLeast(160), true)
                    FilterPresets.All.associate { preset ->
                        val prev = applyPreset(base, preset, 0.7f)
                        preset.id to prev
                    }
                }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Top bar (glass)
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            GlassButton(onClick = { /* navigate back via activity */ }, modifier = Modifier) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                Spacer(Modifier.width(6.dp))
                Text("Back")
            }
            Text("Editor", style = MaterialTheme.typography.titleLarge)
            GlassButton(onClick = {
                scope.launch {
                    working?.let { saveBitmapToGallery(ctx, it, "AICam_Edit") }
                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                }
            }) {
                Icon(Icons.Default.Check, contentDescription = "Save")
                Spacer(Modifier.width(6.dp))
                Text("Save")
            }
        }

        // Image area
Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(12.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { comparing = true },
                        onPress = {
                            comparing = true
                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            tryAwaitRelease()
                            comparing = false
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Main image area with compare on long-press
            val show = if (comparing) original else working
            androidx.compose.animation.AnimatedContent(
                targetState = show,
                modifier = Modifier.fillMaxSize()
            ) { bmp ->
                bmp?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Preview",
                        modifier = Modifier.fillMaxSize()
                    )
                } ?: run {
                    GlassCard { Box(Modifier.padding(16.dp)) { Text("Loading…") } }
                }
            }

            // Compare hint overlay
androidx.compose.animation.AnimatedVisibility(
                visible = working != null && !comparing,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                GlassCard { Box(Modifier.padding(8.dp)) { Text("Long-press to compare", style = MaterialTheme.typography.labelMedium) } }
            }

            // Progress overlay when applying AI
androidx.compose.animation.AnimatedVisibility(
                visible = state.isLoading,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                GlassCard {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        androidx.compose.material3.CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text("Enhancing…")
                    }
                }
            }
        }

        // Prompt input
        GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text("Describe enhancement", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = state.prompt,
                    onValueChange = { controller.setPrompt(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g., brighten, cinematic tone, sharpen details…") }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Presets carousel
        GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text("Presets", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(FilterPresets.All) { preset ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val thumb = thumbnails[preset.id]
                            GlassCard {
                                Box(Modifier.padding(4.dp)) {
                                    if (thumb != null) {
                                        Image(
                                            bitmap = thumb.asImageBitmap(),
                                            contentDescription = preset.name,
                                            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp))
                                        )
                                    } else {
                                        Text("…", modifier = Modifier.size(72.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            GlassButton(
                                onClick = {
                                    activePreset = preset
                                    original?.let { srcBmp ->
                                        working = applyPreset(srcBmp, preset, intensity)
                                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    }
                                },
                                shape = if (preset == activePreset) MaterialTheme.shapes.medium else MaterialTheme.shapes.small
                            ) {
                                Text(
                                    preset.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (preset == activePreset) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Intensity", style = MaterialTheme.typography.labelMedium)
                        Text("${(intensity * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.height(4.dp))
                    Slider(
                        value = intensity,
                        onValueChange = { v ->
                            intensity = v
                            activePreset?.let { p ->
                                original?.let { srcBmp -> working = applyPreset(srcBmp, p, v) }
                                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Bottom actions (AI Enhance)
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassButton(onClick = {
                // Apply remote AI edit via controller
                controller.applyEdit(offline = false)
            }) {
                Icon(Icons.Default.Star, contentDescription = "AI")
                Spacer(Modifier.width(8.dp))
                Text("AI Enhance")
            }
            androidx.compose.animation.AnimatedVisibility(visible = state.resultUrl != null) {
GlassButton(onClick = {
                    val url = state.resultUrl ?: return@GlassButton
                    // Replace working with downloaded result
                    scope.launch {
                        val bmp = com.example.myapplication.utils.loadBitmap(ctx, url)
                        if (bmp != null) working = bmp
                    }
                }) {
                    Text("Use AI Result")
                }
            }
        }
    }
}

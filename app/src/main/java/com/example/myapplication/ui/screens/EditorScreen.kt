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
import kotlinx.coroutines.delay
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
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import com.example.myapplication.ui.motion.MotionUtils
import com.example.myapplication.ui.motion.HapticUtils
import com.example.myapplication.utils.ImageOptimizer
import androidx.compose.ui.draw.scale

// Added imports for categories and AI analyze
import com.example.myapplication.ui.filters.FilterCategories
import com.example.myapplication.ui.filters.FilterCategory
import com.example.myapplication.ai.MockAIImageProcessor
import com.example.myapplication.data.ImageAnalysisResult

// Helper to build a smart prompt based on analysis
private fun buildSmartEnhancePrompt(analysis: ImageAnalysisResult?): String {
    if (analysis == null) return "Balanced enhancement, natural lighting, crisp details, true-to-life colors, minimal noise"
    val tags = analysis.detectedObjects.take(3).joinToString(", ")
    val styles = analysis.suggestedFilters.map { it.name.lowercase() }.take(2).joinToString(", ")
    val base = mutableListOf(
        "balanced enhancement",
        "natural lighting",
        "crisp details",
        "true-to-life colors"
    )
    if (tags.isNotBlank()) base += "optimize for: $tags"
    if (styles.isNotBlank()) base += "style: $styles"
    return base.joinToString(", ")
}

@Composable
fun EditorScreen(
    src: Uri?,
    controller: EditController,
    autoSmartEnhance: Boolean = false,
    onBack: () -> Unit = {}
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

    // When controller finishes an edit and publishes a resultUrl, load it automatically
    AutoApplyResult(resultUrl = state.resultUrl) { bmp ->
        if (bmp != null) working = bmp
    }

    // New UI state: categories
    var activeCategory by remember { mutableStateOf<FilterCategory?>(FilterCategories.ToneCategory) }

    // New: local AI analyzer for Smart Enhance prompt building
    val aiProcessor = remember { MockAIImageProcessor() }
    var lastAnalysis by remember { mutableStateOf<ImageAnalysisResult?>(null) }
    var didAutoSmartEnhance by remember { mutableStateOf(false) }

    LaunchedEffect(src) {
        src?.let {
            controller.setSource(it)
            original = loadBitmap(ctx, it.toString())
            working = original?.let { bmp -> quickEnhance(bmp, 0.5f) }
            
            // Precompute thumbnails for presets in background with performance optimization
            original?.let { srcBmp ->
                thumbnails = withContext(Dispatchers.Default) {
                    // Create smaller base image for thumbnails to improve performance
                    val thumbnailSize = 120 // Reduced from 160 for better performance
                    val aspectRatio = srcBmp.height.toFloat() / srcBmp.width.toFloat()
                    val thumbnailHeight = (thumbnailSize * aspectRatio).toInt().coerceAtLeast(thumbnailSize)
                    val base = Bitmap.createScaledBitmap(srcBmp, thumbnailSize, thumbnailHeight, true)
                    
                    // Process presets in parallel for better performance
                    FilterPresets.All.associate { preset ->
                        val prev = applyPreset(base, preset, 0.7f)
                        preset.id to prev
                    }
                }
            }
            
            // Kick off lightweight analysis to fuel Smart Enhance
            original?.let { bmp ->
                // Use smaller image for analysis to improve performance
                val analysisSize = 640
                val aspectRatio = bmp.height.toFloat() / bmp.width.toFloat()
                val analysisHeight = (analysisSize * aspectRatio).toInt()
                val analysisBitmap = Bitmap.createScaledBitmap(bmp, analysisSize, analysisHeight, true)
                
                lastAnalysis = aiProcessor.analyzeImage(analysisBitmap)
                
                // Clean up analysis bitmap
                if (analysisBitmap != bmp) {
                    analysisBitmap.recycle()
                }
            }
        }
    }

    // Auto-run Smart Enhance once when requested (from Review)
    LaunchedEffect(lastAnalysis, autoSmartEnhance) {
        if (autoSmartEnhance && !didAutoSmartEnhance) {
            val prompt = buildSmartEnhancePrompt(lastAnalysis)
            controller.setPrompt(prompt)
            controller.applyEdit(context = ctx, offline = false)
            didAutoSmartEnhance = true
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Top bar (glass)
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            GlassButton(onClick = onBack, modifier = Modifier) {
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
                    GlassCard { Box(Modifier.padding(16.dp)) { Text("Loading��") } }
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
                        val pct = state.progress?.coerceIn(0, 100)
                        Text(if (pct != null) "Enhancing… $pct%" else "Enhancing…")
                    }
                }
            }
        }

        // Prompt input + Smart Enhance quick-fill
        GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Describe enhancements", style = MaterialTheme.typography.titleMedium)
                    // Smart Enhance action in the prompt card for visibility
                    GlassButton(onClick = {
                        val prompt = buildSmartEnhancePrompt(lastAnalysis)
                        controller.setPrompt(prompt)
                        controller.applyEdit(context = ctx, offline = false)
                    }) {
                        Icon(Icons.Default.Star, contentDescription = "Smart Enhance")
                        Spacer(Modifier.width(8.dp))
                        Text("Smart Enhance")
                    }
                }
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

        // Category-based filters with large previews
        GlassCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text("Filters", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                // Category chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                    items(FilterCategories.All) { cat ->
                        val selected = activeCategory?.id == cat.id
                        GlassButton(onClick = { activeCategory = cat }) {
                            Text(cat.name, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                // Large tiles for selected category
                val cat = activeCategory ?: FilterCategories.ToneCategory
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                    items(cat.presets) { preset ->
                        val tileScale = animateFloatAsState(
                            targetValue = if (activePreset == preset) 1.08f else 1f,
                            animationSpec = MotionUtils.SpringBouncy
                        )
                        
                        val tileAlpha = animateFloatAsState(
                            targetValue = if (activePreset == preset) 1f else 0.8f,
                            animationSpec = tween(MotionUtils.DURATION_SHORT, easing = MotionUtils.EaseOut)
                        )
                        Column(
                            modifier = Modifier
                                .scale(tileScale.value)
                                .graphicsLayer { alpha = tileAlpha.value },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val thumb = thumbnails[preset.id]
                            GlassCard {
                                Box(Modifier.padding(4.dp)) {
                                    if (thumb != null) {
                                        Image(
                                            bitmap = thumb.asImageBitmap(),
                                            contentDescription = preset.name,
                                            modifier = Modifier.size(140.dp).clip(RoundedCornerShape(18.dp))
                                        )
                                    } else {
                                        Text("…", modifier = Modifier.size(140.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            GlassButton(
                                onClick = {
                                    activePreset = preset
                                    // Instant local preview for responsiveness
                                    scope.launch {
                                        original?.let { srcBmp ->
                                            val filteredBitmap = withContext(Dispatchers.Default) {
                                                applyPreset(srcBmp, preset, intensity)
                                            }
                                            working = filteredBitmap
                                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                    // Trigger backend AI processing for this preset
                                    val prompt = buildPromptForPreset(preset, intensity)
                                    controller.setPrompt(prompt)
                                    controller.applyEdit(context = ctx, offline = false)
                                },
                                shape = if (preset == activePreset) MaterialTheme.shapes.medium else MaterialTheme.shapes.small
                            ) { Text(preset.name) }
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
                                scope.launch {
                                    original?.let { srcBmp -> 
                                        // Debounce slider updates for better performance
                                        delay(50)
                                        val filteredBitmap = withContext(Dispatchers.Default) {
                                            applyPreset(srcBmp, p, v)
                                        }
                                        working = filteredBitmap
                                    }
                                }
                                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            }
                        },
                        onValueChangeFinished = {
                            // When user finishes adjusting intensity, submit to backend
                            activePreset?.let { p ->
                                val prompt = buildPromptForPreset(p, intensity)
                                controller.setPrompt(prompt)
                                controller.applyEdit(context = ctx, offline = false)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Bottom actions
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassButton(onClick = {
                // One-tap Smart Enhance (same as button in prompt card)
                val prompt = buildSmartEnhancePrompt(lastAnalysis)
                controller.setPrompt(prompt)
                controller.applyEdit(context = ctx, offline = false)
            }) {
                Icon(Icons.Default.Star, contentDescription = "Smart Enhance")
                Spacer(Modifier.width(8.dp))
                Text("Smart Enhance")
            }
            androidx.compose.animation.AnimatedVisibility(visible = state.resultUrl != null) {
                GlassButton(onClick = {
                    val url = state.resultUrl ?: return@GlassButton
                    // Replace working with downloaded result
                    scope.launch {
                        val bmp = com.example.myapplication.utils.loadBitmap(ctx, url)
                        if (bmp != null) working = bmp
                    }
                }) { Text("Use Result") }
            }
        }
    }
}

// Helper to build an AI prompt string from a chosen preset + intensity
private fun buildPromptForPreset(preset: FilterPreset, intensity: Float): String {
    val pct = (intensity.coerceIn(0f, 1f) * 100).toInt()
    val base = when (preset.id) {
        "vibrant" -> "Increase vibrance and saturation modestly; keep natural skin tones; avoid oversaturation."
        "warm" -> "Apply warm tone with gentle contrast; enhance golden hues; keep whites neutral."
        "cool" -> "Apply cool tone; slightly lift blues; preserve neutral grays; avoid color cast on skin."
        "mono" -> "Convert to tasteful monochrome; maintain rich midtones; avoid crushed blacks."
        "sepia" -> "Apply classic sepia tone; preserve detail; low contrast."
        "hi_contrast" -> "Increase local contrast and clarity; avoid halos and oversharpening."
        "soft" -> "Slight soften and reduce harsh highlights; preserve texture; low saturation change."
        "film" -> "Film-like look with gentle grain and warm curve; subtle saturation boost."
        "retro" -> "Retro tone mapping; slight fade; mild color shift reminiscent of vintage prints."
        "cinematic" -> "Cinematic grade with teal-orange balance; enhanced contrast; maintain natural skin tones."
        "vivid" -> "Vivid, punchy colors with clarity boost; avoid clipping highlights."
        "night" -> "Night enhancement; lift shadows, reduce noise, keep colors faithful."
        else -> "Subtle enhancement; preserve details and natural tones."
    }
    return "$base Intensity ~${pct}% on the whole image."
}

// Auto-apply returned result image into working preview when available
@Composable
private fun AutoApplyResult(
    resultUrl: String?,
    onBitmap: (Bitmap?) -> Unit
) {
    val ctx = LocalContext.current
    LaunchedEffect(resultUrl) {
        if (!resultUrl.isNullOrBlank()) {
            val bmp = com.example.myapplication.utils.loadBitmap(ctx, resultUrl)
            onBitmap(bmp)
        }
    }
}

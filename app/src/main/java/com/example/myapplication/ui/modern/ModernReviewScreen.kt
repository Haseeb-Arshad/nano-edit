package com.example.myapplication.ui.modern

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.filters.RealTimeFilterEngine
import com.example.myapplication.data.EditRepository
import com.example.myapplication.ui.components.AnimatedGradientBackground
import com.example.myapplication.ui.components.GlassmorphicCard
import com.example.myapplication.utils.ImageSaver
import com.example.myapplication.utils.loadBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernReviewScreen(
    uri: Uri?,
    repository: EditRepository,
    onEdit: () -> Unit,
    onSceneLift: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var original by remember(uri) { mutableStateOf<Bitmap?>(null) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var isComparing by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var splitView by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableStateOf(0) }
    var exportError by remember { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }

    // Load original lazily
    LaunchedEffect(uri) {
        if (uri == null) return@LaunchedEffect
        val bmp = loadBitmap(context, uri)
        original = bmp
        preview = bmp
    }

    // Suggestion specs
    val suggestions = remember {
        listOf(
            SuggestionSpec(
                id = "noise_clean",
                title = "Clean Noise",
                why = "Low-light cleanup with texture preserved",
                render = { engine, bmp, strength -> engine.applyBeautyFilter(bmp, 0.35f * strength) }
            ),
            SuggestionSpec(
                id = "green_boost",
                title = "Green Boost",
                why = "Boost foliage and greens naturally",
                render = { _, bmp, strength -> adjustChannels(bmp, r = 1f, g = 1.25f * strength, b = 1f) }
            ),
            SuggestionSpec(
                id = "portrait_smooth",
                title = "Portrait Smooth",
                why = "Skin-aware smoothing at low strength",
                render = { engine, bmp, strength -> engine.applyBeautyFilter(bmp, 0.25f * strength) }
            ),
            SuggestionSpec(
                id = "film_400",
                title = "Film 400",
                why = "Grain + warm tone curve",
                render = { engine, bmp, strength -> engine.applyVintageFilter(bmp, 0.5f * strength) }
            ),
            SuggestionSpec(
                id = "hdr_pop",
                title = "HDR Pop",
                why = "Local contrast and tone curve",
                render = { _, bmp, strength -> adjustContrast(bmp, 1f + 0.25f * strength) }
            )
        )
    }

    // Thumbnails cache and applied state
    var thumbs by remember { mutableStateOf<Map<String, Bitmap>>(emptyMap()) }
    var appliedId by remember { mutableStateOf<String?>(null) }
    var strengths by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    data class AppliedLayer(val id: String, val title: String, val strength: Float, val preview: Bitmap)
    var layers by remember { mutableStateOf<List<AppliedLayer>>(emptyList()) }

    // Generate thumbnails off main thread
    LaunchedEffect(original) {
        val src = original ?: return@LaunchedEffect
        val small = withContext(Dispatchers.Default) {
            val max = 320
            val ratio = minOf(max / src.width.toFloat(), max / src.height.toFloat())
            val w = (src.width * ratio).toInt().coerceAtLeast(64)
            val h = (src.height * ratio).toInt().coerceAtLeast(64)
            Bitmap.createScaledBitmap(src, w, h, true)
        }
        val engine = RealTimeFilterEngine(context)
        val out = suggestions.associate { s ->
            s.id to withContext(Dispatchers.Default) {
                val cfg = small.config ?: Bitmap.Config.ARGB_8888
                s.render(engine, small.copy(cfg, true), 1f)
            }
        }
        thumbs = out
        engine.cleanup()
    }

    // Auto-scene detection to preselect a suggestion on open (best-effort)
    var didAuto by remember { mutableStateOf(false) }
    LaunchedEffect(original) {
        val src = original ?: return@LaunchedEffect
        if (didAuto) return@LaunchedEffect
        try {
            val small = withContext(Dispatchers.Default) {
                val max = 320
                val ratio = minOf(max / src.width.toFloat(), max / src.height.toFloat())
                val w = (src.width * ratio).toInt().coerceAtLeast(64)
                val h = (src.height * ratio).toInt().coerceAtLeast(64)
                Bitmap.createScaledBitmap(src, w, h, true)
            }
            val image = com.google.mlkit.vision.common.InputImage.fromBitmap(small, 0)
            val labeler = com.google.mlkit.vision.label.ImageLabeling.getClient(
                com.google.mlkit.vision.label.defaults.ImageLabelerOptions.DEFAULT_OPTIONS
            )
            val result = labeler.process(image).await()
            val labels = result.map { it.text.lowercase() }
            val pick = when {
                labels.any { it.contains("person") || it.contains("selfie") || it.contains("face") } -> "portrait_smooth"
                labels.any { it.contains("plant") || it.contains("leaf") || it.contains("tree") || it.contains("green") } -> "green_boost"
                labels.any { it.contains("night") || it.contains("dark") || it.contains("low light") } -> "noise_clean"
                else -> "hdr_pop"
            }
            val spec = suggestions.firstOrNull { it.id == pick } ?: return@LaunchedEffect
            val engine = RealTimeFilterEngine(context)
            val resultBmp = withContext(Dispatchers.Default) {
                val cfg = src.config ?: Bitmap.Config.ARGB_8888
                spec.render(engine, src.copy(cfg, true), 1f)
            }
            preview = resultBmp
            appliedId = spec.id
            engine.cleanup()
            didAuto = true
        } catch (_: Throwable) { /* ignore */ }
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedGradientBackground(Modifier.fillMaxSize())

        // Image preview with compare press-and-hold
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(preview, original, splitView) {
                    if (!splitView) {
                        detectTapGestures(
                            onPress = {
                                isComparing = true
                                tryAwaitRelease()
                                isComparing = false
                            }
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (splitView && original != null && preview != null) {
                com.example.myapplication.ui.components.CompareSlider(
                    modifier = Modifier.fillMaxSize(),
                    original = original!!.asImageBitmap(),
                    enhanced = preview!!.asImageBitmap()
                )
            } else {
                val show = if (isComparing) original else preview
                show?.let { Image(bitmap = it.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize()) }
            }
        }

        // Post-capture suggestions sheet
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ModalBottomSheet(
            onDismissRequest = { onBack() },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.15f),
            scrimColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "AI Suggestions",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(suggestions) { spec ->
                        SuggestionCard(
                            title = spec.title,
                            why = spec.why,
                            thumbnail = thumbs[spec.id],
                            applied = appliedId == spec.id,
                            strength = strengths[spec.id] ?: 1f,
                            onStrengthChange = { s -> strengths = strengths.toMutableMap().apply { put(spec.id, s) } },
                            onApply = {
                                val base = (preview ?: original) ?: return@SuggestionCard
                                val engine = RealTimeFilterEngine(context)
                                scope.launch {
                                    val result = withContext(Dispatchers.Default) {
                                        val s = strengths[spec.id] ?: 1f
                                        val cfg = base.config ?: Bitmap.Config.ARGB_8888
                                        spec.render(engine, base.copy(cfg, true), s)
                                    }
                                    preview = result
                                    appliedId = spec.id
                                    // Append layer for version timeline
                                    val thumb = withContext(Dispatchers.Default) {
                                        val max = 160
                                        val ratio = minOf(max / result.width.toFloat(), max / result.height.toFloat())
                                        val w = (result.width * ratio).toInt().coerceAtLeast(40)
                                        val h = (result.height * ratio).toInt().coerceAtLeast(40)
                                        Bitmap.createScaledBitmap(result, w, h, true)
                                    }
                                    layers = layers + AppliedLayer(spec.id, spec.title, strengths[spec.id] ?: 1f, thumb)
                                    engine.cleanup()
                                }
                            }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Versions timeline (horizontal)
                if (layers.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Versions", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f))
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            AssistChip(
                                onClick = { preview = original; layers = emptyList(); appliedId = null },
                                label = { Text("Original") }
                            )
                        }
                        items(layers) { layer ->
                            ElevatedAssistChip(
                                onClick = {
                                    // Revert to this layer: truncate layers after it and show its preview
                                    val idx = layers.indexOfFirst { it === layer }
                                    if (idx >= 0) {
                                        preview = layer.preview
                                        layers = layers.take(idx + 1)
                                        appliedId = layer.id
                                    }
                                },
                                label = { Text(layer.title) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Bottom toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(onClick = onEdit) {
                        Icon(Icons.Outlined.Edit, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Edit")
                    }
                    FilledTonalButton(onClick = { splitView = !splitView }, enabled = (original != null && preview != null)) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (splitView) "Split On" else "Split View")
                    }
                    FilledTonalButton(onClick = { preview = original; appliedId = null }) {
                        Icon(Icons.Outlined.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Reset")
                    }
                    // Export Full‑Res via NanoBanana
                    FilledTonalButton(onClick = {
                        val srcUri = uri ?: return@FilledTonalButton
                        val prompt = buildPromptFromLayers(layers.map { it.title to it.strength })
                        exportError = null
                        exporting = true
                        exportProgress = 0
                        scope.launch {
                            val result = repository.uploadFullResAndPoll(context, srcUri, prompt) { p -> exportProgress = p }
                            exporting = false
                            result.url?.let { url ->
                                // Download final and save to Gallery
                                val bmp = loadBitmap(context, url)
                                if (bmp != null) {
                                    ImageSaver.saveImageToGallery(context, bmp, "AICam_FullRes_${System.currentTimeMillis()}")
                                    // Share CTA
                                    val res = snackbar.showSnackbar(
                                        message = "Exported successfully",
                                        actionLabel = "Share now"
                                    )
                                    if (res == SnackbarResult.ActionPerformed) {
                                        com.example.myapplication.utils.shareBitmapViaFileProvider(
                                            context, bmp, "AICam_Export_${System.currentTimeMillis()}.jpg"
                                        )
                                    }
                                }
                            }
                            if (result.error != null) exportError = result.error
                        }
                    }) {
                        if (exporting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Export ${exportProgress}%")
                        } else {
                            Icon(Icons.Outlined.Download, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Export Full‑Res")
                        }
                    }
                    FilledTonalButton(onClick = {
                        val bmp = preview ?: original ?: return@FilledTonalButton
                        scope.launch {
                            saving = true
                            ImageSaver.saveImageToGallery(context, bmp, "AICam_${System.currentTimeMillis()}")
                            saving = false
                        }
                    }) {
                        Icon(Icons.Outlined.Download, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (saving) "Saving…" else "Save")
                    }
                    FilledTonalButton(onClick = {
                        val bmp = preview ?: original ?: return@FilledTonalButton
                        scope.launch {
                            com.example.myapplication.utils.shareBitmapViaFileProvider(
                                context, bmp, "AICamShare_${System.currentTimeMillis()}.jpg"
                            )
                        }
                    }) {
                        Icon(Icons.Outlined.Share, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Share")
                    }
                }
                if (exportError != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = exportError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Snackbar host overlay
        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp))
    }
}

private data class SuggestionSpec(
    val id: String,
    val title: String,
    val why: String,
    val render: (RealTimeFilterEngine, Bitmap, Float) -> Bitmap
)

private fun buildPromptFromLayers(layers: List<Pair<String, Float>>): String {
    if (layers.isEmpty()) {
        return "Subtle enhancement; preserve details and natural tones; light noise reduction; balanced contrast."
    }
    val steps = layers.joinToString(", ") { (title, strength) ->
        val pct = (strength * 100).toInt().coerceIn(1, 100)
        "$title (~${pct}%)"
    }
    return "Apply in order: $steps. Keep skin tones natural, avoid halos and oversharpening, preserve textures, export full resolution."
}

// Preview-only adjustments
private fun adjustChannels(bitmap: Bitmap, r: Float, g: Float, b: Float): Bitmap {
    val cm = android.graphics.ColorMatrix().apply {
        set(floatArrayOf(
            r, 0f, 0f, 0f, 0f,
            0f, g, 0f, 0f, 0f,
            0f, 0f, b, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
    }
    val paint = android.graphics.Paint().apply { colorFilter = android.graphics.ColorMatrixColorFilter(cm) }
    val out = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(out)
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    return out
}

private fun adjustContrast(bitmap: Bitmap, contrast: Float): Bitmap {
    val translate = ((-0.5f * contrast + 0.5f) * 255f)
    val cm = android.graphics.ColorMatrix().apply {
        set(floatArrayOf(
            contrast, 0f, 0f, 0f, translate,
            0f, contrast, 0f, 0f, translate,
            0f, 0f, contrast, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
    }
    val paint = android.graphics.Paint().apply { colorFilter = android.graphics.ColorMatrixColorFilter(cm) }
    val out = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(out)
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    return out
}

// Local await for ML Kit Task without adding extra dependency
private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
    kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        addOnSuccessListener { res -> cont.resume(res) {} }
        addOnFailureListener { e -> cont.resumeWith(Result.failure(e)) }
        addOnCanceledListener { cont.cancel() }
    }

@Composable
private fun SuggestionCard(
    title: String,
    why: String,
    thumbnail: Bitmap?,
    applied: Boolean,
    strength: Float,
    onStrengthChange: (Float) -> Unit,
    onApply: () -> Unit
) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(120.dp, 80.dp).clip(RoundedCornerShape(12.dp))) {
                    thumbnail?.let { Image(bitmap = it.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize()) }
                    androidx.compose.animation.AnimatedVisibility(visible = thumbnail == null, enter = fadeIn(), exit = fadeOut()) {
                        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(4.dp))
                    Text(why, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                }
                Button(onClick = onApply, enabled = !applied) {
                    if (applied) Icon(Icons.Outlined.Check, contentDescription = null) else Text("Apply")
                }
            }
            // Strength slider with numeric bubble
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Strength", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                    Text("${(strength * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                Slider(value = strength, onValueChange = onStrengthChange)
            }
        }
    }
}

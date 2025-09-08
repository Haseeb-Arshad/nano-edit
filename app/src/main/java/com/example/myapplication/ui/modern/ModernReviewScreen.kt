package com.example.myapplication.ui.modern

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.ui.layout.ContentScale
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    var aiSheet by remember { mutableStateOf(false) }
    var labelsCache by remember { mutableStateOf<List<String>>(emptyList()) }

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
            labelsCache = labels
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

        // Bottom filters + actions panel
        GlassmorphicCard(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            cornerRadius = 20.dp
        ) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Top actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(onClick = { /* open AI sheet */ aiSheet = true }, label = { Text("AI Suggest") }, leadingIcon = { Icon(Icons.Outlined.AutoAwesome, null) })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = { splitView = !splitView }, label = { Text(if (splitView) "Compare" else "Compare") })
                        AssistChip(onClick = {
                            val bmp = preview ?: original ?: return@AssistChip
                            scope.launch { saving = true; ImageSaver.saveImageToGallery(context, bmp, "AICam_Edit_${System.currentTimeMillis()}"); saving = false; snackbar.showSnackbar("Saved to Gallery") }
                        }, label = { Text(if (saving) "Saving…" else "Save") })
                    }
                }

                // Swipeable filter carousel with snapping
                val state = rememberLazyListState()
                val fling = rememberSnapFlingBehavior(lazyListState = state)
                LazyRow(
                    state = state,
                    flingBehavior = fling,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    items(suggestions) { spec ->
                        FilterThumb(
                            title = spec.title,
                            bitmap = thumbs[spec.id],
                            selected = appliedId == spec.id,
                            onClick = {
                                val base = (original ?: return@FilterThumb)
                                val engine = RealTimeFilterEngine(context)
                                scope.launch {
                                    val result = withContext(Dispatchers.Default) {
                                        val cfg = base.config ?: Bitmap.Config.ARGB_8888
                                        val s = strengths[spec.id] ?: 1f
                                        spec.render(engine, base.copy(cfg, true), s)
                                    }
                                    preview = result
                                    appliedId = spec.id
                                    engine.cleanup()
                                }
                            }
                        )
                    }
                }
            }
        }

        // AI sheet (recommendation-based filters)
        if (aiSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { aiSheet = false },
                sheetState = sheetState,
                scrimColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Suggested Filters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    val picks = remember(labelsCache, suggestions) {
                        val ls = labelsCache
                        val ordered = buildList<SuggestionSpec> {
                            if (ls.any { it.contains("person") || it.contains("selfie") || it.contains("face") }) add(suggestions.first { it.id == "portrait_smooth" })
                            if (ls.any { it.contains("plant") || it.contains("leaf") || it.contains("tree") || it.contains("green") }) add(suggestions.first { it.id == "green_boost" })
                            if (ls.any { it.contains("night") || it.contains("dark") || it.contains("low light") }) add(suggestions.first { it.id == "noise_clean" })
                            add(suggestions.first { it.id == "hdr_pop" })
                            add(suggestions.first { it.id == "film_400" })
                        }
                        ordered.distinctBy { it.id }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
                        items(picks) { spec ->
                            FilterThumb(title = spec.title, bitmap = thumbs[spec.id], selected = appliedId == spec.id) {
                                val base = (original ?: return@FilterThumb)
                                val engine = RealTimeFilterEngine(context)
                                scope.launch {
                                    val result = withContext(Dispatchers.Default) {
                                        val cfg = base.config ?: Bitmap.Config.ARGB_8888
                                        val s = strengths[spec.id] ?: 1f
                                        spec.render(engine, base.copy(cfg, true), s)
                                    }
                                    preview = result
                                    appliedId = spec.id
                                    engine.cleanup()
                                    aiSheet = false
                                }
                            }
                        }
                    }
                }
            }
        }

        // Snackbar host overlay
        SnackbarHost(hostState = snackbar, modifier = Modifier
            .align(Alignment.BottomCenter)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = 16.dp))
    }
}

private data class SuggestionSpec(
    val id: String,
    val title: String,
    val why: String,
    val render: (RealTimeFilterEngine, Bitmap, Float) -> Bitmap
)

@Composable
private fun FilterThumb(title: String, bitmap: Bitmap?, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        GlassmorphicCard(cornerRadius = 14.dp) {
            Box(
                modifier = Modifier
                    .size(96.dp, 72.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = if (selected) 0.28f else 0.18f))
                    .padding(2.dp)
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)))
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(16.dp)
                        .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                ) {}
            }
        }
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
        TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("Apply") }
    }
}

// Cache labels and pick suggestions for AI sheet
@Composable
private fun aiPicks(labels: List<String>): List<SuggestionSpec> {
    // This method is a placeholder hook; the actual choices are mapped in the main Composable where suggestions is defined
    return emptyList()
}

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

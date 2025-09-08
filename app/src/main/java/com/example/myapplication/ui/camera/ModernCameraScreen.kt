package com.example.myapplication.ui.camera

import android.Manifest
import android.graphics.Bitmap
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.view.LifecycleCameraController
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.components.*
import com.example.myapplication.ui.theme.DesignTokens
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.rotate
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.windowsizeclass.*
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import android.view.GestureDetector
import android.view.ScaleGestureDetector
import android.view.MotionEvent
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.Camera
import java.util.concurrent.TimeUnit
import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.ContentScale

/**
 * Modern Camera Screen following the master prompt specifications
 * Capture-first design with minimal chrome and glassmorphic UI
 */

// Enums for camera settings
enum class QuickPanel { TIMER, RATIO, GRID }
enum class FlashMode { AUTO, ON, OFF }
enum class AspectRatioMode { FULL, RATIO_4_3, RATIO_16_9, RATIO_1_1 }

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ModernCameraScreen(
    onImageCaptured: (Bitmap) -> Unit,
    onNavigateToGallery: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val needsExtWrite = android.os.Build.VERSION.SDK_INT <= 28
    val storagePermissionState = if (needsExtWrite) rememberPermissionState(Manifest.permission.WRITE_EXTERNAL_STORAGE) else null
    
    var previewView: PreviewView? by remember { mutableStateOf(null) }
    val captureManager = remember { CameraCaptureManager(context, lifecycleOwner) }
    var isCapturing by remember { mutableStateOf(false) }
    var flashMode by remember { mutableStateOf(FlashMode.AUTO) }
    var isAIQuickSheetVisible by remember { mutableStateOf(false) }
    var zoomLevel by remember { mutableStateOf(1f) }
    var showZoomIndicator by remember { mutableStateOf(false) }
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var selectedSuggestion by remember { mutableStateOf<String?>(null) }
    var timerSeconds by remember { mutableStateOf(0) }
    var countdownRemaining by remember { mutableStateOf(0) }

    var countdownTotal by remember { mutableStateOf(0) }
    var showGrid by remember { mutableStateOf(false) }
    var aspectRatio by remember { mutableStateOf(AspectRatioMode.FULL) }
    var showCaptureFlash by remember { mutableStateOf(false) }
    var lastThumb by remember { mutableStateOf<Bitmap?>(null) }
    var gridOpacity by remember { mutableStateOf(0.15f) }
    // Long-press quick tool panel
    var activeQuickPanel by remember { mutableStateOf<QuickPanel?>(null) }
    // Post-capture micro preview overlay state
    var pendingPreview by remember { mutableStateOf<Bitmap?>(null) }
    var showPreviewOverlay by remember { mutableStateOf(false) }
    
    // Enhanced UI state with animations
    var showControls by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
    // Smooth zoom animation
    val animatedZoom by animateFloatAsState(
        targetValue = zoomLevel,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    LaunchedEffect(animatedZoom) {
        showZoomIndicator = true
        delay(1200)
        showZoomIndicator = false
    }
    
    // Controls visibility animation
    val controlsAlpha by animateFloatAsState(
        targetValue = if (showControls) 1f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing)
    )
    
    // Shutter button scale animation
    val shutterScale by animateFloatAsState(
        targetValue = if (isCapturing) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        )
    )
    
    // AI Quick suggestions
    val aiQuickSuggestions = remember {
        listOf(
            "Auto Enhance", "Portrait Glow", "Vivid Greens", 
            "Low-light Clean", "HDR Pop", "Warm Film", "B&W Classic"
        )
    }
    
    
    // Keep flash mode in sync with controller
    LaunchedEffect(flashMode) {
        val mode = when (flashMode) {
            FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
            FlashMode.ON -> ImageCapture.FLASH_MODE_ON
            FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
        }
        captureManager.setFlashMode(mode)
    }
    
    // Update controller selector when switching lenses
    LaunchedEffect(cameraSelector) {
        captureManager.setLens(cameraSelector)
    }
    
    // Auto-hide controls after inactivity
    LaunchedEffect(lastInteractionTime) {
        delay(3000) // Hide after 3 seconds of inactivity
        if (System.currentTimeMillis() - lastInteractionTime >= 3000) {
            showControls = false
        }
    }
    
    // Show controls on any interaction
    fun onUserInteraction() {
        lastInteractionTime = System.currentTimeMillis()
        showControls = true
    }

    // Proactively request permissions on first entry
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
        if (needsExtWrite && storagePermissionState?.status?.isGranted == false) {
            storagePermissionState.launchPermissionRequest()
        }
    }
    
    if (!cameraPermissionState.status.isGranted || (needsExtWrite && storagePermissionState?.status?.isGranted == false)) {
        CameraPermissionScreen(
            onRequestPermission = {
                if (!cameraPermissionState.status.isGranted) cameraPermissionState.launchPermissionRequest()
                if (needsExtWrite && storagePermissionState?.status?.isGranted == false) storagePermissionState.launchPermissionRequest()
            }
        )
        return
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Animated gradient background
        AnimatedGradientBackground(modifier = Modifier.fillMaxSize())
        
        // Camera preview with tap-to-show-controls
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).also { view ->
                    // Improve device compatibility (TextureView)
                    view.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    previewView = view
                    captureManager.bind(view)
                    try { android.widget.Toast.makeText(ctx, "Camera ready", android.widget.Toast.LENGTH_SHORT).show() } catch (_: Throwable) {}
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Preview overlay tint to reflect selected suggestion
        AnimatedVisibility(visible = selectedSuggestion != null, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize().background(getSuggestionOverlayColor(selectedSuggestion)))
        }
        
        // Grid overlay
        if (showGrid) {
            GridOverlay(alpha = gridOpacity)
        }
        
        // Aspect ratio mask
        if (aspectRatio != AspectRatioMode.FULL) {
            AspectRatioMask(aspectRatio)
        }
        
        // Capture flash overlay
        AnimatedVisibility(visible = showCaptureFlash, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.35f)))
        }
        
        // Zoom indicator
        AnimatedVisibility(visible = showZoomIndicator, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                GlassmorphicCard(
                    modifier = Modifier.padding(bottom = 140.dp),
                    cornerRadius = 20.dp
                ) {
                    Text(
                        text = String.format("%.1fx", animatedZoom),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
        
        // Countdown overlay with arc and haptics near end
        AnimatedVisibility(visible = countdownRemaining > 0, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val total = (if (countdownTotal > 0) countdownTotal else timerSeconds).takeIf { it > 0 } ?: 1
                val progress = 1f - (countdownRemaining.toFloat() / total.toFloat())
                Canvas(Modifier.size(180.dp)) {
                    val stroke = 8.dp.toPx()
                    drawCircle(color = Color.White.copy(alpha = 0.15f), style = Stroke(width = stroke))
                    drawArc(
                        color = DesignTokens.Colors.Light.accent,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = stroke)
                    )
                }
                Text(text = countdownRemaining.toString(), color = Color.White, style = MaterialTheme.typography.displayLarge)
            }
        }
        
        // Top glass bar - minimal chrome with fade animation
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ),
            exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ),
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding()
        ) {
            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(DesignTokens.Spacing.md),
                cornerRadius = DesignTokens.Radius.pill
            ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DesignTokens.Sizes.topBarHeight)
                    .padding(horizontal = DesignTokens.Spacing.lg),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Flash toggle with better visual feedback
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(DesignTokens.Radius.pill))
                        .background(
                            if (flashMode != FlashMode.OFF) Color.White.copy(alpha = 0.2f) else Color.Transparent
                        )
                        .clickable {
                            flashMode = when (flashMode) {
                                FlashMode.AUTO -> FlashMode.ON
                                FlashMode.ON -> FlashMode.OFF
                                FlashMode.OFF -> FlashMode.AUTO
                            }
                            onUserInteraction()
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        .padding(horizontal = DesignTokens.Spacing.md, vertical = DesignTokens.Spacing.sm)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = when (flashMode) {
                                FlashMode.AUTO -> Icons.Outlined.FlashAuto
                                FlashMode.ON -> Icons.Outlined.FlashOn
                                FlashMode.OFF -> Icons.Outlined.FlashOff
                            },
                            contentDescription = "Flash",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        if (flashMode != FlashMode.OFF) {
                            Text(
                                text = when (flashMode) {
                                    FlashMode.AUTO -> "AUTO"
                                    FlashMode.ON -> "ON"
                                    else -> ""
                                },
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                // Title (Photo only)
                Text(
                    text = "Photo",
                    color = DesignTokens.Colors.Light.textHigh,
                    style = MaterialTheme.typography.titleMedium
                )
                
                // Lens switcher with rotation animation
                Box {
                    val rotation = remember { Animatable(0f) }
                    
                    LaunchedEffect(cameraSelector) {
                        rotation.animateTo(
                            targetValue = rotation.value + 180f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }
                    
                    GlassmorphicButton(
                        onClick = {
                            // Switch between front/back camera
                            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            } else {
                                CameraSelector.DEFAULT_BACK_CAMERA
                            }
                            onUserInteraction()
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        cornerRadius = DesignTokens.Radius.pill
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Cameraswitch,
                            contentDescription = "Switch camera",
                            tint = Color.White,
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(rotation.value)
                        )
                    }
                }
            }
        }
        }
        
        // Right edge vertical quick tools with slide animation
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(animationSpec = tween(300)) + slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ),
            exit = fadeOut(animationSpec = tween(300)) + slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Column(
                modifier = Modifier
                    .width(DesignTokens.Sizes.edgeToolsWidth)
                    .padding(end = DesignTokens.Spacing.md),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.md)
            ) {
                QuickToolButton(
                    icon = if (timerSeconds > 0) "$timerSeconds" else "⏱",
                    isActive = timerSeconds > 0,
                    onClick = { 
                        // Cycle timer: 0 -> 3 -> 5 -> 10 -> 0
                        timerSeconds = when (timerSeconds) { 0 -> 3; 3 -> 5; 5 -> 10; else -> 0 }
                        onUserInteraction()
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    onLongPress = {
                        activeQuickPanel = QuickPanel.TIMER
                        onUserInteraction()
                    }
                )
                QuickToolButton(
                    icon = when (aspectRatio) {
                        AspectRatioMode.FULL -> "⚏"
                        AspectRatioMode.RATIO_4_3 -> "4:3"
                        AspectRatioMode.RATIO_16_9 -> "16:9"
                        AspectRatioMode.RATIO_1_1 -> "1:1"
                    },
                    isActive = aspectRatio != AspectRatioMode.FULL,
                    onClick = { 
                        // Cycle aspect ratio mode
                        aspectRatio = when (aspectRatio) {
                            AspectRatioMode.FULL -> AspectRatioMode.RATIO_4_3
                            AspectRatioMode.RATIO_4_3 -> AspectRatioMode.RATIO_16_9
                            AspectRatioMode.RATIO_16_9 -> AspectRatioMode.RATIO_1_1
                            AspectRatioMode.RATIO_1_1 -> AspectRatioMode.FULL
                        }
                        onUserInteraction()
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    onLongPress = { activeQuickPanel = QuickPanel.RATIO; onUserInteraction() }
                )
                QuickToolButton(
                    icon = "⊞",
                    isActive = showGrid,
                    onClick = { 
                        // Toggle grid overlay
                        showGrid = !showGrid
                        onUserInteraction()
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    onLongPress = { activeQuickPanel = QuickPanel.GRID; onUserInteraction() }
                )
            }
        }
        
        // Bottom area - shutter and controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = DesignTokens.Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pre-capture suggestion chips with enhanced styling
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm),
                contentPadding = PaddingValues(horizontal = DesignTokens.Spacing.md),
                modifier = Modifier.padding(bottom = DesignTokens.Spacing.lg)
            ) {
                items(aiQuickSuggestions.take(5)) { suggestion ->
                    val isSelected = selectedSuggestion == suggestion
                    
                    val backgroundColor = animateColorAsState(
                        targetValue = if (isSelected) 
                            Color.White.copy(alpha = 0.25f) 
                        else 
                            Color.White.copy(alpha = 0.15f),
                        animationSpec = tween(200)
                    )
                    
                    val borderColor = animateColorAsState(
                        targetValue = if (isSelected) 
                            Color.White.copy(alpha = 0.6f) 
                        else 
                            Color.White.copy(alpha = 0.3f),
                        animationSpec = tween(200)
                    )
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(backgroundColor.value)
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = borderColor.value,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                selectedSuggestion = if (selectedSuggestion == suggestion) null else suggestion
                                onUserInteraction()
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = suggestion,
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            )
                        )
                    }
                }
            }
            
            // Main controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery thumb
                GalleryThumb(
                    bitmap = lastThumb,
                    onClick = onNavigateToGallery,
                    modifier = Modifier.size(DesignTokens.Sizes.galleryThumb)
                )
                
                // Shutter button with enhanced animation
                Box(
                    modifier = Modifier.scale(shutterScale)
                ) {
                    ShutterButton(
                        onClick = {
                            if (isCapturing) {
                                try { android.widget.Toast.makeText(context, "Camera not ready", android.widget.Toast.LENGTH_SHORT).show() } catch (_: Throwable) {}
                                return@ShutterButton
                            }
                            scope.launch {
                                isCapturing = true
                                onUserInteraction()
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                
                                try {
                                    // Optional countdown
                                    if (timerSeconds > 0) {
                                        countdownTotal = timerSeconds
                                        for (i in timerSeconds downTo 1) {
                                            countdownRemaining = i
                                            if (i == 1) {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                            delay(1000)
                                        }
                                        countdownRemaining = 0
                                        countdownTotal = 0
                                    }
                                    
                                    // Flash overlay
                                    showCaptureFlash = true
                                    delay(100)
                                    showCaptureFlash = false
                                    
                                    // Capture image via modular manager (suspend)
                                    val bitmap = captureManager.capture()
                                    // Immediately save to Gallery so it appears in Gallery view
                                    saveBitmapToMediaStore(context, bitmap)
                                    lastThumb = bitmap
                                    pendingPreview = bitmap
                                    showPreviewOverlay = true
                                } catch (e: Exception) {
                                    // Handle capture error
                                    e.printStackTrace()
                                } finally {
                                    delay(200)
                                    isCapturing = false
                                }
                            }
                        },
                        isCapturing = isCapturing
                    )
                }
                
                // AI Quick button
                GlassmorphicButton(
                    onClick = {
                        isAIQuickSheetVisible = true
                        onUserInteraction()
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    modifier = Modifier.size(DesignTokens.Sizes.galleryThumb),
                    cornerRadius = DesignTokens.Radius.chip
                ) {
Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = "AI Quick",
                        tint = DesignTokens.Colors.Light.accent,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
        
        // AI Quick Prompts Sheet with swipe-to-dismiss and scrim
        if (isAIQuickSheetVisible) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
            ModalBottomSheet(
                onDismissRequest = { isAIQuickSheetVisible = false },
                sheetState = sheetState,
                scrimColor = Color.Black.copy(alpha = 0.45f)
            ) {
                AIQuickPromptsSheet(
                    suggestions = aiQuickSuggestions,
                    onDismiss = { isAIQuickSheetVisible = false },
                    onSuggestionSelected = { suggestion ->
                        selectedSuggestion = suggestion
                        isAIQuickSheetVisible = false
                    }
                )
            }
        }
        
        // Quick tool panel overlay
        activeQuickPanel?.let { panel ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .pointerInput(panel) {
                        detectTapGestures(onTap = { activeQuickPanel = null })
                    }
            ) {
                GlassmorphicCard(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 72.dp),
                    cornerRadius = 16.dp
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        when (panel) {
                            QuickPanel.TIMER -> {
                                Text("Timer", color = Color.White, style = MaterialTheme.typography.labelLarge)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(0, 3, 5, 10).forEach { v ->
                                        AssistChip(onClick = { timerSeconds = v; activeQuickPanel = null }, label = { Text(if (v == 0) "Off" else "${v}s") })
                                    }
                                }
                            }
                            QuickPanel.RATIO -> {
                                Text("Aspect", color = Color.White, style = MaterialTheme.typography.labelLarge)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(
                                        AspectRatioMode.FULL to "Full",
                                        AspectRatioMode.RATIO_4_3 to "4:3",
                                        AspectRatioMode.RATIO_16_9 to "16:9",
                                        AspectRatioMode.RATIO_1_1 to "1:1"
                                    ).forEach { (mode, label) ->
                                        AssistChip(onClick = { aspectRatio = mode; activeQuickPanel = null }, label = { Text(label) })
                                    }
                                }
                            }
                            QuickPanel.GRID -> {
                                Text("Grid Opacity", color = Color.White, style = MaterialTheme.typography.labelLarge)
                                Slider(value = gridOpacity, onValueChange = { gridOpacity = it }, valueRange = 0f..0.5f)
                            }
                        }
                    }
                }
            }
        }

        // Post-capture micro preview overlay (Accept / Retake)
        if (showPreviewOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
            ) {
                pendingPreview?.let { bmp ->
                    androidx.compose.foundation.Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Captured preview",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    GlassmorphicButton(
                        onClick = {
                            // Retake: dismiss overlay without saving
                            pendingPreview = null
                            // No temp file in in-memory capture path
                            showPreviewOverlay = false
                            onUserInteraction()
                        },
                        cornerRadius = DesignTokens.Radius.pill
                    ) {
                        Text("Retake", color = Color.White, modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp))
                    }
                    GlassmorphicButton(
                        onClick = {
                            val bmp = pendingPreview ?: return@GlassmorphicButton
                            // Already saved on capture; just proceed to review
                            // Update last captured thumbnail
                            val target = 96
                            val ratio = minOf(target / bmp.width.toFloat(), target / bmp.height.toFloat())
                            val w = (bmp.width * ratio).toInt().coerceAtLeast(48)
                            val h = (bmp.height * ratio).toInt().coerceAtLeast(48)
                            lastThumb = Bitmap.createScaledBitmap(bmp, w, h, true)
                            scope.launch { onImageCaptured(bmp) }
                            pendingPreview = null
                            showPreviewOverlay = false
                        },
                        cornerRadius = DesignTokens.Radius.pill
                    ) {
                        Text("Use Photo", color = Color.White, modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickToolButton(
    icon: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    onLongPress: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale = animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        )
    )
    
    val backgroundColor = animateColorAsState(
        targetValue = if (isActive) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.15f),
        animationSpec = tween(200)
    )
    
    Box(
        modifier = modifier
            .size(DesignTokens.Sizes.touchTarget)
            .scale(scale.value)
            .clip(CircleShape)
            .background(backgroundColor.value)
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = icon,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            ),
            fontSize = 18.sp
        )
    }
}

@Composable
private fun GalleryThumb(
    bitmap: Bitmap?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale = animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        )
    )
    
    Box(
        modifier = modifier
            .scale(scale.value)
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.3f),
                        Color.White.copy(alpha = 0.1f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Last shot",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.PhotoLibrary,
                contentDescription = "Gallery",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun AIQuickPromptsSheet(
    suggestions: List<String>,
    onDismiss: () -> Unit,
    onSuggestionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Minimal, clean sheet content: chips + single input
    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Drag handle
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.3f))
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "AI Suggestions",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(suggestions) { s ->
                AssistChip(onClick = { onSuggestionSelected(s) }, label = { Text(s) })
            }
        }

        Spacer(Modifier.height(16.dp))

        var prompt by remember { mutableStateOf("") }
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            placeholder = { Text("Describe your edit (e.g., warm film, boost greens)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { if (prompt.isNotBlank()) onSuggestionSelected(prompt) }) { Text("Apply") }
            OutlinedButton(onClick = onDismiss) { Text("Close") }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun CameraPermissionScreen(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(DesignTokens.Spacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Camera permission is required",
            color = DesignTokens.Colors.Light.textHigh,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = DesignTokens.Spacing.lg)
        )
        
        GlassmorphicButton(
            onClick = onRequestPermission,
            cornerRadius = DesignTokens.Radius.pill
        ) {
            Text(
                text = "Grant Camera Access",
                color = DesignTokens.Colors.Light.textHigh,
                modifier = Modifier.padding(
                    horizontal = DesignTokens.Spacing.xxl,
                    vertical = DesignTokens.Spacing.md
                )
            )
        }
    }
}

// Helper functions
private fun setupCamera(
    context: android.content.Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    cameraSelector: CameraSelector,
    onSetup: (PreviewView, ImageCapture) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build()
        val imageCapture = ImageCapture.Builder().build()
        
        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        } catch (exc: Exception) {
            // Handle camera binding error
        }
    }, ContextCompat.getMainExecutor(context))
}

private fun setupCameraPreview(
    context: android.content.Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView,
    cameraSelector: CameraSelector,
    onReady: (ImageCapture, Camera) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .build()
        val imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(previewView.display.rotation)
            .build()
        
        preview.setSurfaceProvider(previewView.surfaceProvider)
        
        try {
            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
            onReady(imageCapture, camera)
            try { android.widget.Toast.makeText(context, "Camera ready", android.widget.Toast.LENGTH_SHORT).show() } catch (_: Throwable) {}
        } catch (exc: Exception) {
            try { android.widget.Toast.makeText(context, "Camera init failed: ${exc.message}", android.widget.Toast.LENGTH_SHORT).show() } catch (_: Throwable) {}
        }
    }, ContextCompat.getMainExecutor(context))
}

private fun captureImage(
    imageCapture: ImageCapture,
    context: android.content.Context,
    onImageCaptured: (Bitmap) -> Unit
) {
    // Primary path: in-memory capture then convert YUV -> Bitmap
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                try {
                    val bitmap = com.example.myapplication.utils.ImageConvert.imageProxyToBitmap(image)
                    try { android.widget.Toast.makeText(context, "Photo captured", android.widget.Toast.LENGTH_SHORT).show() } catch (_: Throwable) {}
                    onImageCaptured(optimizeBitmapForDisplay(bitmap))
                } catch (e: Exception) {
                    e.printStackTrace()
                    try { android.widget.Toast.makeText(context, "Capture convert failed; retrying", android.widget.Toast.LENGTH_SHORT).show() } catch (_: Throwable) {}
                    // Fallback: write to temp file and decode
                    captureToFileFallback(imageCapture, context, onImageCaptured)
                } finally {
                    try { image.close() } catch (_: Throwable) {}
                }
            }

            override fun onError(exception: ImageCaptureException) {
                // Fallback to file-based capture
                captureToFileFallback(imageCapture, context, onImageCaptured)
            }
        }
    )
}

private fun captureWithController(
    controller: LifecycleCameraController,
    context: android.content.Context,
    onImageCaptured: (Bitmap) -> Unit
) {
    controller.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                try {
                    val bitmap = com.example.myapplication.utils.ImageConvert.imageProxyToBitmap(image)
                    try { android.widget.Toast.makeText(context, "Photo captured", android.widget.Toast.LENGTH_SHORT).show() } catch (_: Throwable) {}
                    onImageCaptured(optimizeBitmapForDisplay(bitmap))
                } catch (e: Exception) {
                    e.printStackTrace()
                    try { android.widget.Toast.makeText(context, "Capture convert failed; retrying", android.widget.Toast.LENGTH_SHORT).show() } catch (_: Throwable) {}
                    captureControllerToFileFallback(controller, context, onImageCaptured)
                } finally {
                    try { image.close() } catch (_: Throwable) {}
                }
            }

            override fun onError(exception: ImageCaptureException) {
                captureControllerToFileFallback(controller, context, onImageCaptured)
            }
        }
    )
}

private fun captureControllerToFileFallback(
    controller: LifecycleCameraController,
    context: android.content.Context,
    onImageCaptured: (Bitmap) -> Unit
) {
    val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES) ?: context.cacheDir
    val file = java.io.File(dir, "captured_${System.currentTimeMillis()}.jpg")
    val options = ImageCapture.OutputFileOptions.Builder(file).build()
    controller.takePicture(
        options,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                try {
                    val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) {
                        try { android.widget.Toast.makeText(context, "Photo captured (fallback)", android.widget.Toast.LENGTH_SHORT).show() } catch (_: Throwable) {}
                        onImageCaptured(optimizeBitmapForDisplay(bitmap))
                    } else {
                        val placeholder = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
                        placeholder.eraseColor(android.graphics.Color.GRAY)
                        onImageCaptured(placeholder)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    val placeholder = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
                    placeholder.eraseColor(android.graphics.Color.LTGRAY)
                    onImageCaptured(placeholder)
                } finally {
                    try { file.delete() } catch (_: Throwable) {}
                }
            }

            override fun onError(exception: ImageCaptureException) {
                exception.printStackTrace()
                try { android.widget.Toast.makeText(context, "Capture failed: ${exception.message}", android.widget.Toast.LENGTH_SHORT).show() } catch (_: Throwable) {}
            }
        }
    )
}
private fun captureToFileFallback(
    imageCapture: ImageCapture,
    context: android.content.Context,
    onImageCaptured: (Bitmap) -> Unit
) {
    val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES) ?: context.cacheDir
    val file = java.io.File(dir, "captured_${System.currentTimeMillis()}.jpg")
    val options = ImageCapture.OutputFileOptions.Builder(file).build()
    imageCapture.takePicture(
        options,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                try {
                    val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) {
                        try { android.widget.Toast.makeText(context, "Photo captured (fallback)", android.widget.Toast.LENGTH_SHORT).show() } catch (_: Throwable) {}
                        onImageCaptured(optimizeBitmapForDisplay(bitmap))
                    } else {
                        // Placeholder when decode fails
                        val placeholder = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
                        placeholder.eraseColor(android.graphics.Color.GRAY)
                        onImageCaptured(placeholder)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    val placeholder = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
                    placeholder.eraseColor(android.graphics.Color.LTGRAY)
                    onImageCaptured(placeholder)
                } finally {
                    try { file.delete() } catch (_: Throwable) {}
                }
            }

            override fun onError(exception: ImageCaptureException) {
                exception.printStackTrace()
                try { android.widget.Toast.makeText(context, "Capture failed: ${exception.message}", android.widget.Toast.LENGTH_SHORT).show() } catch (_: Throwable) {}
            }
        }
    )
}

private fun saveBitmapToMediaStore(context: android.content.Context, bitmap: Bitmap): android.net.Uri? {
    return try {
        val name = "AICam_" + System.currentTimeMillis()
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AI Camera")
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
        }
        uri
    } catch (t: Throwable) {
        null
    }
}

@Composable
private fun GridOverlay(modifier: Modifier = Modifier, alpha: Float = 0.15f) {
    Canvas(Modifier.fillMaxSize().then(modifier)) {
        val cols = 3
        val rows = 3
        val lineColor = Color.White.copy(alpha = alpha)
        val stroke = 1.dp.toPx()
        val colWidth = size.width / cols
        val rowHeight = size.height / rows
        for (i in 1 until cols) {
            drawLine(
                color = lineColor, 
                start = Offset(colWidth * i, 0f), 
                end = Offset(colWidth * i, size.height), 
                strokeWidth = stroke
            )
        }
        for (j in 1 until rows) {
            drawLine(
                color = lineColor, 
                start = Offset(0f, rowHeight * j), 
                end = Offset(size.width, rowHeight * j), 
                strokeWidth = stroke
            )
        }
    }
}

private fun getSuggestionOverlayColor(name: String?): Color {
    return when (name) {
        "Auto Enhance" -> Color(0x66FFFFFF) // soft glow
        "Portrait Glow" -> Color(0x33FFD39E)
        "Vivid Greens" -> Color(0x3322FF88)
        "Low-light Clean" -> Color(0x332288FF)
        "HDR Pop" -> Color(0x33FFFFFF)
        "Warm Film" -> Color(0x33FFB088)
        "B&W Classic" -> Color(0x33000000)
        else -> Color.Transparent
    }
}

private fun optimizeBitmapForDisplay(bitmap: Bitmap): Bitmap {
    val maxWidth = 1080
    val maxHeight = 1920
    
    return if (bitmap.width > maxWidth || bitmap.height > maxHeight) {
        val ratio = minOf(
            maxWidth.toFloat() / bitmap.width,
            maxHeight.toFloat() / bitmap.height
        )
        val newWidth = (bitmap.width * ratio).toInt()
        val newHeight = (bitmap.height * ratio).toInt()
        
        Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true).also {
            // Recycle original if it's different
            if (it != bitmap) {
                bitmap.recycle()
            }
        }
    } else {
        bitmap
    }
}

@Composable
private fun AspectRatioMask(mode: AspectRatioMode) {
    Canvas(Modifier.fillMaxSize()) {
        val screenW = size.width
        val screenH = size.height
        val targetRatio = when (mode) {
            AspectRatioMode.RATIO_1_1 -> 1f
            AspectRatioMode.RATIO_4_3 -> 4f / 3f
            AspectRatioMode.RATIO_16_9 -> 16f / 9f
            else -> return@Canvas
        }
        val screenRatio = screenW / screenH
        var rectW = screenW
        var rectH = screenH
        if (screenRatio > targetRatio) {
            // screen wider than target: fit height
            rectH = screenH
            rectW = rectH * targetRatio
        } else {
            // screen taller than target: fit width
            rectW = screenW
            rectH = rectW / targetRatio
        }
        val left = (screenW - rectW) / 2f
        val top = (screenH - rectH) / 2f
        // Draw dark scrim outside target rect
        val scrim = Color.Black.copy(alpha = 0.4f)
        // Top
        drawRect(color = scrim, topLeft = Offset(0f, 0f), size = Size(screenW, top))
        // Bottom
        drawRect(color = scrim, topLeft = Offset(0f, top + rectH), size = Size(screenW, screenH - (top + rectH)))
        // Left
        drawRect(color = scrim, topLeft = Offset(0f, top), size = Size(left, rectH))
        // Right
        drawRect(color = scrim, topLeft = Offset(left + rectW, top), size = Size(screenW - (left + rectW), rectH))
        // Border
        drawRect(
            color = Color.White.copy(alpha = 0.2f),
            topLeft = Offset(left, top),
            size = Size(rectW, rectH),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}



// Missing function implementations

private fun captureImageInternal(
    imageCapture: ImageCapture,
    context: android.content.Context,
    onResult: (Bitmap?) -> Unit
) {
    val file = java.io.File(
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.cacheDir,
        "captured_${System.currentTimeMillis()}.jpg"
    )
    val options = ImageCapture.OutputFileOptions.Builder(file).build()
    
    imageCapture.takePicture(
        options,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exception: ImageCaptureException) {
                onResult(null)
            }
            
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    // Save to MediaStore for gallery
                    try {
                        val name = "AICam_" + System.currentTimeMillis()
                        val values = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AI Camera")
                        }
                        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                        if (uri != null) {
                            context.contentResolver.openOutputStream(uri)?.use { out ->
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                            }
                        }
                    } catch (_: Throwable) {}
                }
                onResult(bitmap)
            }
        }
    )
}

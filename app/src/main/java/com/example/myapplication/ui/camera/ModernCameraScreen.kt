package com.example.myapplication.ui.camera

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.camera.core.*
import androidx.camera.core.Camera
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.controller.CameraController
import com.example.myapplication.ui.components.*
import com.example.myapplication.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

data class FilterEffect(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val preview: Color,
    val type: FilterType
)

enum class FilterType {
    BEAUTY, COLOR, AR_MASK, VINTAGE, ARTISTIC, FACE_MORPH
}

enum class CameraMode {
    PHOTO, VIDEO, STORY, AR_FILTER, BEAUTY, PORTRAIT
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ModernCameraScreen(
    onImageCaptured: (Bitmap) -> Unit,
    onNavigateToGallery: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    
    // Camera states
    var previewView: PreviewView? by remember { mutableStateOf(null) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var flashMode by remember { mutableStateOf(ImageCapture.FLASH_MODE_OFF) }
    var isRecording by remember { mutableStateOf(false) }
    var zoomLevel by remember { mutableStateOf(1f) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    
    // UI states
    var selectedFilter by remember { mutableStateOf<FilterEffect?>(null) }
    var currentMode by remember { mutableStateOf(CameraMode.PHOTO) }
    var showBeautyPanel by remember { mutableStateOf(false) }
    var showFilterStore by remember { mutableStateOf(false) }
    var isMenuExpanded by remember { mutableStateOf(false) }
    var timerSeconds by remember { mutableStateOf(0) }
    var showTimerMenu by remember { mutableStateOf(false) }
    var countdownRemaining by remember { mutableStateOf(0) }
    // Category filter state
    var activeFilterType by remember { mutableStateOf<FilterType?>(null) }
    
    // Capture flash overlay state
    var showFlash by remember { mutableStateOf(false) }
    
    // Beauty settings
    var smoothness by remember { mutableStateOf(0.5f) }
    var brightness by remember { mutableStateOf(0.5f) }
    var slimFace by remember { mutableStateOf(0.3f) }
    var enlargeEyes by remember { mutableStateOf(0.3f) }
    
    // Filters list
    val filters = remember {
        listOf(
            FilterEffect("none", "Original", Icons.Default.Clear, Color.Transparent, FilterType.COLOR),
            FilterEffect("beauty", "Beauty", Icons.Default.Face, SkinGlow, FilterType.BEAUTY),
            FilterEffect("vintage", "Vintage", Icons.Default.PhotoLibrary, VintageSepia, FilterType.VINTAGE),
            FilterEffect("cold", "Cold", Icons.Default.AcUnit, ColdBlue, FilterType.COLOR),
            FilterEffect("warm", "Warm", Icons.Default.WbSunny, WarmOrange, FilterType.COLOR),
            FilterEffect("dreamy", "Dreamy", Icons.Default.Lens, DreamyPurple, FilterType.ARTISTIC),
            FilterEffect("neon", "Neon", Icons.Default.Lightbulb, NeonPink, FilterType.ARTISTIC),
            FilterEffect("bunny", "Bunny", Icons.Default.Pets, HotPink, FilterType.AR_MASK),
            FilterEffect("cat", "Cat Ears", Icons.Default.Pets, NeonPurple, FilterType.AR_MASK),
            FilterEffect("hearts", "Hearts", Icons.Default.Favorite, AccentRed, FilterType.AR_MASK)
        )
    }
    
    // Camera setup (re-bind on permission or lens switch)
    LaunchedEffect(cameraPermissionState.status.isGranted, cameraSelector) {
        if (cameraPermissionState.status.isGranted) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            val cameraProvider = cameraProviderFuture.get()
            
            val preview = Preview.Builder().build()
            val imageCaptureUseCase = ImageCapture.Builder()
                .setFlashMode(flashMode)
                .build()
            imageCapture = imageCaptureUseCase
            
            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCaptureUseCase
                )
                boundCamera = camera
                // restore zoom on rebind
                try { boundCamera?.cameraControl?.setZoomRatio(zoomLevel) } catch (_: Exception) {}
                
                previewView?.let { preview.setSurfaceProvider(it.surfaceProvider) }
            } catch (exc: Exception) {
                // Handle camera binding error
            }
        }
    }

    // Keep ImageCapture flash mode in sync when toggled
    LaunchedEffect(flashMode) {
        imageCapture?.flashMode = flashMode
    }
    
    if (cameraPermissionState.status.isGranted) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Camera Preview with gradient overlay
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).also { previewView = it }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            // Pinch to zoom
                            detectTransformGestures { _, _, zoom, _ ->
                                zoomLevel = (zoomLevel * zoom).coerceIn(1f, 5f)
                                try { boundCamera?.cameraControl?.setZoomRatio(zoomLevel) } catch (_: Exception) {}
                            }
                        }
                        .pointerInput(Unit) {
                            // Double tap to switch camera
                            detectTapGestures(
                                onDoubleTap = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                                        CameraSelector.DEFAULT_FRONT_CAMERA
                                    } else {
                                        CameraSelector.DEFAULT_BACK_CAMERA
                                    }
                                }
                            )
                        }
                )
                
                // Gradient overlay for UI elements
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.3f),
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.5f)
                                ),
                                startY = 0f,
                                endY = Float.POSITIVE_INFINITY
                            )
                        )
                )
                
                // Flash overlay
                AnimatedVisibility(
                    visible = showFlash,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.35f))) {}
                }
                
                // Countdown overlay
                AnimatedVisibility(
                    visible = countdownRemaining > 0,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text(
                        text = countdownRemaining.toString(),
                        color = NeonPink,
                        fontSize = 72.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            
            // Top Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left controls
                GlassmorphicCard(
                    modifier = Modifier,
                    cornerRadius = 20.dp
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { 
                            flashMode = when(flashMode) {
                                ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                                ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                                else -> ImageCapture.FLASH_MODE_OFF
                            }
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }) {
                            Icon(
                                imageVector = when(flashMode) {
                                    ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                                    ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                                    else -> Icons.Default.FlashOff
                                },
                                contentDescription = "Flash",
                                tint = Color.White
                            )
                        }
                        
                        Box {
                            IconButton(onClick = {
                                showTimerMenu = true
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "Timer",
                                    tint = if (timerSeconds > 0) NeonPink else Color.White
                                )
                            }
                            DropdownMenu(
                                expanded = showTimerMenu,
                                onDismissRequest = { showTimerMenu = false }
                            ) {
                                listOf(0, 3, 5, 10).forEach { sec ->
                                    DropdownMenuItem(
                                        text = { Text(if (sec == 0) "Off" else "${sec}s") },
                                        onClick = {
                                            timerSeconds = sec
                                            showTimerMenu = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        IconButton(onClick = {
                            showBeautyPanel = !showBeautyPanel
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = "Beauty",
                                tint = if (showBeautyPanel) NeonPink else Color.White
                            )
                        }
                    }
                }
                
                // Mode indicator
                GlassmorphicCard(cornerRadius = 20.dp) {
                    Text(
                        text = currentMode.name.replace("_", " "),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = NeonPink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                
                // Right controls
                GlassmorphicCard(cornerRadius = 20.dp) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            } else {
                                CameraSelector.DEFAULT_BACK_CAMERA
                            }
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Switch Camera",
                                tint = Color.White
                            )
                        }
                        
                        IconButton(onClick = {
                            showFilterStore = true
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Store,
                                contentDescription = "Filter Store",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
            
            // Bottom UI stack: modes, filters, controls (no overlaps)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Camera modes carousel
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    items(CameraMode.values()) { mode ->
                        CameraModeChip(
                            mode = mode,
                            isSelected = currentMode == mode,
                            onClick = {
                                currentMode = mode
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        )
                    }
                }

                // Filter category chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    item {
                        CameraModeChip(
                            mode = CameraMode.PHOTO, // dummy visual style reuse
                            isSelected = activeFilterType == null,
                            onClick = { activeFilterType = null }
                        )
                    }
                    items(FilterType.values()) { t ->
                        GlassmorphicCard(
                            cornerRadius = 16.dp,
                            modifier = Modifier
                                .clickable { activeFilterType = t }
                        ) {
                            Text(
                                text = t.name.replace("_", " ").lowercase().replaceFirstChar { it.titlecase() },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = if (activeFilterType == t) NeonPink else Color.White
                            )
                        }
                    }
                }

                // Filter carousel
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    val displayFilters = activeFilterType?.let { t -> filters.filter { it.type == t } } ?: filters
                    items(displayFilters) { filter ->
                        FilterCarouselItem(
                            filterName = filter.name,
                            filterPreview = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(filter.preview)
                                ) {
                                    Icon(
                                        imageVector = filter.icon,
                                        contentDescription = filter.name,
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .size(24.dp),
                                        tint = Color.White
                                    )
                                }
                            },
                            isSelected = selectedFilter == filter,
                            onClick = {
                                selectedFilter = if (selectedFilter == filter) null else filter
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            modifier = if (activeFilterType != null) Modifier.scale(1.2f) else Modifier
                        )
                    }
                }

                // Bottom controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Gallery button
                    GlassmorphicCard(
                        modifier = Modifier.size(48.dp),
                        cornerRadius = 24.dp
                    ) {
                        IconButton(
                            onClick = {
                                onNavigateToGallery()
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = "Gallery",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Capture button with optional countdown
                    AnimatedCaptureButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (currentMode == CameraMode.VIDEO) {
                                isRecording = !isRecording
                            } else {
                                val performCapture = {
                                    val ic = imageCapture
                                    if (ic != null) {
                                        // Trigger a brief flash overlay for capture feedback
                                        scope.launch {
                                            showFlash = true
                                            delay(120)
                                            showFlash = false
                                        }
                                        val file = java.io.File(context.cacheDir, "cap_${System.currentTimeMillis()}.jpg")
                                        val output = ImageCapture.OutputFileOptions.Builder(file).build()
                                        ic.takePicture(
                                            output,
                                            ContextCompat.getMainExecutor(context),
                                            object : ImageCapture.OnImageSavedCallback {
                                                override fun onError(exc: ImageCaptureException) {
                                                    // You may show a snackbar or log error
                                                }
                                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                                    val bmp = BitmapFactory.decodeFile(file.absolutePath)
                                                    if (bmp != null) onImageCaptured(bmp)
                                                }
                                            }
                                        )
                                    }
                                }
                                if (timerSeconds > 0) {
                                    scope.launch {
                                        for (i in timerSeconds downTo 1) {
                                            countdownRemaining = i
                                            delay(1000)
                                        }
                                        countdownRemaining = 0
                                        performCapture()
                                    }
                                } else {
                                    performCapture()
                                }
                            }
                        },
                        isRecording = isRecording
                    )

                    // Effects button
                    GlassmorphicCard(
                        modifier = Modifier.size(48.dp),
                        cornerRadius = 24.dp
                    ) {
                        IconButton(
                            onClick = {
                                isMenuExpanded = !isMenuExpanded
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Effects",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
            
            // Beauty panel
            AnimatedVisibility(
                visible = showBeautyPanel,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                GlassmorphicCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                        .padding(bottom = 16.dp),
                    cornerRadius = 24.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Beauty Settings",
                            color = NeonPink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        
                        BeautySlider(
                            value = smoothness,
                            onValueChange = { smoothness = it },
                            label = "Smooth Skin"
                        )
                        
                        BeautySlider(
                            value = brightness,
                            onValueChange = { brightness = it },
                            label = "Brightness"
                        )
                        
                        BeautySlider(
                            value = slimFace,
                            onValueChange = { slimFace = it },
                            label = "Slim Face"
                        )
                        
                        BeautySlider(
                            value = enlargeEyes,
                            onValueChange = { enlargeEyes = it },
                            label = "Enlarge Eyes"
                        )
                    }
                }
            }
            
            // Floating action menu
            FloatingActionMenu(
                isExpanded = isMenuExpanded,
                onToggle = {
                    isMenuExpanded = !isMenuExpanded
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                },
                items = listOf(
                    FloatingMenuItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        onClick = {
                            onNavigateToSettings()
                            isMenuExpanded = false
                        },
                        label = "Settings",
                        backgroundColor = NeonBlue
                    ),
                    FloatingMenuItem(
                        icon = { Icon(Icons.Default.MusicNote, contentDescription = "Music") },
                        onClick = { /* Add music */ },
                        label = "Add Music",
                        backgroundColor = NeonPurple
                    ),
                    FloatingMenuItem(
                        icon = { Icon(Icons.Default.TextFields, contentDescription = "Text") },
                        onClick = { /* Add text */ },
                        label = "Add Text",
                        backgroundColor = AccentGreen
                    ),
                    FloatingMenuItem(
                        icon = { Icon(Icons.Default.Draw, contentDescription = "Draw") },
                        onClick = { /* Drawing mode */ },
                        label = "Draw",
                        backgroundColor = AccentOrange
                    )
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .padding(top = 100.dp)
            )
        }
    } else {
        // Permission request UI with modern design
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            NeonPink.copy(alpha = 0.1f),
                            NeonBlue.copy(alpha = 0.1f),
                            NeonPurple.copy(alpha = 0.1f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = NeonPink
                    )
                    
                    Text(
                        "Camera Permission Required",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        "We need camera access to capture amazing photos and apply AR filters!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                    
                    NeonButton(
                        onClick = { cameraPermissionState.launchPermissionRequest() },
                        text = "Grant Permission",
                        icon = {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraModeChip(
    mode: CameraMode,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale = animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    GlassmorphicCard(
        modifier = Modifier
            .scale(scale.value)
            .clickable { onClick() },
        cornerRadius = 16.dp
    ) {
        Text(
            text = mode.name.replace("_", " "),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = if (isSelected) NeonPink else Color.White,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

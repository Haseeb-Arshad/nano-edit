package com.example.myapplication.ui.camera

import android.Manifest
import android.graphics.Bitmap
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import com.example.myapplication.controller.CameraController
import com.example.myapplication.ui.theme.CameraDesignTokens
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Clean, minimalistic camera screen following the design specifications
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CleanCameraScreen(
    onImageCaptured: (Bitmap) -> Unit,
    onNavigateToGallery: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    
    // Camera permission
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    
    // Camera preview
    val previewView = remember { PreviewView(context) }
    
    // UI State
    var currentMode by remember { mutableStateOf(CameraMode.PHOTO) }
    var isCapturing by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var showGrid by remember { mutableStateOf(false) }
    var focusPosition by remember { mutableStateOf<Offset?>(null) }
    var exposureValue by remember { mutableStateOf<String?>(null) }
    var selectedFilterIndex by remember { mutableStateOf(0) }
    var flashMode by remember { mutableStateOf(FlashMode.OFF) }
    var lastCapturedUri by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        cameraPermission.launchPermissionRequest()
    }
    
    if (cameraPermission.status.isGranted) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(CameraDesignTokens.Colors.background)
        ) {
            // Camera preview with overlays
            CleanCameraPreview(
                previewView = previewView,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            focusPosition = Offset(offset.x, offset.y)
                            // Simulate exposure calculation
                            scope.launch {
                                delay(500)
                                exposureValue = "f/2.0"
                                delay(2000)
                                exposureValue = null
                            }
                        }
                    }
            ) {
                // Rule of thirds grid
                RuleOfThirdsGrid(
                    visible = showGrid,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Focus reticle
                FocusReticle(
                    position = focusPosition,
                    modifier = Modifier
                )
                
                // Exposure readout
                AnimatedVisibility(
                    visible = exposureValue != null,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(CameraDesignTokens.Spacing.l)
                ) {
                    exposureValue?.let {
                        ExposureReadout(value = it)
                    }
                }
            }
            
            // Top glass bar with controls
            CleanTopBar(
                showGrid = showGrid,
                flashMode = flashMode,
                onToggleGrid = { showGrid = !showGrid },
                onToggleFlash = { 
                    flashMode = when (flashMode) {
                        FlashMode.OFF -> FlashMode.ON
                        FlashMode.ON -> FlashMode.AUTO
                        FlashMode.AUTO -> FlashMode.OFF
                    }
                },
                modifier = Modifier.align(Alignment.TopCenter)
            )
            
            // Filter carousel (floating above pill)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        bottom = CameraDesignTokens.Dimensions.navBarHeight +
                                CameraDesignTokens.Dimensions.glassPillBottomMargin +
                                CameraDesignTokens.Dimensions.glassPillHeight -
                                CameraDesignTokens.Dimensions.filterCarouselOverlap
                    )
            ) {
                CleanFilterCarousel(
                    filters = sampleFilterPresets,
                    selectedIndex = selectedFilterIndex,
                    onFilterSelected = { index ->
                        selectedFilterIndex = index
                        // Apply filter effect here
                    }
                )
            }
            
            // Glass pill control bar
            GlassPillControlBar(
                galleryThumbnailUri = lastCapturedUri,
                currentMode = currentMode,
                isCapturing = isCapturing,
                isProcessing = isProcessing,
                onCaptureClick = {
                    scope.launch {
                        isCapturing = true
                        delay(200) // Shutter animation
                        isCapturing = false
                        isProcessing = true
                        
                        // Simulate capture and processing
                        delay(1500)
                        
                        // Create a dummy bitmap for demo
                        val bitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
                        onImageCaptured(bitmap)
                        lastCapturedUri = "dummy_uri" // In real app, this would be the actual URI
                        
                        isProcessing = false
                    }
                },
                onGalleryClick = onNavigateToGallery,
                onModeToggle = {
                    currentMode = if (currentMode == CameraMode.PHOTO) {
                        CameraMode.VIDEO
                    } else {
                        CameraMode.PHOTO
                    }
                },
                onSettingsClick = onNavigateToSettings,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
            
            // Error state snackbar would go here
        }
    } else {
        // Permission denied state
        CameraPermissionDenied(
            onRequestPermission = { cameraPermission.launchPermissionRequest() }
        )
    }
}

/**
 * Top bar with flash and grid controls
 */
@Composable
private fun CleanTopBar(
    showGrid: Boolean,
    flashMode: FlashMode,
    onToggleGrid: () -> Unit,
    onToggleFlash: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = CameraDesignTokens.Dimensions.topBarMargin,
                vertical = CameraDesignTokens.Dimensions.statusBarHeight + 
                         CameraDesignTokens.Dimensions.topBarTopOffset
            )
            .height(CameraDesignTokens.Dimensions.topBarHeight),
        color = CameraDesignTokens.Colors.glassSubtle,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(
            CameraDesignTokens.CornerRadius.medium
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = CameraDesignTokens.Spacing.l),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left - empty for now (could add back button in sub-flows)
            Spacer(modifier = Modifier.width(40.dp))
            
            // Center - title
            Text(
                text = "CAMERA",
                fontSize = CameraDesignTokens.Typography.topBarTitleFontSize.sp,
                color = CameraDesignTokens.Colors.textPrimary,
                style = androidx.compose.material3.MaterialTheme.typography.titleSmall
            )
            
            // Right - toggles
            Row(
                horizontalArrangement = Arrangement.spacedBy(CameraDesignTokens.Spacing.s)
            ) {
                IconButton(
                    onClick = onToggleFlash,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = when (flashMode) {
                            FlashMode.OFF -> Icons.Default.FlashOff
                            FlashMode.ON -> Icons.Default.FlashOn
                            FlashMode.AUTO -> Icons.Default.FlashAuto
                        },
                        contentDescription = "Flash: ${flashMode.name}",
                        tint = CameraDesignTokens.Colors.textPrimary
                    )
                }
                
                IconButton(
                    onClick = onToggleGrid,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GridOn,
                        contentDescription = if (showGrid) "Hide grid" else "Show grid",
                        tint = if (showGrid) {
                            CameraDesignTokens.Colors.accentStart
                        } else {
                            CameraDesignTokens.Colors.textSecondary
                        }
                    )
                }
            }
        }
    }
}

/**
 * Camera permission denied screen
 */
@Composable
private fun CameraPermissionDenied(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CameraDesignTokens.Colors.background),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(280.dp),
            color = CameraDesignTokens.Colors.glass,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(
                CameraDesignTokens.CornerRadius.large
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(CameraDesignTokens.Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Camera Permission Required",
                    style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                    color = CameraDesignTokens.Colors.textPrimary
                )
                
                Spacer(modifier = Modifier.height(CameraDesignTokens.Spacing.l))
                
                Text(
                    text = "This app needs camera permission to capture photos and videos",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = CameraDesignTokens.Colors.textSecondary
                )
                
                Spacer(modifier = Modifier.height(CameraDesignTokens.Spacing.xl))
                
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CameraDesignTokens.Colors.accentStart
                    )
                ) {
                    Text("Allow Camera")
                }
            }
        }
    }
}

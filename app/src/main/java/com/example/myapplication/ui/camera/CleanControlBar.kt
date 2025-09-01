package com.example.myapplication.ui.camera

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.myapplication.ui.theme.CameraDesignTokens
import kotlinx.coroutines.delay

/**
 * Glass pill control bar - the main control surface at the bottom
 */
@Composable
fun GlassPillControlBar(
    galleryThumbnailUri: String? = null,
    currentMode: CameraMode = CameraMode.PHOTO,
    isCapturing: Boolean = false,
    isProcessing: Boolean = false,
    onCaptureClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onModeToggle: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val pillHeight = CameraDesignTokens.Breakpoints.getGlassPillHeight(screenWidth)
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = CameraDesignTokens.Dimensions.glassPillHorizontalMargin)
            .padding(
                bottom = CameraDesignTokens.Dimensions.navBarHeight + 
                        CameraDesignTokens.Dimensions.glassPillBottomMargin
            )
    ) {
        // Glass pill background
        GlassBackground(
            modifier = Modifier
                .fillMaxWidth()
                .height(pillHeight)
                .shadow(
                    elevation = CameraDesignTokens.Shadows.glassPill.blurRadius,
                    shape = RoundedCornerShape(CameraDesignTokens.CornerRadius.pill),
                    clip = false
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = CameraDesignTokens.Dimensions.glassPillInnerPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left zone - Gallery thumbnail
                GalleryThumbnail(
                    uri = galleryThumbnailUri,
                    onClick = onGalleryClick
                )
                
                // Center zone - Capture button
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CleanCaptureButton(
                        isCapturing = isCapturing,
                        isProcessing = isProcessing,
                        onClick = onCaptureClick,
                        screenWidth = screenWidth
                    )
                }
                
                // Right zone - Mode toggle and settings
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(CameraDesignTokens.Spacing.s)
                ) {
                    ModeToggle(
                        currentMode = currentMode,
                        onClick = onModeToggle
                    )
                    
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.size(CameraDesignTokens.Dimensions.settingsIconSize)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = CameraDesignTokens.Colors.textPrimary,
                            modifier = Modifier.padding(CameraDesignTokens.Dimensions.settingsIconPadding)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Gallery thumbnail with animation
 */
@Composable
private fun GalleryThumbnail(
    uri: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var shouldAnimate by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (shouldAnimate) 1.0f else 0.9f,
        animationSpec = spring(
            stiffness = CameraDesignTokens.Motion.Spring.stiffness,
            dampingRatio = CameraDesignTokens.Motion.Spring.damping
        ),
        finishedListener = { shouldAnimate = false },
        label = "thumbnail_scale"
    )
    
    LaunchedEffect(uri) {
        if (uri != null) {
            shouldAnimate = true
        }
    }
    
    Box(
        modifier = modifier
            .size(CameraDesignTokens.Dimensions.galleryThumbnailSize)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(CameraDesignTokens.Dimensions.galleryThumbnailCornerRadius))
            .border(
                width = CameraDesignTokens.Dimensions.galleryThumbnailBorder,
                color = CameraDesignTokens.Colors.glassBorder,
                shape = RoundedCornerShape(CameraDesignTokens.Dimensions.galleryThumbnailCornerRadius)
            )
            .background(CameraDesignTokens.Colors.glass)
            .clickable { onClick() }
    ) {
        if (uri != null) {
            AsyncImage(
                model = uri,
                contentDescription = "Last captured image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

/**
 * Clean capture button with all animations and states
 */
@Composable
private fun CleanCaptureButton(
    isCapturing: Boolean,
    isProcessing: Boolean,
    onClick: () -> Unit,
    screenWidth: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val buttonSize = CameraDesignTokens.Breakpoints.getCaptureButtonSize(screenWidth)
    
    // Pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = CameraDesignTokens.Dimensions.captureButtonPulseScale,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = CameraDesignTokens.Motion.pulse,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    // Press animation
    var isPressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) CameraDesignTokens.Dimensions.captureButtonPressedScale else 1.0f,
        animationSpec = tween(
            durationMillis = CameraDesignTokens.Motion.instant,
            easing = FastOutSlowInEasing
        ),
        label = "press_scale"
    )
    
    // Burst animation for capture
    var showBurst by remember { mutableStateOf(false) }
    val burstScale by animateFloatAsState(
        targetValue = if (showBurst) 1.67f else 1.0f, // 140dp / 84dp ≈ 1.67
        animationSpec = tween(
            durationMillis = 350,
            easing = FastOutSlowInEasing
        ),
        label = "burst_scale"
    )
    val burstAlpha by animateFloatAsState(
        targetValue = if (showBurst) 0f else 0.3f,
        animationSpec = tween(
            durationMillis = 350,
            easing = FastOutSlowInEasing
        ),
        finishedListener = { showBurst = false },
        label = "burst_alpha"
    )
    
    LaunchedEffect(isCapturing) {
        if (isCapturing) {
            showBurst = true
        }
    }
    
    Box(
        modifier = modifier
            .size(buttonSize)
            .semantics { contentDescription = "Capture photo" },
        contentAlignment = Alignment.Center
    ) {
        // Burst ring animation
        if (showBurst) {
            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .graphicsLayer {
                        scaleX = burstScale
                        scaleY = burstScale
                        alpha = burstAlpha
                    }
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    CameraDesignTokens.Colors.accentStart.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                    }
            )
        }
        
        // Main button
        Surface(
            modifier = Modifier
                .size(buttonSize)
                .graphicsLayer {
                    scaleX = if (!isProcessing) pressScale * pulseScale else 1.0f
                    scaleY = if (!isProcessing) pressScale * pulseScale else 1.0f
                }
                .clickable(
                    enabled = !isCapturing && !isProcessing,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick()
                    },
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ),
            shape = CircleShape,
            color = Color.Transparent
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = CameraDesignTokens.Dimensions.captureButtonStrokeWidth.toPx()
                
                if (isProcessing) {
                    // Static halo when processing
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                CameraDesignTokens.Colors.accentStart.copy(alpha = 0.3f),
                                CameraDesignTokens.Colors.accentEnd.copy(alpha = 0.3f)
                            )
                        ),
                        style = Stroke(width = strokeWidth)
                    )
                } else {
                    // Gradient outer ring
                    drawCircle(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                CameraDesignTokens.Colors.accentStart,
                                CameraDesignTokens.Colors.accentEnd,
                                CameraDesignTokens.Colors.accentStart
                            )
                        ),
                        style = Stroke(width = strokeWidth)
                    )
                    
                    // Inner gradient circle
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                CameraDesignTokens.Colors.accentEnd,
                                CameraDesignTokens.Colors.accentStart
                            ),
                            radius = size.minDimension / 2.5f
                        ),
                        radius = size.minDimension / 2.5f
                    )
                }
            }
            
            // Processing spinner
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = CameraDesignTokens.Colors.textPrimary,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

/**
 * Mode toggle capsule
 */
@Composable
private fun ModeToggle(
    currentMode: CameraMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .size(
                width = CameraDesignTokens.Dimensions.modeToggleWidth,
                height = CameraDesignTokens.Dimensions.modeToggleHeight
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(CameraDesignTokens.Dimensions.modeToggleCornerRadius),
        color = CameraDesignTokens.Colors.glass
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            androidx.compose.animation.Crossfade(
                targetState = currentMode,
                animationSpec = tween(durationMillis = CameraDesignTokens.Motion.medium),
                label = "mode_crossfade"
            ) { mode ->
                Text(
                    text = mode.name,
                    fontSize = CameraDesignTokens.Typography.modeToggleFontSize.sp,
                    color = CameraDesignTokens.Colors.textPrimary
                )
            }
        }
    }
}

/**
 * Glass background effect
 */
@Composable
private fun GlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(CameraDesignTokens.CornerRadius.pill))
            .then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier.blur(12.dp)
                } else {
                    Modifier
                }
            )
            .background(
                color = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    CameraDesignTokens.Colors.glass
                } else {
                    CameraDesignTokens.Colors.glass.copy(alpha = 0.12f)
                }
            )
    ) {
        content()
    }
}

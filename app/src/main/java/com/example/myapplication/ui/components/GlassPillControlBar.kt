package com.example.myapplication.ui.components

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.myapplication.ui.theme.DesignTokens
import kotlinx.coroutines.launch

enum class CameraMode {
    Photo,
    Video
}

/**
 * Glass pill control bar positioned at bottom of camera screen
 * Height: 112dp with gallery thumbnail, capture button, and mode toggles
 */
@Composable
fun GlassPillControlBar(
    modifier: Modifier = Modifier,
    lastCapturedImage: Any? = null,  // Can be Uri, Bitmap, or any Coil-compatible model
    cameraMode: CameraMode = CameraMode.Photo,
    captureState: CaptureButtonState = CaptureButtonState.Idle,
    onGalleryClick: () -> Unit = {},
    onCaptureClick: () -> Unit = {},
    onModeToggle: (CameraMode) -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    
    // Gallery thumbnail animation
    var thumbnailAnimTrigger by remember { mutableStateOf(0) }
    val thumbnailScale by animateFloatAsState(
        targetValue = if (thumbnailAnimTrigger > 0) 1f else DesignTokens.GalleryThumbnail.animScaleFrom,
        animationSpec = spring(
            stiffness = DesignTokens.GalleryThumbnail.springStiffness,
            dampingRatio = DesignTokens.GalleryThumbnail.springDamping
        ),
        label = "thumbnailScale"
    )
    
    // Trigger thumbnail animation when image changes
    LaunchedEffect(lastCapturedImage) {
        if (lastCapturedImage != null) {
            thumbnailAnimTrigger++
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DesignTokens.GlassPill.horizontalMargin)
            .height(DesignTokens.GlassPill.height)
            .shadow(
                elevation = DesignTokens.GlassPill.shadowY,
                shape = RoundedCornerShape(DesignTokens.GlassPill.cornerRadius),
                spotColor = Color.Black.copy(alpha = DesignTokens.GlassPill.shadowAlpha)
            )
            .clip(RoundedCornerShape(DesignTokens.GlassPill.cornerRadius))
            .then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier.graphicsLayer {
                        renderEffect = android.graphics.RenderEffect.createBlurEffect(
                            DesignTokens.GlassPill.blurRadius,
                            DesignTokens.GlassPill.blurRadius,
                            android.graphics.Shader.TileMode.CLAMP
                        ).asComposeRenderEffect()
                    }
                } else {
                    Modifier.background(
                        Color.Black.copy(alpha = DesignTokens.GlassPill.backgroundAlpha)
                    )
                }
            )
    ) {
        // Glass overlay
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DesignTokens.Colors.glass.copy(alpha = DesignTokens.GlassPill.backgroundAlpha),
                            DesignTokens.Colors.glass.copy(alpha = DesignTokens.GlassPill.backgroundAlpha * 0.6f)
                        )
                    )
                )
        )
        
        // Content layout
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = DesignTokens.GlassPill.innerPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left zone: Gallery thumbnail
            Box(
                modifier = Modifier
                    .size(DesignTokens.GalleryThumbnail.size)
                    .scale(thumbnailScale)
                    .clip(RoundedCornerShape(DesignTokens.GalleryThumbnail.cornerRadius))
                    .border(
                        width = 1.dp,
                        color = DesignTokens.Colors.glass.copy(alpha = DesignTokens.GalleryThumbnail.borderAlpha),
                        shape = RoundedCornerShape(DesignTokens.GalleryThumbnail.cornerRadius)
                    )
                    .clickable(
                        role = Role.Button,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onGalleryClick()
                        }
                    )
                    .semantics {
                        contentDescription = "Open gallery"
                    },
                contentAlignment = Alignment.Center
            ) {
                if (lastCapturedImage != null) {
                    AsyncImage(
                        model = lastCapturedImage,
                        contentDescription = "Last captured image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Placeholder gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF2A2A2A),
                                        Color(0xFF1A1A1A)
                                    )
                                )
                            )
                    )
                }
            }
            
            // Center zone: Capture button
            PreciseCaptureButton(
                state = captureState,
                onCapture = onCaptureClick
            )
            
            // Right zone: Mode toggles + settings
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Mode capsule
                ModeToggleCapsule(
                    currentMode = cameraMode,
                    onModeChange = onModeToggle
                )
                
                // Settings gear
                IconButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSettingsClick()
                    },
                    modifier = Modifier
                        .size(DesignTokens.SettingsIcon.size)
                        .semantics {
                            contentDescription = "Camera settings"
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = DesignTokens.Colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Mode toggle capsule for switching between Photo/Video
 */
@Composable
@OptIn(ExperimentalAnimationApi::class)
private fun ModeToggleCapsule(
    currentMode: CameraMode,
    onModeChange: (CameraMode) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    
    Box(
        modifier = Modifier
            .size(
                width = DesignTokens.ModeToggle.width,
                height = DesignTokens.ModeToggle.height
            )
            .clip(RoundedCornerShape(DesignTokens.ModeToggle.cornerRadius))
            .background(
                DesignTokens.Colors.glass.copy(alpha = DesignTokens.ModeToggle.backgroundAlpha)
            )
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                val newMode = if (currentMode == CameraMode.Photo) CameraMode.Video else CameraMode.Photo
                onModeChange(newMode)
            }
            .semantics {
                contentDescription = "Toggle camera mode"
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = currentMode,
            transitionSpec = {
                fadeIn(
                    animationSpec = tween(DesignTokens.ModeToggle.crossfadeDuration)
                ) with fadeOut(
                    animationSpec = tween(DesignTokens.ModeToggle.crossfadeDuration)
                )
            },
            label = "modeText"
        ) { mode ->
            Text(
                text = when (mode) {
                    CameraMode.Photo -> "PHOTO"
                    CameraMode.Video -> "VIDEO"
                },
                fontSize = DesignTokens.ModeToggle.fontSize,
                fontWeight = FontWeight.Medium,
                color = DesignTokens.Colors.textPrimary
            )
        }
    }
}

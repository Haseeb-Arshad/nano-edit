package com.example.myapplication.ui.components

import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.myapplication.ui.theme.DesignTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class FocusPoint(
    val x: Float,
    val y: Float,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Camera preview container with exact layout specifications
 * Height: 70% of available screen height
 * Corner radius: 20dp
 * Includes overlay controls (grid, focus reticle, exposure readout)
 */
@Composable
fun CameraPreviewContainer(
    modifier: Modifier = Modifier,
    previewView: PreviewView,
    showGrid: Boolean = false,
    exposureValue: String? = null,
    onTap: (Offset) -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    
    // Calculate preview height (70% of available height)
    val screenHeight = configuration.screenHeightDp.dp
    val availableHeight = screenHeight - DesignTokens.SafeArea.statusBarHeight - DesignTokens.SafeArea.navBarHeight
    val previewHeight = availableHeight * DesignTokens.CameraPreview.heightPercent
    
    // Focus reticle state
    var focusPoint by remember { mutableStateOf<FocusPoint?>(null) }
    val scope = rememberCoroutineScope()
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DesignTokens.CameraPreview.horizontalMargin)
            .height(previewHeight)
            .shadow(
                elevation = DesignTokens.CameraPreview.shadowY,
                shape = RoundedCornerShape(DesignTokens.CameraPreview.cornerRadius),
                ambientColor = Color.Black.copy(alpha = DesignTokens.CameraPreview.shadowAlpha),
                spotColor = Color.Black.copy(alpha = DesignTokens.CameraPreview.shadowAlpha * 1.5f)
            )
            .clip(RoundedCornerShape(DesignTokens.CameraPreview.cornerRadius))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { /* Handled in pointerInput */ }
            )
    ) {
        // Camera preview
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
        
        // Overlay container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(DesignTokens.CameraPreview.innerPadding)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        focusPoint = FocusPoint(offset.x, offset.y)
                        onTap(offset)
                        scope.launch {
                            delay(2000)
                            focusPoint = null
                        }
                    }
                }
        ) {
            // Rule of thirds grid overlay
            AnimatedVisibility(
                visible = showGrid,
                enter = fadeIn(
                    animationSpec = tween(DesignTokens.RuleOfThirds.animDuration)
                ),
                exit = fadeOut(
                    animationSpec = tween(DesignTokens.RuleOfThirds.animDuration)
                )
            ) {
                RuleOfThirdsGrid()
            }
            
            // Focus reticle
            focusPoint?.let { point ->
                FocusReticle(
                    position = Offset(point.x, point.y),
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Exposure readout
            exposureValue?.let { value ->
                ExposureReadout(
                    value = value,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 12.dp, start = 12.dp)
                )
            }
        }
    }
}

/**
 * Rule of thirds grid overlay
 */
@Composable
private fun RuleOfThirdsGrid() {
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val strokeWidth = DesignTokens.RuleOfThirds.strokeWidth
        val strokeColor = Color.White.copy(alpha = DesignTokens.RuleOfThirds.strokeAlpha)
        
        // Vertical lines
        val thirdWidth = size.width / 3
        drawLine(
            color = strokeColor,
            start = Offset(thirdWidth, 0f),
            end = Offset(thirdWidth, size.height),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = strokeColor,
            start = Offset(thirdWidth * 2, 0f),
            end = Offset(thirdWidth * 2, size.height),
            strokeWidth = strokeWidth
        )
        
        // Horizontal lines
        val thirdHeight = size.height / 3
        drawLine(
            color = strokeColor,
            start = Offset(0f, thirdHeight),
            end = Offset(size.width, thirdHeight),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = strokeColor,
            start = Offset(0f, thirdHeight * 2),
            end = Offset(size.width, thirdHeight * 2),
            strokeWidth = strokeWidth
        )
    }
}

/**
 * Focus reticle animation
 */
@Composable
private fun FocusReticle(
    position: Offset,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    
    // Animations
    val animatedScale = remember { Animatable(DesignTokens.FocusReticle.scaleFrom) }
    val animatedAlpha = remember { Animatable(0f) }
    
    LaunchedEffect(position) {
        // Reset and animate
        animatedScale.snapTo(DesignTokens.FocusReticle.scaleFrom)
        animatedAlpha.snapTo(0f)
        
        // Scale and fade in
        launch {
            animatedScale.animateTo(
                targetValue = DesignTokens.FocusReticle.scaleTo,
                animationSpec = tween(
                    durationMillis = DesignTokens.FocusReticle.animDuration,
                    easing = DesignTokens.Motion.easingOutCubic
                )
            )
        }
        
        launch {
            animatedAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = DesignTokens.FocusReticle.animDuration / 2
                )
            )
            // Gentle fade to 60% alpha
            animatedAlpha.animateTo(
                targetValue = DesignTokens.FocusReticle.fadeToAlpha,
                animationSpec = tween(
                    durationMillis = DesignTokens.FocusReticle.fadeDuration
                )
            )
        }
    }
    
    Canvas(
        modifier = modifier
    ) {
        val radius = with(density) { DesignTokens.FocusReticle.diameter.toPx() / 2 }
        
        // Draw reticle circle
        drawCircle(
            color = Color.White.copy(alpha = animatedAlpha.value),
            radius = radius * animatedScale.value,
            center = position,
            style = Stroke(
                width = with(density) { DesignTokens.FocusReticle.strokeWidth.toPx() }
            )
        )
        
        // Draw center dot
        drawCircle(
            color = Color.White.copy(alpha = animatedAlpha.value * 0.8f),
            radius = 2.dp.toPx(),
            center = position
        )
    }
}

/**
 * Exposure/Focus readout pill
 */
@Composable
private fun ExposureReadout(
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(DesignTokens.ExposureReadout.height)
            .clip(RoundedCornerShape(DesignTokens.ExposureReadout.height / 2))
            .background(
                DesignTokens.Colors.glass.copy(alpha = DesignTokens.ExposureReadout.backgroundAlpha)
            )
            .padding(horizontal = DesignTokens.ExposureReadout.horizontalPadding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value,
            fontSize = DesignTokens.ExposureReadout.fontSize,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}

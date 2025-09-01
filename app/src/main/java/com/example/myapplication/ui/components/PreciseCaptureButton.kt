package com.example.myapplication.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.DesignTokens
import kotlinx.coroutines.launch

enum class CaptureButtonState {
    Idle,
    Pressed,
    Capturing,
    Processing
}

/**
 * Pixel-perfect capture button matching exact design specifications
 * Size: 84dp diameter with gradient stroke and animations
 */
@Composable
fun PreciseCaptureButton(
    modifier: Modifier = Modifier,
    state: CaptureButtonState = CaptureButtonState.Idle,
    onCapture: () -> Unit = {}
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scope = rememberCoroutineScope()
    
    // Pulse animation for idle state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = DesignTokens.CaptureButton.pulseScaleMin,
        targetValue = DesignTokens.CaptureButton.pulseScaleMax,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = DesignTokens.CaptureButton.pulseDuration,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    // Press scale animation
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) DesignTokens.CaptureButton.pressScale else 1f,
        animationSpec = tween(
            durationMillis = DesignTokens.CaptureButton.pressDuration,
            easing = FastOutSlowInEasing
        ),
        label = "pressScale"
    )
    
    // Burst ring animation for capturing
    var showBurst by remember { mutableStateOf(false) }
    val burstAlpha by animateFloatAsState(
        targetValue = if (showBurst) 0f else 1f,
        animationSpec = tween(
            durationMillis = DesignTokens.CaptureButton.burstDuration,
            easing = LinearOutSlowInEasing
        ),
        label = "burstAlpha"
    )
    
    val burstScale by animateFloatAsState(
        targetValue = if (showBurst) 1.67f else 1f, // 140dp / 84dp
        animationSpec = tween(
            durationMillis = DesignTokens.CaptureButton.burstDuration,
            easing = LinearOutSlowInEasing
        ),
        label = "burstScale"
    )
    
    // Handle state changes
    LaunchedEffect(state) {
        when (state) {
            CaptureButtonState.Capturing -> {
                showBurst = true
                kotlinx.coroutines.delay(DesignTokens.CaptureButton.burstDuration.toLong())
                showBurst = false
            }
            else -> showBurst = false
        }
    }
    
    val currentScale = when (state) {
        CaptureButtonState.Idle -> pulseScale
        CaptureButtonState.Pressed -> pressScale
        else -> 1f
    }
    
    Box(
        modifier = modifier
            .size(DesignTokens.CaptureButton.outerDiameter)
            .scale(currentScale)
            .semantics {
                contentDescription = "Capture photo"
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCapture()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2
            
            // Draw burst ring if capturing
            if (showBurst && burstAlpha > 0f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            DesignTokens.CaptureButton.accentStart.copy(alpha = 0.3f * burstAlpha),
                            DesignTokens.CaptureButton.accentEnd.copy(alpha = 0f)
                        ),
                        center = center,
                        radius = radius * burstScale
                    ),
                    radius = radius * burstScale,
                    center = center
                )
                
                drawCircle(
                    color = DesignTokens.CaptureButton.accentStart.copy(alpha = 0.6f * burstAlpha),
                    radius = radius * burstScale,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            
            // Main button gradient background
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        DesignTokens.CaptureButton.accentEnd,
                        DesignTokens.CaptureButton.accentStart
                    ),
                    center = center,
                    radius = radius * 0.9f
                ),
                radius = radius * 0.9f,
                center = center
            )
            
            // Gradient stroke ring
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        DesignTokens.CaptureButton.accentStart,
                        DesignTokens.CaptureButton.accentEnd,
                        DesignTokens.CaptureButton.accentStart
                    ),
                    center = center
                ),
                radius = radius - DesignTokens.CaptureButton.strokeWidth.toPx() / 2,
                center = center,
                style = Stroke(
                    width = DesignTokens.CaptureButton.strokeWidth.toPx(),
                    cap = StrokeCap.Round
                )
            )
            
            // Inner shadow for depth
            drawIntoCanvas { canvas ->
                val paint = Paint().apply {
                    color = Color.Black.copy(alpha = 0.2f)
                    asFrameworkPaint().apply {
                        maskFilter = android.graphics.BlurMaskFilter(
                            DesignTokens.CaptureButton.innerShadow.toPx(),
                            android.graphics.BlurMaskFilter.Blur.NORMAL
                        )
                    }
                }
                canvas.drawCircle(
                    center = center,
                    radius = radius * 0.85f,
                    paint = paint
                )
            }
        }
        
        // Content based on state
        when (state) {
            CaptureButtonState.Processing -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(DesignTokens.CaptureButton.spinnerSize),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(DesignTokens.CaptureButton.iconSize),
                    tint = Color.White
                )
            }
        }
    }
}

/**
 * Simplified helper for drawing gradient strokes
 */
private fun DrawScope.drawGradientStroke(
    center: Offset,
    radius: Float,
    strokeWidth: Float,
    colors: List<Color>
) {
    drawCircle(
        brush = Brush.sweepGradient(
            colors = colors,
            center = center
        ),
        radius = radius,
        center = center,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round
        )
    )
}

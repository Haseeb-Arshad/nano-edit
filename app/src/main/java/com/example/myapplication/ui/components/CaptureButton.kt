package com.example.myapplication.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.myapplication.ui.motion.Motion
import com.example.myapplication.ui.motion.MotionTokens

enum class CaptureState { Idle, Pressed, Capturing, Processing, Done }

/**
 * Single composable Capture Button with tactile micro-interactions.
 */
@Composable
fun CaptureButton(
    modifier: Modifier = Modifier,
    buttonSize: Dp = 72.dp,
    ringColor: Color = MotionTokens.ColorPrimary,
    onCapture: (() -> Unit)? = null,
    state: MutableState<CaptureState>? = null,
    autoAdvance: Boolean = true
) {
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val scope = rememberCoroutineScope()

    val internalState = remember { mutableStateOf(CaptureState.Idle) }
    val currentState = state ?: internalState

    // Idle pulse glow
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(currentState.value) {
        if (currentState.value == CaptureState.Idle) {
            pulse.snapTo(0f)
            pulse.animateTo(
                1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            pulse.stop()
            pulse.snapTo(0f)
        }
    }

    // Press scale
    val scale by animateFloatAsState(
        targetValue = when (currentState.value) {
            CaptureState.Pressed -> 0.92f
            else -> 1f
        },
        animationSpec = tween(Motion.Durations.Small, easing = Motion.Easings.Standard),
        label = "pressScale"
    )

    // Ring burst animation value (0..1)
    val ring = remember { Animatable(0f) }

    // Shutter flash alpha
    val flash = remember { Animatable(0f) }

    LaunchedEffect(currentState.value) {
        when (currentState.value) {
            CaptureState.Capturing -> {
                ring.snapTo(0f)
                flash.snapTo(0f)
                // Burst ring + flash
                launch {
                    ring.animateTo(1f, tween(durationMillis = Motion.Durations.Medium, easing = Motion.Easings.Standard))
                }
                launch {
                    flash.animateTo(0.8f, tween(Motion.Durations.Small))
                    flash.animateTo(0f, tween(Motion.Durations.Medium))
                }
            }
            else -> Unit
        }
    }

    // State machine auto-advance
    LaunchedEffect(currentState.value, autoAdvance) {
        if (!autoAdvance) return@LaunchedEffect
        when (currentState.value) {
            CaptureState.Pressed -> {
                delay(60)
                currentState.value = CaptureState.Capturing
            }
            CaptureState.Capturing -> {
                delay(Motion.Durations.Medium.toLong())
                currentState.value = CaptureState.Processing
            }
            CaptureState.Processing -> {
                delay(360)
                currentState.value = CaptureState.Done
            }
            CaptureState.Done -> {
                delay(180)
                currentState.value = CaptureState.Idle
            }
            else -> Unit
        }
    }

    val innerColor = MaterialTheme.colorScheme.surface
    val borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .size(buttonSize)
            .clip(CircleShape)
            .drawBehind {
                // Soft glow (idle pulse)
                if (pulse.value > 0f && currentState.value == CaptureState.Idle) {
                    drawCircle(
                        color = ringColor.copy(alpha = 0.25f * pulse.value),
                        radius = size.minDimension * 0.6f
                    )
                }
                // Shutter flash overlay
                if (flash.value > 0f) {
                    drawRect(Color.White.copy(alpha = 0.6f * flash.value))
                }
            }
            .background(Color.Transparent)
            .semantics { stateDescription = "${currentState.value}" }
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = {
                    // Press -> Capturing chain
                    if (currentState.value == CaptureState.Idle) {
                        currentState.value = CaptureState.Pressed
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onCapture?.invoke()
                        scope.launch {
                            delay(40)
                            currentState.value = CaptureState.Capturing
                        }
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Outer ring burst
        Canvas(Modifier.fillMaxSize()) {
            if (ring.value > 0f && ring.value < 1.1f) {
                val stroke = size.minDimension * 0.08f
                drawCircle(
                    color = ringColor,
                    radius = (size.minDimension / 2f) * (0.6f + 0.4f * ring.value),
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
            // Outer border (static)
            drawCircle(
                color = borderColor,
                radius = (size.minDimension / 2f) * 0.98f,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Inner disk with press scale
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(buttonSize * 0.78f)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(CircleShape)
                .background(innerColor)
        )

        // Processing spinner overlay (no layout shift)
        if (currentState.value == CaptureState.Processing) {
            CircularProgressIndicator(
                modifier = Modifier.size(buttonSize * 0.42f),
                color = ringColor,
                strokeWidth = 2.5.dp
            )
        }
    }
}

package com.example.myapplication.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.motion.Motion
import kotlinx.coroutines.launch
import kotlin.math.round

/**
 * Before/After compare slider with GPU-friendly clipping.
 */
@Composable
fun CompareSlider(
    modifier: Modifier = Modifier,
    handleWidth: Dp = 3.dp,
    initialFraction: Float = 0.5f,
    onFractionChanged: (Float) -> Unit = {},
    original: ImageBitmap,
    enhanced: ImageBitmap,
    contentScale: ContentScale = ContentScale.Crop,
    originalContentDescription: String? = "Original",
    enhancedContentDescription: String? = "Enhanced"
) {
    val haptics = LocalHapticFeedback.current
    var size by remember { mutableStateOf(IntSize.Zero) }
    var fraction by remember { mutableStateOf(initialFraction.coerceIn(0f, 1f)) }
    val anim = remember { Animatable(fraction) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val knobHalfWidthPx = with(density) { 12.dp.toPx() }
    val knobRadiusPx = with(density) { 18.dp.toPx() }

    LaunchedEffect(Unit) { anim.snapTo(fraction) }

    Box(
        modifier
            .fillMaxSize()
            .semantics {
                contentDescription = "Compare slider"
                stateDescription = String.format("%.2f", fraction)
            }
    ) {
        // After content in the back
        Image(
            bitmap = enhanced,
            contentDescription = enhancedContentDescription,
            modifier = Modifier.matchParentSize(),
            contentScale = contentScale
        )

        // Before content clipped to fraction
        run {
            val targetWidthDp = with(density) { ((size.width * anim.value).toInt()).toDp() }
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(targetWidthDp)
                    .clipToBounds()
            ) {
                Image(
                    bitmap = original,
                    contentDescription = originalContentDescription,
                    modifier = Modifier.matchParentSize(),
                    contentScale = contentScale
                )
            }
        }

        // Handle
        val handleColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
        Box(
            Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        },
                        onDrag = { change, drag ->
                            if (size.width > 0) {
                                val delta = drag.x / size.width
                                fraction = (fraction + delta).coerceIn(0f, 1f)
                                onFractionChanged(fraction)
                                scope.launch { anim.snapTo(fraction) }
                            }
                        },
                        onDragEnd = {
                            // Snap if near edges or center
                            val target = when {
                                fraction < 0.08f -> 0f
                                fraction > 0.92f -> 1f
                                kotlin.math.abs(fraction - 0.5f) < 0.06f -> 0.5f
                                else -> fraction
                            }
                            scope.launch { anim.animateTo(target, tween(Motion.Durations.Small)) }
                            if (target == 0f || target == 1f || target == 0.5f)
                                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        }
                    )
                }
                .onSizeChanged { size = it }
        ) {
            val x = size.width * anim.value
            // Center knob
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .graphicsLayer { translationX = x - knobHalfWidthPx }
                    .width(handleWidth)
                    .fillMaxSize()
                    .background(handleColor.copy(alpha = 0.7f))
            )
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .graphicsLayer { translationX = x - knobRadiusPx }
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(handleColor.copy(alpha = 0.9f))
            )
        }
    }
}

/**
 * Overload using composable content for before/after. Uses clipRect for GPU-friendly reveal.
 */
@Composable
fun CompareSlider(
    modifier: Modifier = Modifier,
    handleWidth: Dp = 3.dp,
    initialFraction: Float = 0.5f,
    onFractionChanged: (Float) -> Unit = {},
    before: @Composable () -> Unit,
    after: @Composable () -> Unit
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var fraction by remember { mutableStateOf(initialFraction.coerceIn(0f, 1f)) }
    val anim = remember { Animatable(fraction) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(Unit) { anim.snapTo(fraction) }

    Box(
        modifier
            .fillMaxSize()
            .semantics {
                contentDescription = "Compare slider"
                stateDescription = String.format("%.2f", fraction)
            }
    ) {
        after()
        Box(
            Modifier
                .matchParentSize()
                .drawWithCache {
                    onDrawWithContent {
                        val w = size.width * anim.value
                        clipRect(left = 0f, top = 0f, right = w, bottom = size.height.toFloat()) {
                            this@onDrawWithContent.drawContent()
                        }
                    }
                }
        ) { before() }

        // Handle and gestures
        val handleColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
        Box(
            Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        },
                        onDrag = { change, drag ->
                            if (size.width > 0) {
                                val delta = drag.x / size.width
                                fraction = (fraction + delta).coerceIn(0f, 1f)
                                onFractionChanged(fraction)
                                scope.launch { anim.snapTo(fraction) }
                            }
                        },
                        onDragEnd = {
                            val target = when {
                                fraction < 0.08f -> 0f
                                fraction > 0.92f -> 1f
                                kotlin.math.abs(fraction - 0.5f) < 0.06f -> 0.5f
                                else -> fraction
                            }
                            scope.launch { anim.animateTo(target, tween(Motion.Durations.Small)) }
                            if (target == 0f || target == 1f || target == 0.5f)
                                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        }
                    )
                }
                .onSizeChanged { size = it }
        ) {
            val x = size.width * anim.value
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .graphicsLayer { translationX = x - 12.dp.toPx() }
                    .width(handleWidth)
                    .fillMaxSize()
                    .background(handleColor.copy(alpha = 0.7f))
            )
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .graphicsLayer { translationX = x - 18.dp.toPx() }
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(handleColor.copy(alpha = 0.9f))
            )
        }
    }
}

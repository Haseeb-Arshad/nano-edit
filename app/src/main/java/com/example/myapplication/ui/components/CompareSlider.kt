package com.example.myapplication.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription

@Composable
fun CompareSlider(
    modifier: Modifier = Modifier,
    original: ImageBitmap,
    enhanced: ImageBitmap,
    initialFraction: Float = 0.6f
) {
    var fraction by remember { mutableFloatStateOf(initialFraction.coerceIn(0f, 1f)) }
    val animFraction by animateFloatAsState(targetValue = fraction, label = "compareFraction")

    Box(modifier = modifier.semantics { this.contentDescription = "Before/After compare slider. Drag horizontally to compare." }) {
        val handleColor = MaterialTheme.colorScheme.primary
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val x = change.position.x
                        val width = size.width
                        fraction = (x / width).coerceIn(0.05f, 0.95f)
                    }
                }
        ) {
            // Draw original full
            drawImage(original)

            // Draw enhanced clipped to fraction
            val clipWidth = size.width * animFraction
            clipRect(left = 0f, top = 0f, right = clipWidth, bottom = size.height) {
                drawImage(enhanced)
            }

            // Draw handle
            val handleX = clipWidth
            drawLine(
                color = handleColor,
                start = Offset(handleX, 0f),
                end = Offset(handleX, size.height),
                strokeWidth = 2.dp.toPx()
            )
            // Circles at mid to improve affordance
            val radius = 6.dp.toPx()
            drawCircle(color = handleColor, radius = radius, center = Offset(handleX, size.height * 0.5f))
        }
    }
}

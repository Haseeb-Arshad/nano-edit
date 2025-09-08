package com.example.myapplication.ui.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Liquid Glass surfaces and controls.
 */

private val GlassShape = RoundedCornerShape(20.dp)

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = GlassShape,
    tint: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.22f),
    borderGradient: Brush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.35f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        )
    ),
    content: @Composable () -> Unit
) {
    val blurRadius = if (Build.VERSION.SDK_INT >= 31) 20.dp else 0.dp
    Surface(
        modifier = modifier
            .clip(shape)
            .then(if (blurRadius > 0.dp) Modifier.blur(blurRadius, edgeTreatment = BlurredEdgeTreatment.Unbounded) else Modifier)
            .background(tint)
            .padding(1.dp),
        shape = shape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, borderGradient)
    ) { content() }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = GlassShape,
    content: @Composable () -> Unit
) {
    GlassSurface(modifier = modifier, shape = shape) { content() }
}

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    content: @Composable RowScope.() -> Unit
) {
    val bg by animateColorAsState(MaterialTheme.colorScheme.surface.copy(alpha = 0.28f), label = "glassBtnBg")
    val contentColor = MaterialTheme.colorScheme.onSurface
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        colors = ButtonDefaults.outlinedButtonColors(containerColor = bg, contentColor = contentColor),
        border = BorderStroke(1.dp, Brush.linearGradient(listOf(Color.White.copy(0.45f), MaterialTheme.colorScheme.primary.copy(0.45f))))
    ) { content() }
}

@Composable
fun ShutterButtonClassic(
    isCapturing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Simple glassy shutter: gradient ring + inner soft circle
    val ring = Brush.radialGradient(listOf(MaterialTheme.colorScheme.primary.copy(0.9f), Color.Transparent))
    androidx.compose.foundation.Canvas(modifier = modifier.semantics { this.contentDescription = if (isCapturing) "Capturing" else "Shutter" }) {
        val sizePx = size.minDimension
        val radius = sizePx / 2f
        // Outer ring
        drawCircle(brush = ring, radius = radius)
        // Inner circle
        drawCircle(color = Color.White.copy(alpha = if (isCapturing) 0.7f else 0.9f), radius = radius * 0.72f)
    }
    // Clickable overlay
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .padding(0.dp)
            .background(Color.Transparent)
    ) {
        androidx.compose.material3.FloatingActionButton(
            onClick = onClick,
            containerColor = Color.Transparent,
            contentColor = Color.Transparent
        ) { /* tappable hit target */ }
    }
}

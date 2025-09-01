package com.example.myapplication.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.motion.Motion
import com.example.myapplication.ui.motion.MotionTokens

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    clickable: Boolean = false,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = 12.dp,
    backdrop: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val elevation by animateDpAsState(
        targetValue = if (isPressed) Motion.Elevation.Raised else Motion.Elevation.Card,
        animationSpec = tween(Motion.Durations.Small),
        label = "elev"
    )

    val shape = RoundedCornerShape(cornerRadius)
    val glassOverlay = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.surface.copy(alpha = 0.32f),
            MaterialTheme.colorScheme.surface.copy(alpha = 0.18f)
        )
    )

    Box(
        modifier
            .clip(shape)
            .graphicsLayer {
                if (Build.VERSION.SDK_INT >= 31 && backdrop != null) {
                    // Blur the layer content (provided backdrop slot)
                    renderEffect = RenderEffect.createBlurEffect(
                        MotionTokens.BlurRadiusHighApi,
                        MotionTokens.BlurRadiusHighApi,
                        Shader.TileMode.CLAMP
                    ).asComposeRenderEffect()
                }
                shadowElevation = elevation.toPx()
            }
            .background(
                if (Build.VERSION.SDK_INT >= 31 && backdrop != null)
                    Color.Transparent else MaterialTheme.colorScheme.surface.copy(alpha = MotionTokens.BlurFallbackOverlayAlpha)
            )
            .then(if (clickable && onClick != null) Modifier.clickable(interactionSource = interaction, indication = null) { onClick() } else Modifier)
    ) {
        // Provide a backdrop slot that will be blurred (API 31+). On older API it will simply render beneath overlay.
        if (backdrop != null) backdrop()
        // Glass overlay tint + stroke edge
        Box(
            Modifier
                .matchParentSize()
                .background(glassOverlay)
                .drawWithCache {
                    onDrawWithContent {
                        drawContent()
                        // Soft stroke
                        drawRoundRect(
                            color = MotionTokens.GlassStroke,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                        )
                    }
                }
                .padding(contentPadding)
        ) {
            content()
        }
    }
}


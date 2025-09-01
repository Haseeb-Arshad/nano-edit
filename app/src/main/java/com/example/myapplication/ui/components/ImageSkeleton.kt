package com.example.myapplication.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.example.myapplication.ui.motion.MotionTokens

@Composable
fun ShimmerPlaceholder(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "x"
    )
    val base = Color(0xFF1A1C22)
    val highlight = Color(0xFF2A2D36)
    Box(
        modifier
            .clip(RoundedCornerShape(cornerRadius))
            .drawWithCache {
                onDrawBehind {
                    drawRect(base)
                    val w = size.width
                    val startX = (x * w)
                    val brush = Brush.linearGradient(
                        colors = listOf(base, highlight, base),
                        start = Offset(startX - w, 0f),
                        end = Offset(startX, 0f)
                    )
                    drawRect(brush = brush, alpha = 0.9f)
                }
            }
            .fillMaxSize()
    )
}

@Composable
fun ImageWithSkeleton(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    cornerRadius: Dp = 16.dp,
    loadingOverlay: Boolean = true
) {
    SubcomposeAsyncImage(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier.clip(RoundedCornerShape(cornerRadius)),
        contentScale = contentScale
    ) {
        when (val state = painter.state) {
            is coil.compose.AsyncImagePainter.State.Loading, is coil.compose.AsyncImagePainter.State.Empty -> {
                ShimmerPlaceholder(modifier = Modifier.fillMaxSize(), cornerRadius = cornerRadius)
            }
            is coil.compose.AsyncImagePainter.State.Error -> {
                ShimmerPlaceholder(modifier = Modifier.fillMaxSize(), cornerRadius = cornerRadius)
            }
            else -> SubcomposeAsyncImageContent()
        }
    }
}

package com.example.myapplication.ui.navigation

import android.os.Build
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.myapplication.ui.motion.Motion
import com.example.myapplication.ui.motion.MotionTokens
import kotlin.math.max

/**
 * Shared element style transition for image hero animations.
 * Smooth scale + translation + color crossfade + corner radius morph.
 * Fallback on older devices: crossfade only.
 */
@Composable
fun HeroImage(
    modifier: Modifier = Modifier,
    model: Any?,
    contentDescription: String?,
    isExpanded: Boolean,
    startSize: IntSize,
    startOffset: IntOffset,
    endCornerRadius: Dp = 0.dp,
    startCornerRadius: Dp = 20.dp,
    crossfadeOnlyFallback: Boolean = Build.VERSION.SDK_INT < 26
): Unit = BoxWithConstraints(modifier) {
    val density = LocalDensity.current
    val endWidthPx = with(density) { maxWidth.toPx() }
    val endHeightPx = with(density) { maxHeight.toPx() }

    if (crossfadeOnlyFallback) {
        // Simpler, reliable fallback: rely on Coil crossfade
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(if (isExpanded) endCornerRadius else startCornerRadius)),
            contentScale = ContentScale.Crop
        )
        return@BoxWithConstraints
    }

    val transition = updateTransition(targetState = isExpanded, label = "hero")
    val scale by transition.animateFloat(
        transitionSpec = { Motion.Springs.Standard },
        label = "scale"
    ) { expanded -> if (expanded) 1f else max(
        startSize.width / endWidthPx,
        startSize.height / endHeightPx
    ) }

    val translationX by transition.animateFloat(
        transitionSpec = { Motion.Springs.Standard },
        label = "tx"
    ) { expanded -> if (expanded) 0f else startOffset.x.toFloat() }
    val translationY by transition.animateFloat(
        transitionSpec = { Motion.Springs.Standard },
        label = "ty"
    ) { expanded -> if (expanded) 0f else startOffset.y.toFloat() }

    val corner by transition.animateDp(
        transitionSpec = { androidx.compose.animation.core.spring() },
        label = "corner"
    ) { expanded -> if (expanded) endCornerRadius else startCornerRadius }

    // Color crossfade by overlaying same image with animated alpha
    val overlayAlpha by transition.animateFloat(
        transitionSpec = { Motion.Springs.Standard },
        label = "alpha"
    ) { expanded -> if (expanded) 0f else 1f }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                this.scaleX = scale
                this.scaleY = scale
                this.translationX = translationX
                this.translationY = translationY
            }
            .clip(RoundedCornerShape(corner))
    ) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )
        // Overlay with fading alpha for nicer color crossfade feel
        Image(
            painter = rememberAsyncImagePainter(model = model),
            contentDescription = null,
            modifier = Modifier
                .matchParentSize()
                .drawWithContent { this.drawContent() }
                .graphicsLayer { this.alpha = overlayAlpha },
            contentScale = ContentScale.Crop
        )
    }
}

/**
 * Sample: camera thumbnail → editor full-screen with shared element behavior.
 */
@Composable
fun HeroSample(
    thumbnailModel: Any,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var startSize by remember { mutableStateOf(IntSize(0, 0)) }
    var startOffset by remember { mutableStateOf(IntOffset(0, 0)) }

    Box(modifier) {
        // Start: small thumbnail
        AsyncImage(
            model = thumbnailModel,
            contentDescription = "Preview thumbnail",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(96.dp)
                .clip(RoundedCornerShape(20.dp))
                .onGloballyPositioned { coords ->
                    startSize = coords.size
                    val pos = coords.positionInRoot()
                    startOffset = IntOffset(pos.x.toInt(), pos.y.toInt())
                }
                .clickable { expanded = true }
        )

        if (expanded) {
            HeroImage(
                modifier = Modifier.fillMaxSize(),
                model = thumbnailModel,
                contentDescription = "Expanded image",
                isExpanded = true,
                startSize = startSize,
                startOffset = startOffset,
                endCornerRadius = 0.dp,
                startCornerRadius = 20.dp,
                crossfadeOnlyFallback = Build.VERSION.SDK_INT < 26
            )

            // Auto collapse after motion sample duration for demo
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay((MotionTokens.DurationLarge + 120).toLong())
                expanded = false
            }
        }
    }
}


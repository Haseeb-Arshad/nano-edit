package com.example.myapplication.ui.camera

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*

/**
 * Hero transition animation specs as per design document
 */
object HeroSpecs {
    // Durations
    const val HERO_DURATION = 420 // milliseconds
    const val CROSSFADE_DURATION = 240 // fallback duration
    
    // Custom easing for camera-to-review transition
    val HeroEasing = CubicBezierEasing(0.2f, 0.9f, 0.2f, 1.0f)
}

/**
 * Hero transition navigation callback
 * Creates a smooth animation between camera and review screens
 */
@Composable
fun createHeroTransition(
    bounds: HeroBounds? = null,
    onComplete: () -> Unit = {}
): EnterTransition {
    // Use smooth crossfade animation if no bounds
    if (bounds == null) {
        return fadeIn(
            animationSpec = tween(
                durationMillis = HeroSpecs.CROSSFADE_DURATION
            )
        )
    }
    
    // Fade in with hero easing
    return fadeIn(
        animationSpec = tween(
            durationMillis = HeroSpecs.HERO_DURATION,
            easing = HeroSpecs.HeroEasing
        )
    )
}

/**
 * Simple data class for capturing screen element bounds
 */
data class HeroBounds(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val cornerRadius: Float = 0f
)

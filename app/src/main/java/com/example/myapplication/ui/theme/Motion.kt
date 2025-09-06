package com.example.myapplication.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object Motion {
    // Apple-inspired gentle springs
    val GentleSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
    val SubtleSpring = spring<Float>(
        dampingRatio = 0.8f,
        stiffness = 200f
    )

    // Curves
    val EaseOut: Easing = FastOutSlowInEasing

    // Timings
    fun Fast(durationMs: Int = 160) = tween<Float>(durationMs, easing = EaseOut)
    fun Normal(durationMs: Int = 220) = tween<Float>(durationMs, easing = EaseOut)
    fun Slow(durationMs: Int = 320) = tween<Float>(durationMs, easing = EaseOut)
}


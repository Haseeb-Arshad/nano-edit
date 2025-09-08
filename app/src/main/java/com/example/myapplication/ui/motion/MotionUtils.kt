package com.example.myapplication.ui.motion

import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Consistent motion and animation utilities following Apple-inspired design principles
 */
object MotionUtils {
    
    // Standard durations (in milliseconds)
    const val DURATION_SHORT = 150
    const val DURATION_MEDIUM = 300
    const val DURATION_LONG = 500
    
    // Standard easing curves
    val EaseInOut = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
    val EaseOut = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    val EaseIn = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
    val SpringBouncy = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
    val SpringSmooth = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )
    
    // Button press animation
    fun buttonPressSpec() = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessHigh
    )
    
    // Scale animations
    fun scaleInSpec() = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
    
    fun scaleOutSpec() = tween<Float>(
        durationMillis = DURATION_SHORT,
        easing = EaseIn
    )
    
    // Slide transitions
    fun slideInFromTop(): EnterTransition = slideInVertically(
        initialOffsetY = { -it },
        animationSpec = tween(DURATION_MEDIUM, easing = EaseOut)
    ) + fadeIn(animationSpec = tween(DURATION_MEDIUM))
    
    fun slideOutToTop(): ExitTransition = slideOutVertically(
        targetOffsetY = { -it },
        animationSpec = tween(DURATION_MEDIUM, easing = EaseIn)
    ) + fadeOut(animationSpec = tween(DURATION_MEDIUM))
    
    fun slideInFromBottom(): EnterTransition = slideInVertically(
        initialOffsetY = { it },
        animationSpec = tween(DURATION_MEDIUM, easing = EaseOut)
    ) + fadeIn(animationSpec = tween(DURATION_MEDIUM))
    
    fun slideOutToBottom(): ExitTransition = slideOutVertically(
        targetOffsetY = { it },
        animationSpec = tween(DURATION_MEDIUM, easing = EaseIn)
    ) + fadeOut(animationSpec = tween(DURATION_MEDIUM))
    
    fun slideInFromRight(): EnterTransition = slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = tween(DURATION_MEDIUM, easing = EaseOut)
    ) + fadeIn(animationSpec = tween(DURATION_MEDIUM))
    
    fun slideOutToRight(): ExitTransition = slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = tween(DURATION_MEDIUM, easing = EaseIn)
    ) + fadeOut(animationSpec = tween(DURATION_MEDIUM))
    
    fun slideInFromLeft(): EnterTransition = slideInHorizontally(
        initialOffsetX = { -it },
        animationSpec = tween(DURATION_MEDIUM, easing = EaseOut)
    ) + fadeIn(animationSpec = tween(DURATION_MEDIUM))
    
    fun slideOutToLeft(): ExitTransition = slideOutHorizontally(
        targetOffsetX = { -it },
        animationSpec = tween(DURATION_MEDIUM, easing = EaseIn)
    ) + fadeOut(animationSpec = tween(DURATION_MEDIUM))
    
    // Fade transitions
    fun fadeInSpec() = fadeIn(animationSpec = tween(DURATION_MEDIUM, easing = EaseOut))
    fun fadeOutSpec() = fadeOut(animationSpec = tween(DURATION_MEDIUM, easing = EaseIn))
    
    // Sheet animations
    fun sheetSlideIn(): EnterTransition = slideInVertically(
        initialOffsetY = { it },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    ) + fadeIn(animationSpec = tween(DURATION_SHORT))
    
    fun sheetSlideOut(): ExitTransition = slideOutVertically(
        targetOffsetY = { it },
        animationSpec = tween(DURATION_MEDIUM, easing = EaseIn)
    ) + fadeOut(animationSpec = tween(DURATION_SHORT))
}

/**
 * Haptic feedback utilities
 */
object HapticUtils {
    
    @Composable
    fun rememberHapticPerformer(): (HapticType) -> Unit {
        val haptics = LocalHapticFeedback.current
        return { type ->
            when (type) {
                HapticType.Light -> haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                HapticType.Medium -> haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                HapticType.Heavy -> haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                HapticType.Selection -> haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }
    
    enum class HapticType {
        Light,    // For subtle interactions like slider changes
        Medium,   // For button presses
        Heavy,    // For important actions like capture
        Selection // For selection changes
    }
}

/**
 * Animation state helpers
 */
object AnimationStateUtils {
    
    /**
     * Creates a smooth float animation with spring physics
     */
    @Composable
    fun animateFloatSpring(
        targetValue: Float,
        dampingRatio: Float = Spring.DampingRatioMediumBouncy,
        stiffness: Float = Spring.StiffnessLow
    ) = animateFloatAsState(
        targetValue = targetValue,
        animationSpec = spring(
            dampingRatio = dampingRatio,
            stiffness = stiffness
        )
    )
    
    /**
     * Creates a smooth float animation with tween
     */
    @Composable
    fun animateFloatTween(
        targetValue: Float,
        durationMillis: Int = MotionUtils.DURATION_MEDIUM,
        easing: Easing = MotionUtils.EaseInOut
    ) = animateFloatAsState(
        targetValue = targetValue,
        animationSpec = tween(
            durationMillis = durationMillis,
            easing = easing
        )
    )
}
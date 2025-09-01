package com.example.myapplication.ui.motion

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * App-wide motion and visual tokens. Tuned for 60fps on mid-range devices.
 */
@Immutable
object MotionTokens {
    // Durations (ms)
    const val DurationSmall: Int = 120
    const val DurationMedium: Int = 240
    const val DurationLarge: Int = 360
    const val StaggerBase: Int = 40

    // Springs
    const val SpringStiffnessMedium: Float = 800f
    const val SpringDampingMedium: Float = 0.8f
    const val SpringStiffnessSnappy: Float = 1400f
    const val SpringDampingPlayful: Float = 0.7f

    val SpringStandard = spring<Float>(
        stiffness = SpringStiffnessMedium,
        dampingRatio = SpringDampingMedium
    )
    val SpringSnappy = spring<Float>(
        stiffness = SpringStiffnessSnappy,
        dampingRatio = SpringDampingPlayful
    )

    // Easings
    val EasingStandard: Easing = FastOutSlowInEasing
    val EasingEmphasized: Easing = LinearOutSlowInEasing
    val EasingDecelerate: Easing = FastOutLinearInEasing
    val EasingLinear: Easing = LinearEasing

    // Elevation scale
    val ElevationNone: Dp = 0.dp
    val ElevationLow: Dp = 1.dp
    val ElevationCard: Dp = 4.dp
    val ElevationRaised: Dp = 8.dp
    val ElevationFloating: Dp = 16.dp

    // Blur radii
    const val BlurRadiusHighApi: Float = 12f // API 31+
    const val BlurFallbackOverlayAlpha: Float = 0.06f // < 31

    // Spacing scale
    val Space2: Dp = 2.dp
    val Space4: Dp = 4.dp
    val Space8: Dp = 8.dp
    val Space12: Dp = 12.dp
    val Space16: Dp = 16.dp
    val Space20: Dp = 20.dp
    val Space24: Dp = 24.dp
    val Space32: Dp = 32.dp

    // Shadow/Glow tokens
    val GlowPrimary = Color(0x33FFFFFF)
    val ShadowSoft = Color(0x14000000)
    val ShadowStrong = Color(0x28000000)

    // Glass colors (overlay tints)
    val GlassLight = Color(0x26FFFFFF)
    val GlassDark = Color(0x1A000000)
    val GlassStroke = Color(0x33FFFFFF)

    // Brand/Palette tokens (keep in sync with DesignTokens.md)
    val ColorPrimary = Color(0xFF5E8BFF)
    val ColorOnPrimary = Color(0xFFFFFFFF)
    val ColorSecondary = Color(0xFF00D1B2)
    val ColorBackground = Color(0xFF0E0F12)
    val ColorSurface = Color(0xFF15171C)
    val ColorOnSurface = Color(0xFFE6E8EE)
}

/**
 * Convenience namespace for consumers.
 */
object Motion {
    object Durations {
        const val Small = MotionTokens.DurationSmall
        const val Medium = MotionTokens.DurationMedium
        const val Large = MotionTokens.DurationLarge
        const val Stagger = MotionTokens.StaggerBase
    }

    object Springs {
        val Standard = MotionTokens.SpringStandard
        val Snappy = MotionTokens.SpringSnappy
        val MediumSpec = spring<Float>(
            stiffness = MotionTokens.SpringStiffnessMedium,
            dampingRatio = MotionTokens.SpringDampingMedium
        )
        val SnappySpec = spring<Float>(
            stiffness = MotionTokens.SpringStiffnessSnappy,
            dampingRatio = MotionTokens.SpringDampingPlayful
        )
    }

    object Easings {
        val Standard = MotionTokens.EasingStandard
        val Heavy = MotionTokens.EasingEmphasized
        val Light = MotionTokens.EasingDecelerate
        val Linear = MotionTokens.EasingLinear
    }

    object Elevation {
        val None = MotionTokens.ElevationNone
        val Low = MotionTokens.ElevationLow
        val Card = MotionTokens.ElevationCard
        val Raised = MotionTokens.ElevationRaised
        val Floating = MotionTokens.ElevationFloating
    }
}


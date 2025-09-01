package com.example.myapplication.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Pixel-perfect design tokens from specification
 * Base canvas: 1080×2340 px (360dp × 780dp at xhdpi)
 * 8-point spacing system
 */
object DesignTokens {
    
    // === SPACING SCALE (8-point grid) ===
    object Spacing {
        val xxs = 4.dp
        val xs = 8.dp
        val sm = 12.dp
        val md = 16.dp
        val lg = 24.dp
        val xl = 32.dp
        val xxl = 48.dp
        val xxxl = 64.dp
    }
    
    // === SAFE AREA INSETS ===
    object SafeArea {
        val statusBarHeight = 24.dp  // Top safe inset (72px)
        val navBarHeight = 48.dp     // Bottom safe inset (144px)
        val navBarExtraPadding = 20.dp // Extra padding above controls for gesture areas
    }
    
    // === CAMERA PREVIEW ===
    object CameraPreview {
        const val heightPercent = 0.70f  // 70% of vertical height
        val topMargin = SafeArea.statusBarHeight + 12.dp
        val horizontalMargin = 16.dp
        val cornerRadius = 20.dp
        val innerPadding = 8.dp
        val shadowY = 12.dp
        val shadowBlur = 28.dp
        const val shadowAlpha = 0.12f
    }
    
    // === PRIMARY GLASS CONTROL BAR (BOTTOM PILL) ===
    object GlassPill {
        val height = 112.dp  // (336px)
        val horizontalMargin = 16.dp
        val bottomMargin = SafeArea.navBarHeight + 12.dp
        val cornerRadius = 999.dp  // Fully rounded
        val innerPadding = 12.dp
        val shadowY = 8.dp
        val shadowBlur = 20.dp
        const val shadowAlpha = 0.10f
        const val backgroundAlpha = 0.08f  // 8-12% range
        const val blurRadius = 12f  // For RenderEffect on API 31+
    }
    
    // === CAPTURE BUTTON (PRIMARY CTA) ===
    object CaptureButton {
        val outerDiameter = 84.dp  // (252px)
        val touchTargetMin = 48.dp
        val strokeWidth = 3.dp
        val innerShadow = 2.dp
        val iconSize = 24.dp
        
        // Animation values
        const val pulseScaleMin = 1.00f
        const val pulseScaleMax = 1.04f
        const val pulseDuration = 1200  // ms
        const val pressScale = 0.92f
        const val pressDuration = 80  // ms
        val burstDiameter = 140.dp
        const val burstDuration = 350  // ms
        val spinnerSize = 22.dp
        
        // Gradient colors
        val accentStart = Color(0xFFFF7A59)
        val accentEnd = Color(0xFFFFB36B)
    }
    
    // === GALLERY THUMBNAIL ===
    object GalleryThumbnail {
        val size = 56.dp  // (168px)
        val cornerRadius = 12.dp
        const val borderAlpha = 0.06f
        const val animScaleFrom = 0.9f
        const val animScaleTo = 1.0f
        const val animDuration = 180  // ms
        const val springStiffness = 900f
        const val springDamping = 0.8f
    }
    
    // === MODE TOGGLES & SETTINGS ===
    object ModeToggle {
        val width = 44.dp
        val height = 30.dp
        val cornerRadius = 15.dp
        const val backgroundAlpha = 0.10f
        val fontSize = 12.sp
        const val crossfadeDuration = 180  // ms
    }
    
    object SettingsIcon {
        val size = 36.dp
        val padding = 6.dp
        val modalHeight = 280.dp
    }
    
    // === FILTER CAROUSEL ===
    object FilterCarousel {
        val height = 96.dp
        val cardSize = 84.dp
        val cardCornerRadius = 12.dp
        val horizontalGutter = 16.dp
        val cardSpacing = 12.dp
        val overlapAbovePill = 10.dp
        
        // Scale & animation
        const val centerScale = 1.00f
        const val neighborScale = 0.86f
        val shadowY = 6.dp
        val shadowBlur = 16.dp
        const val shadowAlpha = 0.08f
        const val parallaxOffset = 8f  // px
        const val pressScale = 0.96f
        const val pressDuration = 90  // ms
    }
    
    // === TOP BAR ===
    object TopBar {
        val height = 56.dp
        val horizontalMargin = 16.dp
        val topMargin = SafeArea.statusBarHeight + 8.dp
        val fontSize = 14.sp
        const val backgroundAlpha = 0.06f
    }
    
    // === OVERLAY CONTROLS ===
    object FocusReticle {
        val diameter = 64.dp  // (192px)
        val strokeWidth = 2.dp
        const val scaleFrom = 0.8f
        const val scaleTo = 1.0f
        const val animDuration = 220  // ms
        const val fadeToAlpha = 0.6f
        const val fadeDuration = 600  // ms
    }
    
    object RuleOfThirds {
        const val strokeWidth = 1f  // px
        const val strokeAlpha = 0.10f
        const val animDuration = 180  // ms
    }
    
    object ExposureReadout {
        val height = 28.dp
        val horizontalPadding = 8.dp
        const val backgroundAlpha = 0.08f
        val fontSize = 12.sp
    }
    
    // === CAPTURE REVIEW ===
    object CaptureReview {
        val imageMargin = 16.dp
        val undoPillWidth = 44.dp
        val undoPillHeight = 36.dp
        val reeditButtonDiameter = 64.dp
    }
    
    // === COMPARE SLIDER ===
    object CompareSlider {
        val handleDiameter = 28.dp
        val handleStrokeWidth = 4.dp
        const val snapLeft = 0.10f
        const val snapCenter = 0.50f
        const val snapRight = 0.90f
    }
    
    // === MASK PAINTER ===
    object MaskPainter {
        val toolbarWidth = 56.dp
        val toolButtonSize = 44.dp
        val toolSpacing = 8.dp
        val brushSizeMin = 8.dp
        val brushSizeMax = 48.dp
        const val sampleRate = 60  // Hz
        const val maxUndoStack = 30
    }
    
    // === HERO TRANSITION ===
    object HeroTransition {
        const val duration = 420  // ms
        val easing = CubicBezierEasing(0.2f, 0.9f, 0.2f, 1.0f)
        val elevationFrom = 12.dp
        val elevationTo = 28.dp
        val cornerRadiusFrom = 12.dp
        val cornerRadiusTo = 6.dp
        const val fallbackDuration = 240  // ms
    }
    
    // === SNACKBAR ===
    object Snackbar {
        val maxWidth = 600.dp
        val height = 56.dp
        val horizontalMargin = 16.dp
        val slideOffset = 20.dp
        const val slideDuration = 300  // ms
        const val maxMessages = 2
        const val maxChars = 50
        val fontSize = 14.sp
    }
    
    // === COLORS ===
    object Colors {
        val background = Color(0xFF0F1115)
        val glass = Color(0xFFFFFFFF)
        val accentStart = Color(0xFFFF7A59)
        val accentEnd = Color(0xFFFFB36B)
        val textPrimary = Color(0xFFFFFFFF)
        val textSecondary = Color(0xB3FFFFFF)  // 70% alpha
        val borderSubtle = Color(0x0FFFFFFF)   // 6% alpha
    }
    
    // === MOTION ===
    object Motion {
        const val durationSmall = 120  // ms
        const val durationMedium = 240  // ms  
        const val durationLarge = 420  // ms (hero)
        
        val springConfig = spring<Float>(
            stiffness = 1200f,
            dampingRatio = 0.78f
        )
        
        val springSnappy = spring<Float>(
            stiffness = Spring.StiffnessMedium,
            dampingRatio = Spring.DampingRatioMediumBouncy
        )
        
        val easingStandard = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
        val easingEmphasized = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
        val easingOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)
    }
    
    // === RESPONSIVE BREAKPOINTS ===
    object Responsive {
        const val baselineWidth = 360  // dp
        const val largeWidth = 440  // dp
        const val smallWidth = 360  // dp
        
        // Large screen adjustments
        val captureButtonLarge = 96.dp
        val previewCornerRadiusLarge = 24.dp
        val pillHeightLarge = 128.dp
        
        // Small screen adjustments
        val captureButtonSmall = 72.dp
        val filterCardSmall = 72.dp
    }
    
    // === ACCESSIBILITY ===
    object Accessibility {
        val minTouchTarget = 48.dp
        const val maxFontScale = 1.4f
        const val labelReserveWidth = 1.2f  // 20% extra for localization
    }
    
    // === PERFORMANCE THRESHOLDS ===
    object Performance {
        const val lowPerfApiLevel = 23
        const val blurApiLevel = 31
        const val reducedShadowFactor = 0.5f
        const val maxCaptureLatency = 900  // ms
    }
}

package com.example.myapplication.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Design tokens for the clean, minimalistic camera UI
 * Following the pixel-perfect specifications from the design prompt
 */
object CameraDesignTokens {
    
    // Spacing scale (8-point system)
    object Spacing {
        val xs = 4.dp
        val s = 8.dp
        val m = 12.dp
        val l = 16.dp
        val xl = 24.dp
        val xxl = 32.dp
        val xxxl = 48.dp
        val xxxxl = 64.dp
    }
    
    // Corner radius tokens
    object CornerRadius {
        val small = 8.dp
        val medium = 12.dp
        val large = 16.dp
        val xlarge = 20.dp
        val pill = 999.dp
    }
    
    // Color tokens
    object Colors {
        val background = Color(0xFF0F1115)
        val glass = Color(0x14FFFFFF) // White at 8% alpha
        val glassSubtle = Color(0x0AFFFFFF) // White at 4% alpha
        val glassBorder = Color(0x0FFFFFFF) // White at 6% alpha
        val accentStart = Color(0xFFFF7A59)
        val accentEnd = Color(0xFFFFB36B)
        val textPrimary = Color.White
        val textSecondary = Color(0xB3FFFFFF) // White at 70% alpha
        val overlay = Color(0x1AFFFFFF) // White at 10% alpha for grid lines
        val focusReticle = Color.White
        val shadowColor = Color(0x1F000000) // Black at 12% alpha
    }
    
    // Motion/Animation durations (in milliseconds)
    object Motion {
        const val instant = 80
        const val small = 120
        const val medium = 180
        const val large = 240
        const val hero = 420
        const val slow = 600
        const val pulse = 1200
        
        // Spring animation parameters
        object Spring {
            const val stiffness = 900f
            const val damping = 0.8f
            const val stiffnessHigh = 1200f
            const val dampingLow = 0.78f
        }
    }
    
    // Component-specific dimensions
    object Dimensions {
        // Safe areas
        val statusBarHeight = 24.dp
        val navBarHeight = 48.dp
        val navBarExtraPadding = 20.dp
        
        // Camera preview
        val previewCornerRadius = 20.dp
        val previewInnerPadding = 8.dp
        val previewHorizontalMargin = 16.dp
        val previewTopMargin = 12.dp
        
        // Glass pill control bar
        val glassPillHeight = 112.dp
        val glassPillHorizontalMargin = 16.dp
        val glassPillBottomMargin = 12.dp
        val glassPillInnerPadding = 12.dp
        
        // Capture button
        val captureButtonOuter = 84.dp
        val captureButtonTouchTarget = 48.dp
        val captureButtonStrokeWidth = 3.dp
        val captureButtonIconSize = 24.dp
        val captureButtonPressedScale = 0.92f
        val captureButtonPulseScale = 1.04f
        val captureButtonBurstExpansion = 140.dp
        
        // Gallery thumbnail
        val galleryThumbnailSize = 56.dp
        val galleryThumbnailCornerRadius = 12.dp
        val galleryThumbnailBorder = 1.dp
        
        // Mode toggle
        val modeToggleWidth = 44.dp
        val modeToggleHeight = 30.dp
        val modeToggleCornerRadius = 15.dp
        
        // Settings gear
        val settingsIconSize = 36.dp
        val settingsIconPadding = 6.dp
        
        // Filter carousel
        val filterCarouselHeight = 96.dp
        val filterCardSize = 84.dp
        val filterCardCornerRadius = 12.dp
        val filterCardSpacing = 12.dp
        val filterCardCenterScale = 1.00f
        val filterCardNeighborScale = 0.86f
        val filterCarouselOverlap = 10.dp
        
        // Focus reticle
        val focusReticleDiameter = 64.dp
        val focusReticleStrokeWidth = 2.dp
        val focusReticleInitialScale = 0.8f
        
        // Exposure readout
        val exposureReadoutHeight = 28.dp
        val exposureReadoutPaddingHorizontal = 8.dp
        
        // Top bar
        val topBarHeight = 56.dp
        val topBarMargin = 16.dp
        val topBarTopOffset = 8.dp
        
        // Review screen
        val reviewButtonDiameter = 64.dp
        val reviewUndoPillWidth = 44.dp
        val reviewUndoPillHeight = 36.dp
        
        // Compare slider handle
        val compareHandleSize = 28.dp
        val compareHandleStrokeWidth = 4.dp
        
        // Mask painter
        val maskPainterToolbarWidth = 56.dp
        val maskPainterToolButtonSize = 44.dp
        val maskPainterToolSpacing = 8.dp
        val brushSizeMin = 8.dp
        val brushSizeMax = 48.dp
        
        // Snackbar
        val snackbarHeight = 56.dp
        val snackbarMaxWidth = 600.dp
        val snackbarMargin = 32.dp
    }
    
    // Shadow specifications
    object Shadows {
        data class ShadowSpec(
            val offsetY: Dp,
            val blurRadius: Dp,
            val alpha: Float
        )
        
        val preview = ShadowSpec(offsetY = 12.dp, blurRadius = 28.dp, alpha = 0.12f)
        val glassPill = ShadowSpec(offsetY = 8.dp, blurRadius = 20.dp, alpha = 0.10f)
        val filterCard = ShadowSpec(offsetY = 6.dp, blurRadius = 16.dp, alpha = 0.08f)
        val elevated = ShadowSpec(offsetY = 12.dp, blurRadius = 28.dp, alpha = 0.15f)
    }
    
    // Responsive breakpoints
    object Breakpoints {
        val compact = 360.dp
        val medium = 440.dp
        
        // Scaling factors for different screen sizes
        fun getCaptureButtonSize(screenWidth: Dp): Dp {
            return when {
                screenWidth >= medium -> 96.dp
                screenWidth < compact -> 72.dp
                else -> 84.dp
            }
        }
        
        fun getPreviewCornerRadius(screenWidth: Dp): Dp {
            return when {
                screenWidth >= medium -> 24.dp
                else -> 20.dp
            }
        }
        
        fun getGlassPillHeight(screenWidth: Dp): Dp {
            return when {
                screenWidth >= medium -> 128.dp
                else -> 112.dp
            }
        }
    }
    
    // Typography specifications
    object Typography {
        val modeToggleFontSize = 12 // sp
        val exposureReadoutFontSize = 12 // sp
        val topBarTitleFontSize = 14 // sp
        val snackbarFontSize = 14 // sp
    }
}

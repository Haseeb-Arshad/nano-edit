package com.example.myapplication.ui.camera

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.myapplication.ui.theme.CameraDesignTokens
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize

/**
 * Clean, minimalistic camera preview component
 * Implements the hero preview with proper dimensions and safe area handling
 */
@Composable
fun CleanCameraPreview(
    previewView: PreviewView,
    modifier: Modifier = Modifier,
    onTap: (Float, Float) -> Unit = { _, _ -> },
    overlayContent: @Composable BoxScope.() -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    
    // Calculate preview dimensions (70% of vertical height)
    val safeHeight = screenHeight - CameraDesignTokens.Dimensions.statusBarHeight - CameraDesignTokens.Dimensions.navBarHeight
    val previewHeight = safeHeight * 0.70f
    val previewWidth = screenWidth - (CameraDesignTokens.Dimensions.previewHorizontalMargin * 2)
    
    // Get responsive corner radius
    val cornerRadius = CameraDesignTokens.Breakpoints.getPreviewCornerRadius(screenWidth)
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                top = CameraDesignTokens.Dimensions.statusBarHeight + CameraDesignTokens.Dimensions.previewTopMargin,
                start = CameraDesignTokens.Dimensions.previewHorizontalMargin,
                end = CameraDesignTokens.Dimensions.previewHorizontalMargin
            )
    ) {
        // Preview container with shadow
        Box(
            modifier = Modifier
                .size(width = previewWidth, height = previewHeight)
                .shadow(
                    elevation = CameraDesignTokens.Shadows.preview.blurRadius,
                    shape = RoundedCornerShape(cornerRadius),
                    clip = false
                )
                .clip(RoundedCornerShape(cornerRadius))
                .background(Color.Black)
        ) {
            // Camera preview
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
            
            // Inner content safe area for overlays
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(CameraDesignTokens.Dimensions.previewInnerPadding)
            ) {
                overlayContent()
            }
        }
    }
}

/**
 * Rule of thirds grid overlay
 */
@Composable
fun RuleOfThirdsGrid(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 0.1f else 0f,
        animationSpec = tween(
            durationMillis = CameraDesignTokens.Motion.medium,
            easing = FastOutSlowInEasing
        ),
        label = "grid_alpha"
    )
    
    if (alpha > 0f) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .graphicsLayer { this.alpha = alpha }
        ) {
            // Vertical lines
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .align(Alignment.CenterStart)
                    .offset(x = with(LocalDensity.current) { 
                        (LocalConfiguration.current.screenWidthDp.dp * 0.333f) 
                    })
                    .background(CameraDesignTokens.Colors.overlay)
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .align(Alignment.CenterStart)
                    .offset(x = with(LocalDensity.current) { 
                        (LocalConfiguration.current.screenWidthDp.dp * 0.666f) 
                    })
                    .background(CameraDesignTokens.Colors.overlay)
            )
            
            // Horizontal lines
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = with(LocalDensity.current) { 
                        (LocalConfiguration.current.screenHeightDp.dp * 0.333f) 
                    })
                    .background(CameraDesignTokens.Colors.overlay)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = with(LocalDensity.current) { 
                        (LocalConfiguration.current.screenHeightDp.dp * 0.666f) 
                    })
                    .background(CameraDesignTokens.Colors.overlay)
            )
        }
    }
}

/**
 * Focus reticle animation
 */
@Composable
fun FocusReticle(
    position: Offset? = null,
    modifier: Modifier = Modifier
) {
    position?.let { pos ->
        var isAnimating by remember { mutableStateOf(true) }
        
        val scale by animateFloatAsState(
            targetValue = if (isAnimating) 1.0f else 1.0f,
            animationSpec = spring(
                dampingRatio = 0.6f,
                stiffness = CameraDesignTokens.Motion.Spring.stiffness
            ),
            label = "reticle_scale"
        )
        
        val alpha by animateFloatAsState(
            targetValue = if (isAnimating) 1.0f else 0.6f,
            animationSpec = tween(
                durationMillis = CameraDesignTokens.Motion.slow,
                easing = FastOutSlowInEasing
            ),
            finishedListener = { isAnimating = false },
            label = "reticle_alpha"
        )
        
        LaunchedEffect(pos) {
            isAnimating = true
        }
        
        Box(
            modifier = modifier
                .offset(x = pos.x.dp, y = pos.y.dp)
                .size(CameraDesignTokens.Dimensions.focusReticleDiameter)
                .graphicsLayer {
                    scaleX = scale * CameraDesignTokens.Dimensions.focusReticleInitialScale
                    scaleY = scale * CameraDesignTokens.Dimensions.focusReticleInitialScale
                    this.alpha = alpha
                }
                .border(
                    width = CameraDesignTokens.Dimensions.focusReticleStrokeWidth,
                    color = CameraDesignTokens.Colors.focusReticle.copy(alpha = alpha),
                    shape = RoundedCornerShape(50)
                )
        )
    }
}

/**
 * Exposure/Focus readout pill
 */
@Composable
fun ExposureReadout(
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(CameraDesignTokens.CornerRadius.pill),
        color = CameraDesignTokens.Colors.glass
    ) {
        androidx.compose.material3.Text(
            text = value,
            modifier = Modifier.padding(
                horizontal = CameraDesignTokens.Dimensions.exposureReadoutPaddingHorizontal,
                vertical = 4.dp
            ),
            fontSize = CameraDesignTokens.Typography.exposureReadoutFontSize.sp,
            color = CameraDesignTokens.Colors.textPrimary
        )
    }
}

// Helper data class for offset
data class Offset(val x: Float, val y: Float)

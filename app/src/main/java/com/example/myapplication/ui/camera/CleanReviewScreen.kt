package com.example.myapplication.ui.camera

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.CameraDesignTokens
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Clean review screen with before/after slider and action buttons
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanReviewScreen(
    originalBitmap: Bitmap,
    enhancedBitmap: Bitmap,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sliderPosition by remember { mutableStateOf(0.5f) }
    var showEditControls by remember { mutableStateOf(true) }
    val haptics = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val scope = rememberCoroutineScope()
    
    // Snap to common positions with haptic feedback
    fun snapToPosition(position: Float) {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        sliderPosition = position
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CameraDesignTokens.Colors.background)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { showEditControls = !showEditControls }
                )
            }
    ) {
        // Main image with compare slider
        CompareSlider(
            originalImage = {
                Image(
                    bitmap = originalBitmap.asImageBitmap(),
                    contentDescription = "Original image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            },
            enhancedImage = {
                Image(
                    bitmap = enhancedBitmap.asImageBitmap(),
                    contentDescription = "Enhanced image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            },
            sliderPosition = sliderPosition,
            onSliderPositionChange = { position ->
                sliderPosition = position
                
                // Snap to positions with haptic feedback
                when {
                    position in 0.08f..0.12f -> scope.launch { snapToPosition(0.1f) }
                    position in 0.48f..0.52f -> scope.launch { snapToPosition(0.5f) }
                    position in 0.88f..0.92f -> scope.launch { snapToPosition(0.9f) }
                }
            },
            handleContent = {
                CleanSliderHandle(
                    percentage = (sliderPosition * 100).roundToInt()
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = CameraDesignTokens.Dimensions.statusBarHeight,
                    bottom = CameraDesignTokens.Dimensions.navBarHeight
                )
        )
        
        // Overlay controls with animations
        AnimatedVisibility(
            visible = showEditControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top bar
                CleanTopControls(
                    onBack = onBack,
                    onShare = onShare,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(horizontal = CameraDesignTokens.Spacing.l)
                )
                
                // Bottom actions
                CleanBottomControls(
                    onSave = onSave,
                    onEdit = onEdit,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(
                            horizontal = CameraDesignTokens.Spacing.l,
                            vertical = CameraDesignTokens.Spacing.l
                        )
                )
            }
        }
    }
}

/**
 * Clean image compare slider
 */
@Composable
private fun CompareSlider(
    originalImage: @Composable BoxScope.() -> Unit,
    enhancedImage: @Composable BoxScope.() -> Unit,
    sliderPosition: Float,
    onSliderPositionChange: (Float) -> Unit,
    handleContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val maxWidth = constraints.maxWidth.toFloat()
        
        Box(modifier = Modifier.fillMaxSize()) {
            // Enhanced image (full)
            Box(modifier = Modifier.fillMaxSize()) {
                enhancedImage()
            }
            
            // Original image (clipped)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        val clipWidth = size.width * sliderPosition
                        clipRect(left = 0f, top = 0f, right = clipWidth, bottom = size.height) {
                            this@drawWithContent.drawContent()
                        }
                    }
            ) {
                originalImage()
            }
            
            // Draggable slider handle
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (maxWidth * sliderPosition).roundToInt() - 
                                CameraDesignTokens.Dimensions.compareHandleSize.roundToPx() / 2,
                            y = 0
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val newPosition = ((maxWidth * sliderPosition) + dragAmount.x)
                                .coerceIn(0f, maxWidth) / maxWidth
                            onSliderPositionChange(newPosition)
                        }
                    }
            ) {
                handleContent()
            }
        }
    }
}

/**
 * Clean slider handle UI
 */
@Composable
private fun CleanSliderHandle(
    percentage: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .wrapContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Vertical line
        Box(
            modifier = Modifier
                .width(CameraDesignTokens.Dimensions.compareHandleStrokeWidth)
                .fillMaxHeight()
                .background(
                    color = CameraDesignTokens.Colors.accentStart,
                    shape = RoundedCornerShape(50)
                )
        )
        
        // Handle with percentage
        Surface(
            modifier = Modifier
                .size(CameraDesignTokens.Dimensions.compareHandleSize)
                .shadow(
                    elevation = CameraDesignTokens.Shadows.preview.blurRadius,
                    shape = CircleShape,
                    ambientColor = CameraDesignTokens.Colors.accentStart,
                    spotColor = CameraDesignTokens.Colors.accentEnd
                ),
            shape = CircleShape,
            color = Color.White,
            border = BorderStroke(
                width = CameraDesignTokens.Dimensions.compareHandleStrokeWidth,
                brush = Brush.linearGradient(
                    colors = listOf(
                        CameraDesignTokens.Colors.accentStart,
                        CameraDesignTokens.Colors.accentEnd
                    )
                )
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$percentage%",
                    fontSize = 10.sp,
                    color = CameraDesignTokens.Colors.accentStart
                )
            }
        }
    }
}

/**
 * Clean top controls bar
 */
@Composable
private fun CleanTopControls(
    onBack: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(CameraDesignTokens.Dimensions.topBarHeight),
        color = CameraDesignTokens.Colors.glass,
        shape = RoundedCornerShape(CameraDesignTokens.CornerRadius.medium)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = CameraDesignTokens.Spacing.l),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.semantics {
                    contentDescription = "Back"
                }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = null,
                    tint = CameraDesignTokens.Colors.textPrimary
                )
            }
            
            Text(
                text = "Compare",
                fontSize = CameraDesignTokens.Typography.topBarTitleFontSize.sp,
                color = CameraDesignTokens.Colors.textPrimary
            )
            
            IconButton(
                onClick = onShare,
                modifier = Modifier.semantics {
                    contentDescription = "Share"
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = CameraDesignTokens.Colors.textPrimary
                )
            }
        }
    }
}

/**
 * Clean bottom action buttons
 */
@Composable
private fun CleanBottomControls(
    onSave: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        color = CameraDesignTokens.Colors.glass,
        shape = RoundedCornerShape(CameraDesignTokens.CornerRadius.pill)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = CameraDesignTokens.Spacing.l),
            horizontalArrangement = Arrangement.spacedBy(CameraDesignTokens.Spacing.l)
        ) {
            Button(
                onClick = onSave,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = CameraDesignTokens.Spacing.s),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CameraDesignTokens.Colors.accentStart
                ),
                shape = RoundedCornerShape(CameraDesignTokens.CornerRadius.pill)
            ) {
                Text("Save")
            }
            
            Button(
                onClick = onEdit,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = CameraDesignTokens.Spacing.s),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CameraDesignTokens.Colors.glass
                ),
                shape = RoundedCornerShape(CameraDesignTokens.CornerRadius.pill)
            ) {
                Text("Edit")
            }
        }
    }
}

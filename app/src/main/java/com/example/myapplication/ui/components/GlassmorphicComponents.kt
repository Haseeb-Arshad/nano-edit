package com.example.myapplication.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.DesignTokens

/**
 * Glassmorphic components following the master prompt design system
 */

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = DesignTokens.Radius.card,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .background(
                color = DesignTokens.Colors.Light.surface,
                shape = RoundedCornerShape(cornerRadius)
            )
            .clip(RoundedCornerShape(cornerRadius)),
        content = content
    )
}

@Composable
fun GlassmorphicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    cornerRadius: androidx.compose.ui.unit.Dp = DesignTokens.Radius.chip,
    content: @Composable BoxScope.() -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = tween(DesignTokens.Motion.chipApply)
    )
    
    Box(
        modifier = modifier
            .scale(scale)
            .background(
                color = if (enabled) DesignTokens.Colors.Light.surface else DesignTokens.Colors.Light.surface.copy(alpha = 0.3f),
                shape = RoundedCornerShape(cornerRadius)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            tryAwaitRelease()
                            isPressed = false
                        },
                        onTap = { onClick() }
                    )
                }
            },
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun ShutterButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCapturing: Boolean = false
) {
    val haptics = LocalHapticFeedback.current
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = tween(DesignTokens.Motion.shutterCompress),
        label = "shutterPressScale"
    )
    
    val ringScale by animateFloatAsState(
        targetValue = if (isCapturing) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(120),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Box(
        modifier = modifier
            .size(DesignTokens.Sizes.shutterButton)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        // Outer gradient ring
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(ringScale)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            DesignTokens.Colors.Light.accent,
                            DesignTokens.Colors.Light.accent.copy(alpha = 0.3f)
                        )
                    ),
                    shape = CircleShape
                )
        )
        
        // Inner button
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    color = Color.White,
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = interaction,
                    indication = null
                ) {
                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onClick()
                }
        )
    }
}

@Composable
fun ModeChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    
    GlassmorphicButton(
        onClick = {
            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = modifier.height(32.dp),
        cornerRadius = DesignTokens.Radius.pill
    ) {
        Text(
            text = text,
            color = if (isSelected) DesignTokens.Colors.Light.accent else DesignTokens.Colors.Light.textMid,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = DesignTokens.Spacing.lg, vertical = DesignTokens.Spacing.sm)
        )
    }
}

@Composable
fun SuggestionCard(
    title: String,
    description: String,
    thumbnailUrl: String? = null,
    onApply: () -> Unit,
    onOptions: () -> Unit,
    modifier: Modifier = Modifier,
    isApplied: Boolean = false,
    isLoading: Boolean = false
) {
    val haptics = LocalHapticFeedback.current
    
    GlassmorphicCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = DesignTokens.Radius.card
    ) {
        Column(
            modifier = Modifier.padding(DesignTokens.Spacing.lg)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = DesignTokens.Colors.Light.textHigh,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = description,
                        color = DesignTokens.Colors.Light.textMid,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = DesignTokens.Spacing.xs)
                    )
                }
                
                // Thumbnail placeholder
                Box(
                    modifier = Modifier
                        .size(DesignTokens.Sizes.suggestionCardThumb.first, DesignTokens.Sizes.suggestionCardThumb.second)
                        .background(
                            color = DesignTokens.Colors.Light.surface,
                            shape = RoundedCornerShape(DesignTokens.Radius.chip)
                        )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.Center),
                            color = DesignTokens.Colors.Light.accent,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(DesignTokens.Spacing.lg))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm)
            ) {
                GlassmorphicButton(
                    onClick = {
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onApply()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isApplied && !isLoading
                ) {
                    Text(
                        text = if (isApplied) "Applied" else "Apply",
                        color = if (isApplied) DesignTokens.Colors.Light.success else DesignTokens.Colors.Light.textHigh,
                        modifier = Modifier.padding(vertical = DesignTokens.Spacing.sm)
                    )
                }
                
                GlassmorphicButton(
                    onClick = {
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        onOptions()
                    },
                    modifier = Modifier.size(DesignTokens.Sizes.touchTarget)
                ) {
                    Text("⋯", color = DesignTokens.Colors.Light.textMid)
                }
            }
        }
    }
}

@Composable
fun AnimatedGradientBackground(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DesignTokens.Colors.Light.background,
                        DesignTokens.Colors.Light.background.copy(alpha = 0.8f),
                        DesignTokens.Colors.Light.background
                    ),
                    startY = offset * 1000f,
                    endY = (offset + 1f) * 1000f
                )
            )
    )
}

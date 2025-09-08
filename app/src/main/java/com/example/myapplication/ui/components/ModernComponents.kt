package com.example.myapplication.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.myapplication.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Glassmorphic card with blur effect
 */
@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    borderWidth: Dp = 1.dp,
    blurRadius: Dp = 10.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.1f),
                        Color.White.copy(alpha = 0.05f)
                    )
                )
            )
            .border(
                width = borderWidth,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.5f),
                        Color.White.copy(alpha = 0.2f)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
    ) {
        content()
    }
}

/**
 * Animated capture button like Snapchat
 */
@Composable
fun AnimatedCaptureButton(
    onClick: () -> Unit,
    isRecording: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(1f) }
    val rotation = remember { Animatable(0f) }
    val borderScale = remember { Animatable(1f) }
    
    LaunchedEffect(isRecording) {
        if (isRecording) {
            launch {
                scale.animateTo(
                    targetValue = 0.8f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            launch {
                rotation.animateTo(
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    )
                )
            }
        } else {
            launch { scale.animateTo(1f) }
            launch { rotation.snapTo(0f) }
        }
    }
    
    Box(
        modifier = modifier
            .size(80.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Outer animated ring
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .scale(borderScale.value)
                .rotate(rotation.value)
        ) {
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        NeonPink,
                        NeonBlue,
                        NeonPurple,
                        NeonYellow,
                        NeonPink
                    )
                ),
                radius = size.minDimension / 2,
                style = Stroke(width = 4.dp.toPx())
            )
        }
        
        // Inner button
        Box(
            modifier = Modifier
                .size(64.dp)
                .scale(scale.value)
                .clip(CircleShape)
                .background(
                    brush = if (isRecording) {
                        Brush.radialGradient(
                            colors = listOf(AccentRed, AccentRed.copy(alpha = 0.7f))
                        )
                    } else {
                        Brush.radialGradient(
                            colors = listOf(Color.White, Color.White.copy(alpha = 0.9f))
                        )
                    }
                )
                .border(
                    width = 2.dp,
                    color = Color.White.copy(alpha = 0.3f),
                    shape = CircleShape
                )
        )
    }
}

/**
 * Filter carousel item with preview
 */
@Composable
fun FilterCarouselItem(
    filterName: String,
    filterPreview: @Composable () -> Unit,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    Column(
        modifier = modifier
            .scale(scale.value)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    brush = if (isSelected) {
                        Brush.linearGradient(
                            colors = listOf(NeonPink, NeonBlue)
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.3f),
                                Color.White.copy(alpha = 0.1f)
                            )
                        )
                    },
                    shape = CircleShape
                )
        ) {
            filterPreview()
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = filterName,
            fontSize = 11.sp,
            color = if (isSelected) NeonPink else Color.White,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * Neon gradient button
 */
@Composable
fun NeonButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    gradient: List<Color> = listOf(NeonPink, NeonPurple)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale = animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    
    Box(
        modifier = modifier
            .scale(scale.value)
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.linearGradient(colors = gradient)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            icon?.invoke()
            if (icon != null) Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * Story ring component
 */
@Composable
fun StoryRing(
    modifier: Modifier = Modifier,
    hasStory: Boolean = true,
    isViewed: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = if (hasStory && !isViewed) {
        listOf(StoryRing1, StoryRing2, StoryRing3)
    } else if (hasStory && isViewed) {
        listOf(Color.Gray.copy(alpha = 0.5f), Color.Gray.copy(alpha = 0.3f))
    } else {
        listOf(Color.Transparent, Color.Transparent)
    }
    
    Box(
        modifier = modifier
            .border(
                width = 3.dp,
                brush = Brush.linearGradient(colors = colors),
                shape = CircleShape
            )
            .padding(4.dp)
    ) {
        content()
    }
}

/**
 * Beauty mode slider
 */
@Composable
fun BeautySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
            Text(
                text = "${(value * 100).toInt()}%",
                fontSize = 12.sp,
                color = NeonPink
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = NeonPink,
                activeTrackColor = NeonPink,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
    }
}

/**
 * Floating action menu
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun FloatingActionMenu(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    items: List<FloatingMenuItem>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Menu items
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                items.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (item.label != null) {
                            GlassmorphicCard(
                                modifier = Modifier.padding(end = 8.dp),
                                cornerRadius = 16.dp
                            ) {
                                Text(
                                    text = item.label,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }
                        }
                        
                        FloatingActionButton(
                            onClick = item.onClick,
                            modifier = Modifier.size(48.dp),
                            containerColor = item.backgroundColor,
                            contentColor = Color.White
                        ) {
                            item.icon()
                        }
                    }
                }
            }
        }
        
        // Main FAB
        FloatingActionButton(
            onClick = onToggle,
            modifier = Modifier.size(56.dp),
            containerColor = NeonPink
        ) {
            AnimatedContent(
                targetState = isExpanded,
                transitionSpec = {
                    fadeIn() + scaleIn() with fadeOut() + scaleOut()
                }
            ) { expanded ->
                if (expanded) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                } else {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Add,
                        contentDescription = "Menu",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

data class FloatingMenuItem(
    val icon: @Composable () -> Unit,
    val onClick: () -> Unit,
    val label: String? = null,
    val backgroundColor: Color = NeonBlue
)



/**
 * Animated gradient background
 */
@Composable
fun AnimatedGradientBackground(
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(
        Color.Black,
        Color.Black
    )
) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Black,
                        Color.Black.copy(alpha = 0.95f)
                    )
                )
            )
    )
}

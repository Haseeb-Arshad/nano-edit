package com.example.myapplication.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.myapplication.ui.theme.DesignTokens
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.abs


/**
 * Precise filter carousel matching exact design specifications
 * Card size: 84dp x 84dp with parallax and scale effects
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PreciseFilterCarousel(
    modifier: Modifier = Modifier,
    imageModel: Any? = null,
    presets: List<FilterPreset>,
    selectedPresetId: String? = null,
    onPresetSelected: (FilterPreset) -> Unit = {}
) {
    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(listState)
    val haptics = LocalHapticFeedback.current
    
    LazyRow(
        state = listState,
        flingBehavior = flingBehavior,
        modifier = modifier
            .fillMaxWidth()
            .height(DesignTokens.FilterCarousel.height),
        contentPadding = PaddingValues(
            horizontal = DesignTokens.FilterCarousel.horizontalGutter
        ),
        horizontalArrangement = Arrangement.spacedBy(DesignTokens.FilterCarousel.cardSpacing)
    ) {
        items(
            count = presets.size,
            key = { index -> presets[index].id }
        ) { index ->
            FilterCard(
                preset = presets[index],
                imageModel = imageModel,
                isSelected = presets[index].id == selectedPresetId,
                listState = listState,
                index = index,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onPresetSelected(presets[index])
                }
            )
        }
    }
}

@Composable
private fun FilterCard(
    preset: FilterPreset,
    imageModel: Any?,
    isSelected: Boolean,
    listState: LazyListState,
    index: Int,
    onClick: () -> Unit
) {
    val density = LocalDensity.current
    
    // Calculate position relative to center for scale and parallax
    val layoutInfo = listState.layoutInfo
    val viewportCenter = layoutInfo.viewportEndOffset / 2f
    val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == index }
    
    val distanceFromCenter = if (itemInfo != null) {
        val itemCenter = itemInfo.offset + itemInfo.size / 2f
        (itemCenter - viewportCenter) / viewportCenter
    } else {
        1f
    }
    
    val normalizedDistance = distanceFromCenter.coerceIn(-1f, 1f)
    val absDistance = abs(normalizedDistance)
    
    // Animated scale based on distance from center
    val targetScale = DesignTokens.FilterCarousel.centerScale - 
        (DesignTokens.FilterCarousel.centerScale - DesignTokens.FilterCarousel.neighborScale) * absDistance
    
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            stiffness = Spring.StiffnessMedium,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "cardScale"
    )
    
    // Animated shadow based on distance
    val shadowElevation by animateFloatAsState(
        targetValue = (1f - absDistance * 0.6f) * with(density) { DesignTokens.FilterCarousel.shadowY.toPx() },
        animationSpec = spring(
            stiffness = Spring.StiffnessLow,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "shadowElevation"
    )
    
    // Press animation
    var isPressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) DesignTokens.FilterCarousel.pressScale else 1f,
        animationSpec = tween(
            durationMillis = DesignTokens.FilterCarousel.pressDuration,
            easing = FastOutSlowInEasing
        ),
        label = "pressScale"
    )
    
    // Parallax offset for image
    val parallaxOffset = normalizedDistance * DesignTokens.FilterCarousel.parallaxOffset
    
    Box(
        modifier = Modifier
            .size(DesignTokens.FilterCarousel.cardSize)
            .scale(scale * pressScale)
            .graphicsLayer {
                this.shadowElevation = shadowElevation
            }
            .clip(RoundedCornerShape(DesignTokens.FilterCarousel.cardCornerRadius))
            .clickable(
                onClick = {
                    isPressed = true
                    onClick()
                    // Reset press state after animation
                    kotlinx.coroutines.GlobalScope.launch {
                        kotlinx.coroutines.delay(DesignTokens.FilterCarousel.pressDuration.toLong())
                        isPressed = false
                    }
                }
            )
            .semantics {
                contentDescription = "Filter: ${preset.name}"
            },
        contentAlignment = Alignment.Center
    ) {
        // Background image with parallax
        if (imageModel != null) {
            AsyncImage(
                model = imageModel,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = parallaxOffset
                    },
                contentScale = ContentScale.Crop
            )
        } else {
            // Placeholder gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF3A3A3A),
                                Color(0xFF2A2A2A)
                            )
                        )
                    )
            )
        }
        
        // Filter tint overlay
        if (preset.tint != Color.Unspecified) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(preset.tint.copy(alpha = 0.4f))
            )
        }
        
        // Label at bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0f),
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
                .padding(vertical = 6.dp)
        ) {
            Text(
                text = preset.name,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Selection indicator
        if (isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(DesignTokens.FilterCarousel.cardCornerRadius))
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                DesignTokens.CaptureButton.accentStart.copy(alpha = 0.9f),
                                DesignTokens.CaptureButton.accentEnd.copy(alpha = 0.9f)
                            )
                        ),
                        shape = RoundedCornerShape(DesignTokens.FilterCarousel.cardCornerRadius)
                    )
            )
        }
    }
}

// Default filter presets
val defaultFilterPresets = listOf(
    FilterPreset("original", "Original"),
    FilterPreset("vivid", "Vivid", Color(0xFF56CCF2)),
    FilterPreset("warm", "Warm", Color(0xFFF2B56B)),
    FilterPreset("cool", "Cool", Color(0xFF48C9B0)),
    FilterPreset("mono", "B&W", Color(0xFF222222)),
    FilterPreset("cinema", "Cinema", Color(0xFF4E342E)),
    FilterPreset("retro", "Retro", Color(0xFFE67E22)),
    FilterPreset("dreamy", "Dreamy", Color(0xFFBB8FCE))
)

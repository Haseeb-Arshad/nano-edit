package com.example.myapplication.ui.camera

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.myapplication.ui.theme.CameraDesignTokens
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/**
 * Filter carousel with parallax effects floating above the pill
 */
@Composable
fun CleanFilterCarousel(
    filters: List<FilterPreset>,
    selectedIndex: Int = 0,
    onFilterSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedIndex
    )
    
    // Calculate center position for scaling
    val centerIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = layoutInfo.viewportEndOffset / 2
            
            layoutInfo.visibleItemsInfo.minByOrNull { item ->
                val itemCenter = item.offset + item.size / 2
                kotlin.math.abs(itemCenter - viewportCenter)
            }?.index ?: 0
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(CameraDesignTokens.Dimensions.filterCarouselHeight)
            .padding(horizontal = CameraDesignTokens.Dimensions.glassPillHorizontalMargin)
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(CameraDesignTokens.Dimensions.filterCardSpacing),
            contentPadding = PaddingValues(
                horizontal = (screenWidth - CameraDesignTokens.Dimensions.filterCardSize) / 2
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(filters) { index, filter ->
                FilterCard(
                    filter = filter,
                    isCenter = index == centerIndex,
                    isSelected = index == selectedIndex,
                    scrollOffset = calculateScrollOffset(listState, index),
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onFilterSelected(index)
                        scope.launch {
                            listState.animateScrollToItem(
                                index = index,
                                scrollOffset = -(screenWidth.value.toInt() / 2 - 
                                              CameraDesignTokens.Dimensions.filterCardSize.value.toInt() / 2).toInt()
                            )
                        }
                    }
                )
            }
        }
    }
}

/**
 * Individual filter card with parallax effect
 */
@Composable
private fun FilterCard(
    filter: FilterPreset,
    isCenter: Boolean,
    isSelected: Boolean,
    scrollOffset: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    
    // Scale animation based on center position
    val targetScale = if (isCenter) {
        CameraDesignTokens.Dimensions.filterCardCenterScale
    } else {
        CameraDesignTokens.Dimensions.filterCardNeighborScale
    }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) targetScale * 0.96f else targetScale,
        animationSpec = spring(
            stiffness = Spring.StiffnessHigh,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "card_scale"
    )
    
    // Parallax offset for image inside card
    val parallaxOffset = scrollOffset * 8f // +/- 8px as specified
    
    Surface(
        modifier = modifier
            .size(CameraDesignTokens.Dimensions.filterCardSize)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = CameraDesignTokens.Shadows.filterCard.blurRadius,
                shape = RoundedCornerShape(CameraDesignTokens.Dimensions.filterCardCornerRadius),
                clip = false
            )
            .clickable {
                isPressed = true
                onClick()
            },
        shape = RoundedCornerShape(CameraDesignTokens.Dimensions.filterCardCornerRadius),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(CameraDesignTokens.Dimensions.filterCardCornerRadius))
        ) {
            // Thumbnail with parallax effect
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = parallaxOffset
                    }
            ) {
                if (filter.thumbnailUrl != null) {
                    AsyncImage(
                        model = filter.thumbnailUrl,
                        contentDescription = filter.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Gradient placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        filter.gradientStart,
                                        filter.gradientEnd
                                    )
                                )
                            )
                    )
                }
            }
            
            // Filter name overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f)
                            )
                        )
                    )
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = filter.name,
                    fontSize = 10.sp,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            
            // Selected indicator
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = CameraDesignTokens.Colors.accentStart.copy(alpha = 0.2f)
                        )
                )
            }
        }
    }
    
    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(90)
            isPressed = false
        }
    }
}

/**
 * Calculate scroll offset for parallax effect
 */
@Composable
private fun calculateScrollOffset(
    listState: androidx.compose.foundation.lazy.LazyListState,
    itemIndex: Int
): Float {
    val layoutInfo = listState.layoutInfo
    val viewportCenter = layoutInfo.viewportEndOffset / 2f
    
    val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == itemIndex }
    return if (itemInfo != null) {
        val itemCenter = itemInfo.offset + itemInfo.size / 2f
        val offset = (itemCenter - viewportCenter) / viewportCenter
        offset.coerceIn(-1f, 1f)
    } else {
        0f
    }
}

/**
 * Filter preset data class
 */
data class FilterPreset(
    val id: String,
    val name: String,
    val thumbnailUrl: String? = null,
    val gradientStart: Color = CameraDesignTokens.Colors.accentStart,
    val gradientEnd: Color = CameraDesignTokens.Colors.accentEnd,
    val intensity: Float = 1.0f
)

/**
 * Sample filter presets
 */
val sampleFilterPresets = listOf(
    FilterPreset(
        id = "original",
        name = "Original",
        gradientStart = Color(0xFF1A1A1A),
        gradientEnd = Color(0xFF3A3A3A)
    ),
    FilterPreset(
        id = "vivid",
        name = "Vivid",
        gradientStart = Color(0xFFFF6B6B),
        gradientEnd = Color(0xFF4ECDC4)
    ),
    FilterPreset(
        id = "mono",
        name = "Mono",
        gradientStart = Color(0xFF2C3E50),
        gradientEnd = Color(0xFF95A5A6)
    ),
    FilterPreset(
        id = "warm",
        name = "Warm",
        gradientStart = Color(0xFFF39C12),
        gradientEnd = Color(0xFFE74C3C)
    ),
    FilterPreset(
        id = "cool",
        name = "Cool",
        gradientStart = Color(0xFF3498DB),
        gradientEnd = Color(0xFF2980B9)
    ),
    FilterPreset(
        id = "dramatic",
        name = "Dramatic",
        gradientStart = Color(0xFF8E44AD),
        gradientEnd = Color(0xFF2C3E50)
    ),
    FilterPreset(
        id = "soft",
        name = "Soft",
        gradientStart = Color(0xFFFFB6C1),
        gradientEnd = Color(0xFFFFC0CB)
    ),
    FilterPreset(
        id = "vintage",
        name = "Vintage",
        gradientStart = Color(0xFFD2691E),
        gradientEnd = Color(0xFF8B4513)
    )
)

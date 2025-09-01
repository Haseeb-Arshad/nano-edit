package com.example.myapplication.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.myapplication.ui.motion.Motion
import com.example.myapplication.ui.motion.MotionTokens
import kotlin.math.abs

data class FilterPreset(
    val id: String,
    val name: String,
    val tint: Color = Color.Unspecified
)

val SamplePresets = listOf(
    FilterPreset("orig", "Original"),
    FilterPreset("pop", "Pop", Color(0x4056CCF2)),
    FilterPreset("warm", "Warm", Color(0x40F2B56B)),
    FilterPreset("cool", "Cool", Color(0x4048C9B0)),
    FilterPreset("mono", "Mono", Color(0x40222222)),
    FilterPreset("cin", "Cinematic", Color(0x404E342E))
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilterCarousel(
    imageModel: Any?,
    presets: List<FilterPreset> = SamplePresets,
    onApplyPreset: (FilterPreset) -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 140.dp,
    cardHeight: Dp = 180.dp
) {
    val listState = rememberLazyListState()
    val fling = rememberSnapFlingBehavior(listState)

    LazyRow(
        state = listState,
        flingBehavior = fling,
        contentPadding = PaddingValues(horizontal = 24.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        items(count = presets.size, key = { i -> presets[i].id }) { index ->
            val info: LazyListItemInfo? = listState.layoutInfo.visibleItemsInfo.find { it.index == index }
            val center = listState.layoutInfo.viewportEndOffset / 2f
            val offsetCenterPx = if (info != null) info.offset + info.size / 2f else 0f
            val distanceFromCenter = (offsetCenterPx - center)
            val norm = if (listState.layoutInfo.viewportEndOffset == 0) 0f else distanceFromCenter / center
            val clamped = norm.coerceIn(-1f, 1f)

            val scale by animateFloatAsState(
                targetValue = 1f - 0.14f * abs(clamped),
                animationSpec = Motion.Springs.Standard,
                label = "scale"
            )
            val elevation by animateFloatAsState(
                targetValue = 8f - 6f * abs(clamped),
                animationSpec = Motion.Springs.Standard,
                label = "elev"
            )
            val parallaxX by animateFloatAsState(
                targetValue = 24f * clamped,
                animationSpec = Motion.Springs.Standard,
                label = "parallax"
            )

            Box(
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 12.dp)
                    .height(cardHeight)
                    .width(cardWidth)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        shadowElevation = elevation
                    }
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(2.dp)
            ) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = presets[index].name,
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer { translationX = parallaxX },
                    contentScale = ContentScale.Crop
                )
                if (presets[index].tint != Color.Unspecified) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(presets[index].tint)
                    )
                }
                Text(
                    text = presets[index].name,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}


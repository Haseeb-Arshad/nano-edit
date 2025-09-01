package com.example.myapplication.ui.screens

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.myapplication.ui.motion.Motion
import com.example.myapplication.ui.motion.MotionTokens
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryAnimated(
    items: List<Uri>,
    modifier: Modifier = Modifier,
    onItemClick: (Uri) -> Unit
) {
    val gridState = rememberLazyGridState()
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        state = gridState,
        modifier = modifier.testTag("galleryGrid")
    ) {
        items(items.size, key = { i -> items[i] }) { index ->
            val alpha = remember { Animatable(0f) }
            val scale = remember { Animatable(0.9f) }
            val clickScope = rememberCoroutineScope()
            LaunchedEffect(Unit) {
                val delayMs = (index % 40) * MotionTokens.StaggerBase
                kotlinx.coroutines.delay(delayMs.toLong())
                alpha.animateTo(1f, tween(Motion.Durations.Medium))
                scale.animateTo(1f, tween(Motion.Durations.Medium))
            }
            AsyncImage(
                model = items[index],
                contentDescription = "Gallery item $index",
                modifier = Modifier
                    .padding(4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .aspectRatio(1f)
                    .graphicsLayer {
                        this.alpha = alpha.value
                        this.scaleX = scale.value
                        this.scaleY = scale.value
                    }
                    .semantics { contentDescription = "Image ${index + 1}" }
                    .combinedClickable(
                        onClick = {
                            // Small tap animation then callback
                            clickScope.launch {
                                scale.snapTo(0.96f)
                                scale.animateTo(1f, tween(Motion.Durations.Small))
                            }
                            onItemClick(items[index])
                        },
                        onLongClick = {}
                    ),
                contentScale = ContentScale.Crop
            )
        }
    }
}

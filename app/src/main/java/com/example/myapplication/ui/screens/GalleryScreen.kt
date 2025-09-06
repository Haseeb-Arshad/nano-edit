package com.example.myapplication.ui.screens

import android.content.ContentUris
import android.provider.MediaStore
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width

enum class ViewMode { Grid, List }


@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onBack: (() -> Unit)? = null,
    onOpen: ((android.net.Uri) -> Unit)? = null
) {
    val ctx = LocalContext.current
    val entries = remember { mutableStateListOf<Pair<android.net.Uri, Long>>() }

    var sortDescending by remember { mutableStateOf(true) }
    var viewMode by remember { mutableStateOf(ViewMode.Grid) }
    var selected by remember { mutableStateOf<android.net.Uri?>(null) }

    fun reload() {
        entries.clear()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.RELATIVE_PATH
        )
        val selection = MediaStore.Images.Media.RELATIVE_PATH + " LIKE ?"
        val selectionArgs = arrayOf("Pictures/AI Camera%")
        val order = MediaStore.Images.Media.DATE_ADDED + if (sortDescending) " DESC" else " ASC"
        ctx.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            order
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val dateSec = cursor.getLong(dateCol)
                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                entries += uri to dateSec
            }
        }
    }

    fun bucketFor(epochSeconds: Long): String {
        val now = System.currentTimeMillis() / 1000
        val days = ((now - epochSeconds) / 86400).toInt()
        return when {
            days < 1 -> "Today"
            days < 7 -> "This week"
            days < 30 -> "This month"
            else -> "Earlier"
        }
    }

    LaunchedEffect(sortDescending) { reload() }

    androidx.compose.foundation.layout.Column(Modifier.fillMaxSize()) {
        // Glass top bar with title and segmented controls
        com.example.myapplication.ui.components.GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            androidx.compose.foundation.layout.Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    com.example.myapplication.ui.components.GlassButton(onClick = onBack) { Text("Back") }
                } else {
                    androidx.compose.foundation.layout.Spacer(Modifier)
                }
                Text("Library", style = MaterialTheme.typography.titleLarge)
                androidx.compose.foundation.layout.Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    com.example.myapplication.ui.components.GlassButton(onClick = { sortDescending = !sortDescending }) { Text(if (sortDescending) "Recent" else "Oldest") }
                    com.example.myapplication.ui.components.GlassButton(onClick = { viewMode = if (viewMode == ViewMode.Grid) ViewMode.List else ViewMode.Grid }) { Text(if (viewMode == ViewMode.Grid) "Grid" else "List") }
                }
            }
        }

        // Build ordered bucket labels from the current entries order
        val labelOrder = entries.map { bucketFor(it.second) }.distinct()

        if (viewMode == ViewMode.Grid) {
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(minSize = 120.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                if (entries.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        com.example.myapplication.ui.components.GlassCard { androidx.compose.foundation.layout.Box(Modifier.padding(16.dp)) { Text("No edits yet. Capture your first photo!") } }
                    }
                } else {
                    labelOrder.forEach { label ->
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp))
                        }
                        val group = entries.filter { bucketFor(it.second) == label }
                        items(group.size) { idx ->
                            val uri = group[idx].first
                            androidx.compose.material3.Card(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .aspectRatio(1f)
                                        .combinedClickable(
                                            onClick = { onOpen?.invoke(uri) },
                                            onLongClick = { selected = uri }
                                        ),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // List mode with bigger rows and metadata buckets
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp)
            ) {
                if (entries.isEmpty()) {
                    item { com.example.myapplication.ui.components.GlassCard { androidx.compose.foundation.layout.Box(Modifier.padding(16.dp)) { Text("No edits yet. Capture your first photo!") } } }
                } else {
                    labelOrder.forEach { label ->
                        item { Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)) }
                        val group = entries.filter { bucketFor(it.second) == label }
                        items(group) { pair ->
                            val uri = pair.first
                            androidx.compose.material3.Card(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                androidx.compose.foundation.layout.Row(Modifier.fillMaxSize().combinedClickable(onClick = { onOpen?.invoke(uri) }, onLongClick = { selected = uri }).padding(8.dp)) {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = null,
                                        modifier = Modifier.size(96.dp).aspectRatio(1f),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                    androidx.compose.foundation.layout.Spacer(Modifier.width(12.dp))
                                    androidx.compose.foundation.layout.Column(Modifier.align(androidx.compose.ui.Alignment.CenterVertically)) {
                                        Text("Edited photo", style = MaterialTheme.typography.titleSmall)
                                        Text("Tap to open", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom sheet for details/actions
        val sel = selected
        if (sel != null) {
            ModalBottomSheet(onDismissRequest = { selected = null }) {
                androidx.compose.foundation.layout.Column(Modifier.padding(16.dp)) {
                    Text("Item", style = MaterialTheme.typography.titleMedium)
                    androidx.compose.foundation.layout.Spacer(Modifier.padding(4.dp))
                    androidx.compose.material3.TextButton(onClick = { onOpen?.invoke(sel); selected = null }) { Text("Open in Editor") }
                    androidx.compose.material3.TextButton(onClick = { com.example.myapplication.utils.shareImage(ctx, sel); selected = null }) { Text("Share") }
                    androidx.compose.material3.TextButton(onClick = {
                        try {
                            ctx.contentResolver.delete(sel, null, null)
                        } catch (_: Exception) {}
                        selected = null
                        reload()
                    }) { Text("Delete") }
                }
            }
        }
    }
}

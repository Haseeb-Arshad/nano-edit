package com.example.myapplication.ui.screens

import android.content.ContentUris
import android.provider.MediaStore
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.foundation.lazy.grid.GridItemSpan

@Composable
fun GalleryScreen(
    onBack: (() -> Unit)? = null,
    onOpen: ((android.net.Uri) -> Unit)? = null
) {
    val ctx = LocalContext.current
    val entries = remember { mutableStateListOf<Pair<android.net.Uri, Long>>() }

    var sortDescending by remember { mutableStateOf(true) }

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
        // Minimal top bar with sort toggle
        androidx.compose.foundation.layout.Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
        ) {
            if (onBack != null) {
                androidx.compose.material3.Button(onClick = onBack) { Text("Back") }
            }
            Text("Your Edits", style = MaterialTheme.typography.titleLarge)
            androidx.compose.material3.OutlinedButton(onClick = { sortDescending = !sortDescending }) {
                Text(if (sortDescending) "Recent" else "Oldest")
            }
        }

        // Build ordered bucket labels from the current entries order
        val labelOrder = entries.map { bucketFor(it.second) }.distinct()

        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(minSize = 120.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            if (entries.isEmpty()) {
                item {
                    Text(
                        "No edits yet",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
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
                                    .clickable { onOpen?.invoke(uri) },
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }
    }
}


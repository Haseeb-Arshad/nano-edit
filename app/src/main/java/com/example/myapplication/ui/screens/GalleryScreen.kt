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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun GalleryScreen(
    onBack: (() -> Unit)? = null,
    onOpen: ((android.net.Uri) -> Unit)? = null
) {
    val ctx = LocalContext.current
    val uris = remember { mutableStateListOf<android.net.Uri>() }
    LaunchedEffect(Unit) {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH
        )
        val selection = MediaStore.Images.Media.RELATIVE_PATH + " LIKE ?"
        val selectionArgs = arrayOf("Pictures/AI Camera%")
        ctx.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            MediaStore.Images.Media.DATE_ADDED + " DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                uris += uri
            }
        }
    }

    androidx.compose.foundation.layout.Column(Modifier.fillMaxSize()) {
        // Minimal top bar
        androidx.compose.foundation.layout.Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
        ) {
            if (onBack != null) {
                androidx.compose.material3.Button(onClick = onBack) { Text("Back") }
            }
            Text("Gallery", style = MaterialTheme.typography.titleLarge)
            androidx.compose.foundation.layout.Spacer(Modifier)
        }

        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(minSize = 120.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            items(uris.size) { idx ->
                val uri = uris[idx]
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
            if (uris.isEmpty()) {
                item {
                    Text(
                        "No edits yet",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}


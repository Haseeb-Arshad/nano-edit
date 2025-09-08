package com.example.myapplication.ui.modern

import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted
import com.example.myapplication.controller.EditController
import com.example.myapplication.ui.components.*
import com.example.myapplication.ui.theme.DesignTokens

@Composable
fun ModernEditorScreen(
    src: Uri?,
    controller: EditController,
    onBack: () -> Unit,
    autoSceneLift: Boolean = false
) {
    // Delegate to existing EditorScreen for now
    com.example.myapplication.ui.screens.EditorScreen(
        src = src, 
        controller = controller, 
        autoSmartEnhance = autoSceneLift, 
        onBack = onBack
    )
}

data class GalleryItem(val uri: Uri, val dateAdded: Long, val width: Int, val height: Int)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ModernGalleryScreen(
    onBack: () -> Unit,
    onOpen: (Uri) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val isT = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val readPerm = if (isT) android.Manifest.permission.READ_MEDIA_IMAGES else android.Manifest.permission.READ_EXTERNAL_STORAGE
    val permState = rememberPermissionState(readPerm)
    var onlyAICamera by remember { mutableStateOf(false) }
    var items by remember { mutableStateOf<List<GalleryItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(permState.status.isGranted, onlyAICamera) {
        if (!permState.status.isGranted) return@LaunchedEffect
        loading = true; error = null
        items = loadGalleryItems(ctx, onlyAICamera)
        loading = false
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedGradientBackground(modifier = Modifier.fillMaxSize())
        
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(DesignTokens.Spacing.md)
                    .statusBarsPadding(),
                cornerRadius = DesignTokens.Radius.pill
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(DesignTokens.Sizes.topBarHeight)
                        .padding(horizontal = DesignTokens.Spacing.lg),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassmorphicButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onBack()
                        },
                        cornerRadius = DesignTokens.Radius.pill
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Text(
                        text = "Gallery",
                        color = DesignTokens.Colors.Light.textHigh,
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Spacer(modifier = Modifier.size(40.dp)) // Balance the back button
                }
            }
            
            // Permission gate and content
            if (!permState.status.isGranted) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Allow access to your photos to view the gallery", color = Color.White.copy(alpha = 0.85f))
                        Spacer(Modifier.height(12.dp))
                        GlassmorphicButton(onClick = { permState.launchPermissionRequest() }, cornerRadius = DesignTokens.Radius.pill) {
                            Text("Grant Permission", color = Color.White, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
                        }
                    }
                }
            } else {
                Column(Modifier.weight(1f).fillMaxWidth()) {
                    // Filter row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = DesignTokens.Spacing.md, vertical = DesignTokens.Spacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = { onlyAICamera = false },
                            label = { Text("All Photos") },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = Color.White,
                                containerColor = if (!onlyAICamera) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f)
                            )
                        )
                        AssistChip(
                            onClick = { onlyAICamera = true },
                            label = { Text("AI Camera") },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = Color.White,
                                containerColor = if (onlyAICamera) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f)
                            )
                        )
                    }

                    if (loading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (items.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No photos found", color = Color.White.copy(alpha = 0.8f))
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 120.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = DesignTokens.Spacing.md)
                                .navigationBarsPadding(),
                            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm),
                            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm)
                        ) {
                            items(items) { item ->
                                val ratio = if (item.width > 0 && item.height > 0) item.width.toFloat() / item.height.toFloat() else 1f
                                GlassmorphicCard(cornerRadius = DesignTokens.Radius.md) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(ratio)
                                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(DesignTokens.Radius.md))
                                            .clickable {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onOpen(item.uri)
                                            }
                                    ) {
                                        SubcomposeAsyncImage(
                                            model = item.uri,
                                            contentDescription = "Photo",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop,
                                            loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White.copy(alpha = 0.6f)) } }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun loadGalleryItems(context: android.content.Context, onlyAi: Boolean): List<GalleryItem> {
    val resolver = context.contentResolver
    val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DATE_ADDED,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
        MediaStore.Images.Media.WIDTH,
        MediaStore.Images.Media.HEIGHT
    )
    val selection: String?
    val args: Array<String>?
    if (onlyAi) {
        selection = MediaStore.Images.Media.BUCKET_DISPLAY_NAME + "=?"
        args = arrayOf("AI Camera")
    } else {
        selection = null
        args = null
    }
    val sort = MediaStore.Images.Media.DATE_ADDED + " DESC"
    val uriBase = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val out = mutableListOf<GalleryItem>()
    resolver.query(uriBase, projection, selection, args, sort)?.use { c ->
        val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        val dateCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
        val wCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
        val hCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
        while (c.moveToNext()) {
            val id = c.getLong(idCol)
            val date = c.getLong(dateCol)
            val uri = android.content.ContentUris.withAppendedId(uriBase, id)
            val w = c.getInt(wCol)
            val h = c.getInt(hCol)
            out += GalleryItem(uri, date, w, h)
        }
    }
    return out
}

@Composable
fun ModernSettingsScreen(
    onBack: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedGradientBackground(modifier = Modifier.fillMaxSize())
        
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(DesignTokens.Spacing.md)
                    .statusBarsPadding(),
                cornerRadius = DesignTokens.Radius.pill
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(DesignTokens.Sizes.topBarHeight)
                        .padding(horizontal = DesignTokens.Spacing.lg),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassmorphicButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onBack()
                        },
                        cornerRadius = DesignTokens.Radius.pill
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    Text(
                        text = "Settings",
                        color = DesignTokens.Colors.Light.textHigh,
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Spacer(modifier = Modifier.size(40.dp)) // Balance the back button
                }
            }
            
            // Settings content placeholder
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                GlassmorphicCard(cornerRadius = DesignTokens.Radius.lg) {
                    Text(
                        text = "Settings coming soon",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(DesignTokens.Spacing.xl)
                    )
                }
            }
        }
    }
}

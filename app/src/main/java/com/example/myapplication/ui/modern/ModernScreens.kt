package com.example.myapplication.ui.modern

import android.net.Uri
import androidx.compose.runtime.Composable
import com.example.myapplication.controller.EditController

@Composable
fun ModernReviewScreen(
    uri: Uri?,
    onEdit: () -> Unit,
    onBack: () -> Unit
) {
    // Delegate to existing ReviewScreen for now
    com.example.myapplication.ReviewScreen(uri = uri, onEdit = onEdit)
}

@Composable
fun ModernEditorScreen(
    src: Uri?,
    controller: EditController,
    onBack: () -> Unit
) {
    // Delegate to existing EditorScreen
    com.example.myapplication.ui.screens.EditorScreen(src = src, controller = controller)
}

@Composable
fun ModernGalleryScreen(
    onBack: () -> Unit
) {
    // Delegate to existing GalleryScreen for now
    com.example.myapplication.ui.screens.GalleryScreen()
}

@Composable
fun ModernSettingsScreen(
    onBack: () -> Unit
) {
    // Delegate to existing SettingsScreen for now
    com.example.myapplication.ui.screens.SettingsScreen()
}


package com.example.myapplication.ui.modern

import android.net.Uri
import androidx.compose.runtime.Composable
import com.example.myapplication.controller.EditController

@Composable
fun ModernReviewScreen(
    uri: Uri?,
    onEdit: () -> Unit,
    onSceneLift: () -> Unit,
    onBack: () -> Unit
) {
    // Delegate to existing ReviewScreen for now
    com.example.myapplication.ReviewScreen(uri = uri, onEdit = onEdit, onSceneLift = onSceneLift)
}

@Composable
fun ModernEditorScreen(
    src: Uri?,
    controller: EditController,
    onBack: () -> Unit,
    autoSceneLift: Boolean = false
) {
    // Delegate to existing EditorScreen
    com.example.myapplication.ui.screens.EditorScreen(src = src, controller = controller, autoSmartEnhance = autoSceneLift, onBack = onBack)
}

@Composable
fun ModernGalleryScreen(
    onBack: () -> Unit,
    onOpen: (Uri) -> Unit
) {
    com.example.myapplication.ui.screens.GalleryScreen(onBack = onBack, onOpen = onOpen)
}

@Composable
fun ModernSettingsScreen(
    onBack: () -> Unit
) {
    // Delegate to existing SettingsScreen for now
    com.example.myapplication.ui.screens.SettingsScreen()
}


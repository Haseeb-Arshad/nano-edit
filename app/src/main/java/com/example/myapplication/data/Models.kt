package com.example.myapplication.data

import android.net.Uri

data class CameraUiState(
    val hasPermission: Boolean = false,
    val isCapturing: Boolean = false,
    val flashEnabled: Boolean = false,
    val lastCaptured: Uri? = null,
    val error: String? = null
)

data class Suggestion(
    val id: String,
    val title: String,
    val prompt: String
)

data class EditUiState(
    val sourceUri: Uri? = null,
    val prompt: String = "",
    val isLoading: Boolean = false,
    val resultUrl: String? = null,
    val error: String? = null
)


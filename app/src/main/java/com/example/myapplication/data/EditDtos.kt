package com.example.myapplication.data

data class EditRequestDto(
    val imageUrl: String?,
    val maskUrl: String?,
    val prompt: String,
    val strength: Float? = null,
    val upscaling: Boolean = false,
    val size: String = "original"
)

data class EditJobDto(
    val id: String,
    val status: String,
    val resultUrl: String? = null,
    val error: String? = null
)


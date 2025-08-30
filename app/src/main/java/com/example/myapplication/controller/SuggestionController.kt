package com.example.myapplication.controller

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.scale
import com.example.myapplication.data.Suggestion
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.common.ImageLabelerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class SuggestionController {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val _chips = MutableStateFlow<List<Suggestion>>(emptyList())
    val chips: StateFlow<List<Suggestion>> = _chips

    fun computeSuggestions(context: Context, uri: Uri?) {
        if (uri == null) {
            _chips.value = fallback()
            return
        }
        scope.launch {
            runCatching {
                val stream = context.contentResolver.openInputStream(uri) ?: return@runCatching
                stream.use {
                    val bmp = BitmapFactory.decodeStream(it) ?: return@runCatching
                    val small = bmp.scale(640, (640f / bmp.width * bmp.height).toInt(), filter = true)
                    val image = InputImage.fromBitmap(small, 0)
                    val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
                    val result = labeler.process(image).await()
                    val labels = result.map { l -> l.text.lowercase() }
                    _chips.value = mapLabelsToSuggestions(labels)
                }
            }.onFailure {
                _chips.value = fallback()
            }
        }
    }

    private fun mapLabelsToSuggestions(labels: List<String>): List<Suggestion> {
        val chips = mutableListOf<Suggestion>()
        if (labels.any { it.contains("person") || it.contains("selfie") || it.contains("portrait") }) {
            chips += Suggestion("portrait", "Portrait Pop", "enhance skin, bokeh background, clarity")
        }
        if (labels.any { it.contains("food") }) {
            chips += Suggestion("food", "Tasty Warmth", "warm tone, increase contrast, sharpen details")
        }
        if (labels.any { it.contains("document") || it.contains("text") }) {
            chips += Suggestion("doc", "Scan Clean", "high contrast, black and white, reduce noise")
        }
        if (chips.isEmpty()) chips += fallback()
        return chips.take(3)
    }

    private fun fallback(): List<Suggestion> = listOf(
        Suggestion("warm", "Warm Glow", "warm tone, soft light, subtle contrast"),
        Suggestion("bw", "Crisp B/W", "high contrast, black and white, fine grain"),
        Suggestion("hdr", "Detail Boost", "increase clarity, micro-contrast, natural colors")
    )
}

// Simple Task.await bridge for ML Kit Task
private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
    kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        addOnSuccessListener { res -> cont.resume(res) {} }
        addOnFailureListener { e -> cont.resumeWith(Result.failure(e)) }
        addOnCanceledListener { cont.cancel() }
    }

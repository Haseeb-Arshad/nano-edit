package com.example.myapplication.ui.filters

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

data class FilterPreset(
    val id: String,
    val name: String,
    val matrix: (Float) -> ColorMatrix // function of intensity [0..1]
)

object FilterPresets {
    val Vibrant = FilterPreset("vibrant", "Vibrant") { i ->
        ColorMatrix().apply {
            val sat = 1f + 0.6f * i
            val s = ColorMatrix().apply { setSaturation(sat) }
            postConcat(s)
        }
    }
    val Warm = FilterPreset("warm", "Warm") { i ->
        val m = ColorMatrix()
        val warmth = 1f + 0.2f * i
        val contrast = 1f + 0.1f * i
        m.postConcat(ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, 8f * i,
            0f, contrast, 0f, 0f, 6f * i,
            0f, 0f, contrast, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )))
        val s = ColorMatrix().apply { setSaturation(1f + 0.2f * i) }
        m.postConcat(s)
        m
    }
    val Cool = FilterPreset("cool", "Cool") { i ->
        val m = ColorMatrix()
        m.postConcat(ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, -6f * i,
            0f, 1f, 0f, 0f, 0f,
            0f, 0f, 1f, 0f, 8f * i,
            0f, 0f, 0f, 1f, 0f
        )))
        val s = ColorMatrix().apply { setSaturation(1f + 0.1f * i) }
        m.postConcat(s)
        m
    }
    val Mono = FilterPreset("mono", "Mono") { i ->
        ColorMatrix().apply {
            setSaturation(1f - 1f * i)
        }
    }
    val Sepia = FilterPreset("sepia", "Sepia") { i ->
        val depth = 20f * i
        ColorMatrix(floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, depth,
            0.349f, 0.686f, 0.168f, 0f, depth,
            0.272f, 0.534f, 0.131f, 0f, depth,
            0f, 0f, 0f, 1f, 0f
        ))
    }
    val HighContrast = FilterPreset("hi_contrast", "High Contrast") { i ->
        val c = 1f + 0.7f * i
        val e = 10f * i
        ColorMatrix(floatArrayOf(
            c, 0f, 0f, 0f, e,
            0f, c, 0f, 0f, e,
            0f, 0f, c, 0f, e,
            0f, 0f, 0f, 1f, 0f
        ))
    }
    val Soft = FilterPreset("soft", "Soft") { i ->
        val c = 1f - 0.2f * i
        val s = 1f + 0.1f * i
        ColorMatrix().apply {
            postConcat(ColorMatrix(floatArrayOf(
                c, 0f, 0f, 0f, 0f,
                0f, c, 0f, 0f, 0f,
                0f, 0f, c, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )))
            val sat = ColorMatrix().apply { setSaturation(s) }
            postConcat(sat)
        }
    }
    val Film = FilterPreset("film", "Film") { i ->
        val m = ColorMatrix()
        m.postConcat(ColorMatrix(floatArrayOf(
            1.1f, 0f, 0f, 0f, -8f * i,
            0f, 1.05f, 0f, 0f, -6f * i,
            0f, 0f, 0.95f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )))
        val s = ColorMatrix().apply { setSaturation(1f + 0.2f * i) }
        m.postConcat(s)
        m
    }
    val Retro = FilterPreset("retro", "Retro") { i ->
        val m = ColorMatrix()
        m.postConcat(ColorMatrix(floatArrayOf(
            1.05f, 0f, 0f, 0f, 6f * i,
            0f, 1.0f, 0f, 0f, 2f * i,
            0f, 0f, 0.9f, 0f, -4f * i,
            0f, 0f, 0f, 1f, 0f
        )))
        m
    }
    val Cinematic = FilterPreset("cinematic", "Cinematic") { i ->
        val m = ColorMatrix()
        val c = 1f + 0.25f * i
        val s = 1f + 0.1f * i
        m.postConcat(ColorMatrix(floatArrayOf(
            c, 0f, 0f, 0f, -10f * i,
            0f, c, 0f, 0f, -10f * i,
            0f, 0f, c, 0f, 10f * i,
            0f, 0f, 0f, 1f, 0f
        )))
        val sm = ColorMatrix().apply { setSaturation(s) }
        m.postConcat(sm)
        m
    }
    val Vivid = FilterPreset("vivid", "Vivid") { i ->
        val s = ColorMatrix().apply { setSaturation(1.4f * i + 1f) }
        s
    }
    val Night = FilterPreset("night", "Night") { i ->
        val m = ColorMatrix()
        val c = 1f - 0.1f * i
        m.postConcat(ColorMatrix(floatArrayOf(
            c, 0f, 0f, 0f, -8f * i,
            0f, c, 0f, 0f, -8f * i,
            0f, 0f, c, 0f, 8f * i,
            0f, 0f, 0f, 1f, 0f
        )))
        m
    }

    val All = listOf(
        Vibrant, Warm, Cool, Mono, Sepia, HighContrast, Soft, Film, Retro, Cinematic, Vivid, Night
    )
}

fun applyPreset(src: Bitmap, preset: FilterPreset, intensity: Float): Bitmap {
    val m = preset.matrix(intensity.coerceIn(0f, 1f))
    val out = Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
    val c = Canvas(out)
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { colorFilter = ColorMatrixColorFilter(m) }
    c.drawBitmap(src, 0f, 0f, p)
    return out
}

package com.example.myapplication.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

/**
 * Fast local enhancement to provide an instant "AI-like" preview.
 * Adjusts exposure, contrast, saturation, and a mild clarity effect.
 */
fun quickEnhance(src: Bitmap, intensity: Float = 0.6f): Bitmap {
    val i = intensity.coerceIn(0f, 1f)

    // Parameters tuned for pleasing default; scale by intensity
    val exposure = 1f + 0.25f * i      // >1 brightens
    val contrast = 1f + 0.35f * i      // >1 increases contrast
    val saturation = 1f + 0.4f * i     // >1 increases saturation

    val cm = ColorMatrix()

    // Exposure/contrast via scale and translate on RGB
    val c = contrast
    val e = (exposure - 1f) * 128f
    val contrastMatrix = ColorMatrix(
        floatArrayOf(
            c, 0f, 0f, 0f, e,
            0f, c, 0f, 0f, e,
            0f, 0f, c, 0f, e,
            0f, 0f, 0f, 1f, 0f
        )
    )
    cm.postConcat(contrastMatrix)

    // Saturation
    val satMatrix = ColorMatrix()
    satMatrix.setSaturation(saturation)
    cm.postConcat(satMatrix)

    // Output
    val out = Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(cm)
    }
    canvas.drawBitmap(src, 0f, 0f, paint)

    return out
}

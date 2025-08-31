package com.example.myapplication.filters

import android.graphics.*
import android.renderscript.*
import android.content.Context
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.toArgb
import com.example.myapplication.ui.theme.*
import kotlin.math.*

/**
 * Real-time filter engine for camera effects
 * Inspired by B612 and Snapchat filters
 */
class RealTimeFilterEngine(private val context: Context) {
    
    private val renderScript = RenderScript.create(context)
    
    // Filter presets
    enum class FilterPreset {
        NONE,
        BEAUTY_SMOOTH,
        VINTAGE_FILM,
        COLD_WINTER,
        WARM_SUNSET,
        DREAMY_PINK,
        NEON_GLOW,
        BLACK_WHITE,
        SEPIA_CLASSIC,
        CYBERPUNK,
        FAIRY_TALE,
        TOKYO_NIGHT
    }
    
    /**
     * Apply filter to bitmap
     */
    fun applyFilter(bitmap: Bitmap, preset: FilterPreset, intensity: Float = 1.0f): Bitmap {
        return when (preset) {
            FilterPreset.NONE -> bitmap
            FilterPreset.BEAUTY_SMOOTH -> applyBeautyFilter(bitmap, intensity)
            FilterPreset.VINTAGE_FILM -> applyVintageFilter(bitmap, intensity)
            FilterPreset.COLD_WINTER -> applyColdFilter(bitmap, intensity)
            FilterPreset.WARM_SUNSET -> applyWarmFilter(bitmap, intensity)
            FilterPreset.DREAMY_PINK -> applyDreamyFilter(bitmap, intensity)
            FilterPreset.NEON_GLOW -> applyNeonFilter(bitmap, intensity)
            FilterPreset.BLACK_WHITE -> applyBlackWhiteFilter(bitmap, intensity)
            FilterPreset.SEPIA_CLASSIC -> applySepiaFilter(bitmap, intensity)
            FilterPreset.CYBERPUNK -> applyCyberpunkFilter(bitmap, intensity)
            FilterPreset.FAIRY_TALE -> applyFairyTaleFilter(bitmap, intensity)
            FilterPreset.TOKYO_NIGHT -> applyTokyoNightFilter(bitmap, intensity)
        }
    }
    
    /**
     * Beauty filter with skin smoothing and face enhancement
     */
    fun applyBeautyFilter(bitmap: Bitmap, intensity: Float): Bitmap {
        val result = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            isAntiAlias = true
            maskFilter = BlurMaskFilter(intensity * 5, BlurMaskFilter.Blur.NORMAL)
        }
        
        // Apply gaussian blur for skin smoothing
        val blurred = gaussianBlur(bitmap, (intensity * 10).toInt())
        
        // Blend original with blurred for natural look
        val blendPaint = Paint().apply {
            alpha = (intensity * 180).toInt()
        }
        canvas.drawBitmap(blurred, 0f, 0f, blendPaint)
        
        // Enhance brightness slightly
        val colorMatrix = ColorMatrix().apply {
            setScale(
                1.0f + intensity * 0.1f,
                1.0f + intensity * 0.1f,
                1.0f + intensity * 0.15f,
                1.0f
            )
        }
        
        val colorPaint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(result, 0f, 0f, colorPaint)
        
        return result
    }
    
    /**
     * Vintage film filter
     */
    fun applyVintageFilter(bitmap: Bitmap, intensity: Float): Bitmap {
        val result = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
val canvas = Canvas(result)
        
        // Add sepia tone
        val sepiaMatrix = ColorMatrix().apply {
            setSaturation(0.5f)
            val sepiaValues = floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
            postConcat(ColorMatrix(sepiaValues))
        }
        
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(sepiaMatrix)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        // Add vignette effect
        addVignetteEffect(canvas, result.width, result.height, intensity)
        
        // Add film grain
        addFilmGrain(canvas, result.width, result.height, intensity)
        
        return result
    }
    
    /**
     * Cold filter with blue tones
     */
    fun applyColdFilter(bitmap: Bitmap, intensity: Float): Bitmap {
        val result = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        
        val colorMatrix = ColorMatrix().apply {
            setScale(
                1.0f - intensity * 0.2f,
                1.0f,
                1.0f + intensity * 0.3f,
                1.0f
            )
        }
        
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return result
    }
    
    /**
     * Warm filter with orange/yellow tones
     */
    fun applyWarmFilter(bitmap: Bitmap, intensity: Float): Bitmap {
        val result = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        
        val colorMatrix = ColorMatrix().apply {
            setScale(
                1.0f + intensity * 0.3f,
                1.0f + intensity * 0.1f,
                1.0f - intensity * 0.2f,
                1.0f
            )
        }
        
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return result
    }
    
    /**
     * Dreamy pink filter
     */
    fun applyDreamyFilter(bitmap: Bitmap, intensity: Float): Bitmap {
        val result = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        
        // Apply soft pink overlay
        val overlayPaint = Paint().apply {
            color = DreamyPurple.toArgb()
            alpha = (intensity * 50).toInt()
            xfermode = PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
        }
        canvas.drawRect(0f, 0f, result.width.toFloat(), result.height.toFloat(), overlayPaint)
        
        // Increase brightness and contrast
        val colorMatrix = ColorMatrix().apply {
            setScale(
                1.0f + intensity * 0.2f,
                1.0f + intensity * 0.1f,
                1.0f + intensity * 0.15f,
                1.0f
            )
        }
        
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(result, 0f, 0f, paint)
        
        // Add soft blur for dreamy effect
        return gaussianBlur(result, (intensity * 3).toInt())
    }
    
    /**
     * Neon glow filter
     */
    fun applyNeonFilter(bitmap: Bitmap, intensity: Float): Bitmap {
val result = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        
        // Increase contrast dramatically
        val contrastMatrix = ColorMatrix().apply {
            val contrast = 1f + intensity * 2f
            val translate = (-.5f * contrast + .5f) * 255f
            set(floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        
        // Add neon colors
        val neonMatrix = ColorMatrix().apply {
            postConcat(contrastMatrix)
            val neonValues = floatArrayOf(
                1.5f, 0f, 0.5f, 0f, 0f,
                0f, 1.5f, 0.5f, 0f, 0f,
                0.5f, 0.5f, 1.5f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
            postConcat(ColorMatrix(neonValues))
        }
        
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(neonMatrix)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        // Add glow effect
        val glowPaint = Paint().apply {
            maskFilter = BlurMaskFilter(intensity * 10, BlurMaskFilter.Blur.OUTER)
            alpha = (intensity * 100).toInt()
        }
        canvas.drawBitmap(result, 0f, 0f, glowPaint)
        
        return result
    }
    
    /**
     * Black and white filter
     */
    fun applyBlackWhiteFilter(bitmap: Bitmap, intensity: Float): Bitmap {
        val result = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
val canvas = Canvas(result)
        
        val colorMatrix = ColorMatrix().apply {
            setSaturation(1f - intensity)
        }
        
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return result
    }
    
    /**
     * Sepia classic filter
     */
    fun applySepiaFilter(bitmap: Bitmap, intensity: Float): Bitmap {
val result = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        
        val sepiaMatrix = floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        
        val colorMatrix = ColorMatrix(sepiaMatrix)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
            alpha = (255 * intensity).toInt()
        }
        
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }
    
    /**
     * Cyberpunk filter with neon colors
     */
    fun applyCyberpunkFilter(bitmap: Bitmap, intensity: Float): Bitmap {
        val result = applyNeonFilter(bitmap, intensity * 0.7f)
        val canvas = Canvas(result)
        
        // Add cyan/magenta split
        val splitPaint = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        }
        
        val cyanMatrix = ColorMatrix().apply {
            setScale(0f, 1f, 1f, 1f)
        }
        splitPaint.colorFilter = ColorMatrixColorFilter(cyanMatrix)
        canvas.save()
        canvas.translate(-intensity * 5, 0f)
        canvas.drawBitmap(bitmap, 0f, 0f, splitPaint)
        canvas.restore()
        
        val magentaMatrix = ColorMatrix().apply {
            setScale(1f, 0f, 1f, 1f)
        }
        splitPaint.colorFilter = ColorMatrixColorFilter(magentaMatrix)
        canvas.save()
        canvas.translate(intensity * 5, 0f)
        canvas.drawBitmap(bitmap, 0f, 0f, splitPaint)
        canvas.restore()
        
        return result
    }
    
    /**
     * Fairy tale filter with soft pastel colors
     */
    fun applyFairyTaleFilter(bitmap: Bitmap, intensity: Float): Bitmap {
        val result = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
val canvas = Canvas(result)
        
        // Apply soft pastel overlay
        val pastelPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, result.width.toFloat(), result.height.toFloat(),
                intArrayOf(
                    android.graphics.Color.argb((intensity * 50).toInt(), 255, 182, 193),
                    android.graphics.Color.argb((intensity * 50).toInt(), 255, 218, 185),
                    android.graphics.Color.argb((intensity * 50).toInt(), 230, 230, 250)
                ),
                null,
                Shader.TileMode.CLAMP
            )
            xfermode = PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
        }
        canvas.drawRect(0f, 0f, result.width.toFloat(), result.height.toFloat(), pastelPaint)
        
        // Soft blur for dreamy effect
        return gaussianBlur(result, (intensity * 2).toInt())
    }
    
    /**
     * Tokyo night filter with neon city vibes
     */
    fun applyTokyoNightFilter(bitmap: Bitmap, intensity: Float): Bitmap {
val result = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        
        // Dark base with enhanced blues and purples
        val nightMatrix = ColorMatrix().apply {
            setScale(
                0.7f - intensity * 0.2f,
                0.7f - intensity * 0.1f,
                1.0f + intensity * 0.3f,
                1.0f
            )
        }
        
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(nightMatrix)
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        // Add neon accents
        val neonOverlay = Paint().apply {
            shader = RadialGradient(
                result.width / 2f, result.height / 2f, 
                min(result.width, result.height) / 2f,
                intArrayOf(
                    android.graphics.Color.argb((intensity * 30).toInt(), 255, 0, 255),
                    android.graphics.Color.argb((intensity * 20).toInt(), 0, 255, 255),
                    android.graphics.Color.TRANSPARENT
                ),
                null,
                Shader.TileMode.CLAMP
            )
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        }
        canvas.drawRect(0f, 0f, result.width.toFloat(), result.height.toFloat(), neonOverlay)
        
        return result
    }
    
    // Helper functions
    
    private fun gaussianBlur(bitmap: Bitmap, radius: Int): Bitmap {
        if (radius < 1) return bitmap
        
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val allocationIn = Allocation.createFromBitmap(renderScript, bitmap)
        val allocationOut = Allocation.createFromBitmap(renderScript, result)
        
        val script = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript))
        script.setRadius(min(radius.toFloat(), 25f))
        script.setInput(allocationIn)
        script.forEach(allocationOut)
        
        allocationOut.copyTo(result)
        return result
    }
    
    private fun addVignetteEffect(canvas: Canvas, width: Int, height: Int, intensity: Float) {
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = min(width, height) / 2f
        
        val vignettePaint = Paint().apply {
            shader = RadialGradient(
                centerX, centerY, radius,
                intArrayOf(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.argb((intensity * 100).toInt(), 0, 0, 0)
                ),
                floatArrayOf(0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vignettePaint)
    }
    
    private fun addFilmGrain(canvas: Canvas, width: Int, height: Int, intensity: Float) {
        val grainPaint = Paint().apply {
            alpha = (intensity * 30).toInt()
        }
        
        // Simple grain effect using random dots
        for (i in 0 until (intensity * 100).toInt()) {
            val x = (Math.random() * width).toFloat()
            val y = (Math.random() * height).toFloat()
            grainPaint.color = if (Math.random() > 0.5) {
                android.graphics.Color.WHITE
            } else {
                android.graphics.Color.BLACK
            }
            canvas.drawPoint(x, y, grainPaint)
        }
    }
    
    fun cleanup() {
        renderScript.destroy()
    }
    
    /**
     * Face detection and enhancement
     */
    data class FaceEnhancement(
        val smoothSkin: Float = 0.5f,
        val enlargeEyes: Float = 0.3f,
        val slimFace: Float = 0.3f,
        val whitenTeeth: Float = 0.4f,
        val clearBlemishes: Boolean = true
    )
    
    /**
     * Apply face enhancement with beauty settings
     */
    fun applyFaceEnhancement(bitmap: Bitmap, settings: FaceEnhancement): Bitmap {
        var result = bitmap
        
        if (settings.smoothSkin > 0) {
            result = applyBeautyFilter(result, settings.smoothSkin)
        }
        
        // Additional face enhancements would require ML Kit Face Detection
        // These are placeholder implementations
        
        return result
    }
}

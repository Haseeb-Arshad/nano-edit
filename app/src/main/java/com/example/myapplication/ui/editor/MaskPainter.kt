package com.example.myapplication.ui.editor

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas as ComposeCanvas

data class MaskStroke(
    val points: MutableList<Offset> = mutableListOf(),
    val radius: Float,
    val erase: Boolean
)

class MaskSession(
    width: Int,
    height: Int
) {
    val mask: ImageBitmap = ImageBitmap(width, height)
    private val strokes = mutableStateListOf<MaskStroke>()

    fun addStroke(stroke: MaskStroke) { strokes += stroke }
    fun undo() { if (strokes.isNotEmpty()) strokes.removeLast() }
    fun clear() { strokes.clear() }
    fun strokes(): List<MaskStroke> = strokes

    fun commitToBitmap() {
        val bitmap = mask.asAndroidBitmap()
        val canvas = android.graphics.Canvas(bitmap)
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = Color.White.toArgb()
            style = android.graphics.Paint.Style.FILL
        }
        // Clear
        canvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.SRC)
        for (s in strokes) {
            paint.xfermode = if (s.erase) android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR) else null
            val pts = s.points
            if (pts.isEmpty()) continue
            if (pts.size == 1) {
                canvas.drawCircle(pts[0].x, pts[0].y, s.radius, paint)
            } else {
                for (i in 1 until pts.size) {
                    val a = pts[i - 1]
                    val b = pts[i]
                    val dx = b.x - a.x
                    val dy = b.y - a.y
                    val dist = kotlin.math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
                    val step = (s.radius * 0.5f).coerceAtLeast(1f)
                    var t = 0f
                    while (t <= dist) {
                        val f = if (dist == 0f) 0f else t / dist
                        val x = a.x + dx * f
                        val y = a.y + dy * f
                        canvas.drawCircle(x, y, s.radius, paint)
                        t += step
                    }
                }
            }
        }
    }
}

private fun drawStroke(scope: DrawScope, stroke: MaskStroke) {
    val color = Color.White
    val blend = if (stroke.erase) BlendMode.Clear else BlendMode.SrcOver
    // Simple Catmull-Rom-ish interpolation by sampling between points
    val pts = stroke.points
    if (pts.size == 1) {
        scope.drawCircle(color, stroke.radius, center = pts[0], blendMode = blend)
        return
    }
    for (i in 1 until pts.size) {
        val a = pts[i - 1]
        val b = pts[i]
        val steps = (a - b).getDistance() / (stroke.radius * 0.5f)
        val n = steps.toInt().coerceAtLeast(1)
        for (t in 0..n) {
            val f = t / n.toFloat()
            val x = a.x + (b.x - a.x) * f
            val y = a.y + (b.y - a.y) * f
            scope.drawCircle(color, stroke.radius, center = Offset(x, y), blendMode = blend)
        }
    }
}

/**
 * MaskPainter overlays a paintable mask. Use commit/export for persistence.
 */
@Composable
fun MaskPainter(
    imageSize: IntSize,
    brushRadiusPx: Float,
    erase: Boolean,
    session: MaskSession = remember(imageSize) { MaskSession(imageSize.width, imageSize.height) },
    onSessionChanged: (MaskSession) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val strokes = remember { session.strokes() }

    Box(Modifier.fillMaxSize().alpha(0.999f)) { // ensure layer
        Canvas(
            Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .pointerInput(erase, brushRadiusPx) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            session.addStroke(MaskStroke(mutableListOf(offset), brushRadiusPx, erase))
                            onSessionChanged(session)
                        },
                        onDrag = { change, drag ->
                            if (strokes.isNotEmpty()) {
                                val last = strokes.last()
                                last.points += change.position
                            }
                        },
                        onDragEnd = {
                            session.commitToBitmap()
                            onSessionChanged(session)
                        }
                    )
                }
        ) {
            // Live preview from strokes list (incremental)
            for (s in strokes) drawStroke(this, s)
        }
    }
}

suspend fun exportMaskPng(session: MaskSession): ByteArray = withContext(Dispatchers.Default) {
    session.commitToBitmap()
    val bmp: Bitmap = session.mask.asAndroidBitmap()
    val out = ByteArrayOutputStream()
    bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
    out.toByteArray()
}

package com.example.myapplication.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

object ImageConvert {
    fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val bmp: Bitmap = when (image.format) {
            ImageFormat.JPEG -> decodeJpeg(image)
            ImageFormat.YUV_420_888 -> decodeYuv(image)
            else -> decodeJpeg(image) // best-effort
        }
        val rotation = image.imageInfo.rotationDegrees
        return if (rotation != 0) rotate(bmp, rotation) else bmp
    }

    private fun decodeJpeg(image: ImageProxy): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun decodeYuv(image: ImageProxy): Bitmap {
        val nv21 = yuv420888ToNv21(image)
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 95, out)
        val jpegBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
    }

    private fun rotate(src: Bitmap, degrees: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees.toFloat())
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    }

    private fun yuv420888ToNv21(image: ImageProxy): ByteArray {
        val width = image.width
        val height = image.height
        val ySize = width * height
        val uvSize = width * height / 2
        val nv21 = ByteArray(ySize + uvSize)

        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        // Copy Y
        var outputPos = 0
        val yBuffer = yPlane.buffer
        val yRowStride = yPlane.rowStride
        val yPixelStride = yPlane.pixelStride
        var row = 0
        while (row < height) {
            var col = 0
            var yPos = row * yRowStride
            if (yPixelStride == 1) {
                yBuffer.position(yPos)
                yBuffer.get(nv21, outputPos, width)
                outputPos += width
            } else {
                while (col < width) {
                    nv21[outputPos++] = yBuffer.get(yPos)
                    col++
                    yPos += yPixelStride
                }
            }
            row++
        }

        // Copy interleaved VU
        val vBuffer = vPlane.buffer
        val uBuffer = uPlane.buffer
        val vRowStride = vPlane.rowStride
        val uRowStride = uPlane.rowStride
        val vPixelStride = vPlane.pixelStride
        val uPixelStride = uPlane.pixelStride

        row = 0
        while (row < height / 2) {
            var col = 0
            var vPos = row * vRowStride
            var uPos = row * uRowStride
            while (col < width) {
                val v = vBuffer.get(vPos)
                val u = uBuffer.get(uPos)
                nv21[outputPos++] = v
                nv21[outputPos++] = u
                col += 2
                vPos += vPixelStride
                uPos += uPixelStride
            }
            row++
        }

        return nv21
    }
}

package com.example.myapplication.ui.camera

import android.content.Context
import android.graphics.BitmapFactory
import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import android.graphics.Bitmap
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.myapplication.utils.ImageConvert
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Modular CameraX binding + capture manager using ProcessCameraProvider.
 */
class CameraCaptureManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private var previewView: PreviewView? = null
    private var imageCapture: ImageCapture? = null
    private var selector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var flashMode: Int = ImageCapture.FLASH_MODE_AUTO

    fun bind(view: PreviewView) {
        previewView = view
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()
                .also { it.setSurfaceProvider(view.surfaceProvider) }

            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(view.display.rotation)
                .build()
                .also { it.flashMode = flashMode }

            try {
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
                imageCapture = capture
            } catch (_: Exception) {
                // leave imageCapture null; capture() will handle
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun setLens(cameraSelector: CameraSelector) {
        selector = cameraSelector
        // Rebind if preview is available
        previewView?.let { bind(it) }
    }

    fun setFlashMode(mode: Int) {
        flashMode = mode
        imageCapture?.flashMode = flashMode
    }

    suspend fun capture(): Bitmap = suspendCancellableCoroutine { cont ->
        val ic = imageCapture
        if (ic == null) {
            cont.resume(createErrorBitmap())
            return@suspendCancellableCoroutine
        }
        ic.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        val bmp = ImageConvert.imageProxyToBitmap(image)
                        cont.resume(bmp)
                    } catch (e: Exception) {
                        // Fallback to file-based capture if in-memory conversion fails
                        try {
                            captureToFileFallback(ic) { fb ->
                                cont.resume(fb ?: createErrorBitmap())
                            }
                        } catch (_: Throwable) {
                            cont.resume(createErrorBitmap())
                        }
                    } finally {
                        try { image.close() } catch (_: Throwable) {}
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    // Fallback to file-based capture when image capture fails
                    try {
                        captureToFileFallback(ic) { fb ->
                            cont.resume(fb ?: createErrorBitmap())
                        }
                    } catch (_: Throwable) {
                        cont.resume(createErrorBitmap())
                    }
                }
            }
        )
    }

    private fun captureToFileFallback(ic: ImageCapture, onResult: (Bitmap?) -> Unit) {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.cacheDir
        val file = java.io.File(dir, "captured_${System.currentTimeMillis()}.jpg")
        val opts = ImageCapture.OutputFileOptions.Builder(file).build()
        ic.takePicture(
            opts,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    onResult(null)
                }
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val bmp = try { BitmapFactory.decodeFile(file.absolutePath) } catch (_: Throwable) { null }
                    // Best-effort: also add to MediaStore for user gallery discoverability
                    try {
                        if (bmp != null) {
                            val name = "AICam_" + System.currentTimeMillis()
                            val values = ContentValues().apply {
                                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AI Camera")
                            }
                            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                            if (uri != null) {
                                context.contentResolver.openOutputStream(uri)?.use { out ->
                                    bmp.compress(Bitmap.CompressFormat.JPEG, 95, out)
                                }
                            }
                        }
                    } catch (_: Throwable) {}
                    try { file.delete() } catch (_: Throwable) {}
                    onResult(bmp)
                }
            }
        )
    }

    private fun createErrorBitmap(): Bitmap {
        return Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.DKGRAY)
        }
    }
}

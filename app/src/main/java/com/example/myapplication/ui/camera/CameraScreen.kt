package com.example.myapplication.ui.camera

import android.Manifest
import android.graphics.Bitmap
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
// CameraController import removed - using direct camera implementation
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.concurrent.Executors
import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import androidx.camera.core.ImageCaptureException
import android.graphics.BitmapFactory
import androidx.camera.core.ImageCapture

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onImageCaptured: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val scope = rememberCoroutineScope()
    // Direct camera implementation without controller
    
    var previewView: PreviewView? by remember { mutableStateOf(null) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    
    LaunchedEffect(cameraPermissionState.status.isGranted) {
        if (cameraPermissionState.status.isGranted) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            val cameraProvider = cameraProviderFuture.get()
            
            val preview = Preview.Builder().build()
            val imageCaptureUseCase = ImageCapture.Builder().build()
            imageCapture = imageCaptureUseCase
            
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCaptureUseCase
                )
                
                previewView?.let { preview.setSurfaceProvider(it.surfaceProvider) }
            } catch (exc: Exception) {
                // Handle camera binding error
            }
        }
    }
    
    if (cameraPermissionState.status.isGranted) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { previewView = it }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // Camera controls
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // AI Analysis Status
                if (false) { // Analysis status not implemented yet
                    Card(
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyzing image...")
                        }
                    }
                }

                // Capture button
                FloatingActionButton(
                    onClick = {
                        val capture = imageCapture ?: return@FloatingActionButton
                        // Capture to app file and decode, also save to MediaStore so it appears in Gallery
                        val file = java.io.File(
                            context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES) ?: context.cacheDir,
                            "captured_${System.currentTimeMillis()}.jpg"
                        )
                        val options = ImageCapture.OutputFileOptions.Builder(file).build()
                        capture.takePicture(
                            options,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onError(exception: ImageCaptureException) {
                                    // Fallback placeholder on error
                                    val placeholderBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
                                    onImageCaptured(placeholderBitmap)
                                }
                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                    val bmp = BitmapFactory.decodeFile(file.absolutePath)
                                    if (bmp != null) {
                                        // Persist to MediaStore (Pictures/AI Camera)
                                        try {
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
                                        } catch (_: Throwable) {}
                                        onImageCaptured(bmp)
                                    } else {
                                        val placeholderBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
                                        onImageCaptured(placeholderBitmap)
                                    }
                                }
                            }
                        )
                    },
                    shape = CircleShape,
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Capture",
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Suggestion chips (if available)
                // TODO: Implement analysis results properly
            }
        }
    } else {
        // Permission request UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Camera permission is required to use this feature",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { cameraPermissionState.launchPermissionRequest() }
            ) {
                Text("Grant Permission")
            }
        }
    }
}

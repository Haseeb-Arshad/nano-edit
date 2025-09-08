package com.example.myapplication

import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.FloatingActionButton
import com.example.myapplication.ui.modern.ModernReviewScreen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.controller.CameraController
import com.example.myapplication.controller.EditController
import com.example.myapplication.controller.SuggestionController
import com.example.myapplication.data.CameraUiState
import com.example.myapplication.data.EditRepository
import com.example.myapplication.ui.navigation.NavRoutes
import com.example.myapplication.ui.theme.AppTheme
import com.example.myapplication.utils.shareImage
import com.example.myapplication.data.PreferencesRepository
import com.example.myapplication.utils.loadBitmap
import com.example.myapplication.utils.saveBitmapToGallery
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.LifecycleOwner
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var editRepository: EditRepository
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Set status bar and navigation bar colors
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        
        setContent { ModernAppRoot(editRepository) }
    }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ModernAppRoot(editRepository: EditRepository) {
    AppTheme {
        val nav = rememberNavController()
        val snackbar = remember { SnackbarHostState() }
        val editController = remember { com.example.myapplication.controller.EditController(editRepository) }
        val ctx = LocalContext.current

        Box(modifier = Modifier.fillMaxSize()) {
            // Animated gradient background
            com.example.myapplication.ui.components.AnimatedGradientBackground(
                modifier = Modifier.fillMaxSize()
            )
            
            NavHost(
                navController = nav,
                startDestination = NavRoutes.Camera,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(
                    NavRoutes.Camera
                ) {
                    com.example.myapplication.ui.camera.ModernCameraScreen(
                        onImageCaptured = { bitmap ->
                            // Save bitmap and navigate to review
                            val uri = saveBitmapTemporary(ctx, bitmap)
                            nav.navigate(NavRoutes.review(Uri.encode(uri.toString())))
                        },
                        onNavigateToGallery = {
                            nav.navigate(NavRoutes.Gallery)
                        },
                        onNavigateToSettings = {
                            nav.navigate(NavRoutes.Settings)
                        }
                    )
                }
                
                composable(
                    route = NavRoutes.Review,
                    arguments = listOf(navArgument("uri") { type = NavType.StringType })
                ) { backStackEntry ->
                    val arg = backStackEntry.arguments?.getString("uri")
                    val uri = arg?.let { Uri.parse(Uri.decode(it)) }
                    // Auto Smart Enhance preference
                    val ctx2 = LocalContext.current
                    val prefs = remember { com.example.myapplication.data.PreferencesRepository(ctx2) }
                    val autoLift by prefs.autoSceneLiftFlow.collectAsState(initial = false)
                    var didAuto by remember { mutableStateOf(false) }
                    LaunchedEffect(uri, autoLift) {
                        if (uri != null && autoLift && !didAuto) {
                            didAuto = true
                            nav.navigate(NavRoutes.editor(Uri.encode(uri.toString()), autolift = true))
                        }
                    }
                    com.example.myapplication.ui.modern.ModernReviewScreen(
                        uri = uri,
                        repository = editRepository,
                        onEdit = {
                            nav.navigate(NavRoutes.editor(Uri.encode(uri.toString())))
                        },
                        onSceneLift = {
                            nav.navigate(NavRoutes.editor(Uri.encode(uri.toString()), autolift = true))
                        },
                        onBack = { nav.popBackStack() }
                    )
                }
                
composable(
                    route = NavRoutes.Editor,
                    arguments = listOf(
                        navArgument("uri") { type = NavType.StringType },
                        navArgument("autolift") { type = NavType.BoolType; defaultValue = false }
                    )
                ) { backStackEntry ->
                    val arg = backStackEntry.arguments?.getString("uri")
                    val uri = arg?.let { Uri.parse(Uri.decode(it)) }
                    val autolift = backStackEntry.arguments?.getBoolean("autolift") ?: false
                    com.example.myapplication.ui.modern.ModernEditorScreen(
                        src = uri,
                        controller = editController,
                        onBack = { nav.popBackStack() },
                        autoSceneLift = autolift
                    )
                }
                
                composable(
                    NavRoutes.Gallery
                ) {
                    com.example.myapplication.ui.modern.ModernGalleryScreen(
                        onBack = { nav.popBackStack() },
                        onOpen = { uri ->
                            nav.navigate(NavRoutes.review(Uri.encode(uri.toString())))
                        }
                    )
                }
                
                composable(
                    NavRoutes.Settings
                ) {
                    com.example.myapplication.ui.modern.ModernSettingsScreen(
                        onBack = { nav.popBackStack() }
                    )
                }
            }
            
            // Snackbar host with modern styling
            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) { data ->
                com.example.myapplication.ui.components.GlassmorphicCard(
                    modifier = Modifier.padding(16.dp),
                    cornerRadius = 16.dp
                ) {
                    Text(
                        text = data.visuals.message,
                        modifier = Modifier.padding(16.dp),
                        color = Color.White
                    )
                }
            }
        }
    }
}

// Helper function to save bitmap temporarily
private fun saveBitmapTemporary(context: android.content.Context, bitmap: Bitmap): Uri {
    val file = File(context.cacheDir, "temp_${System.currentTimeMillis()}.jpg")
    try {
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        stream.flush()
        stream.close()
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return Uri.fromFile(file)
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun CameraScreenHost(
    controller: CameraController,
    suggestionController: SuggestionController,
    onCaptured: (Uri) -> Unit
) {
    val permission = rememberPermissionState(Manifest.permission.CAMERA)
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var granted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { permission.launchPermissionRequest() }
    LaunchedEffect(permission.status) {
        granted = permission.status.isGranted
        controller.updatePermission(granted)
    }
    Box(Modifier.fillMaxSize()) {
        if (granted) {
            CameraScreen(controller = controller, suggestions = suggestionController, onCaptured = onCaptured, lifecycleOwner = lifecycleOwner)
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Camera permission is required", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                FilledTonalButton(onClick = { permission.launchPermissionRequest() }, contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)) {
                    Text("Grant Camera Access")
                }
            }
        }
    }
}

// Stub composables below; implemented in separate files for clarity in larger projects
@Composable
fun CameraScreen(
    controller: CameraController,
    suggestions: SuggestionController,
    onCaptured: (Uri) -> Unit,
    lifecycleOwner: LifecycleOwner
) {
    val ctx = LocalContext.current
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    val state: CameraUiState = controller.state.collectAsState(initial = CameraUiState()).value
    val preview = remember { androidx.camera.view.PreviewView(ctx) }

    LaunchedEffect(Unit) {
        suggestions.computeSuggestions(context = ctx, uri = null)
        controller.bindPreview(
            context = ctx,
            lifecycleOwner = lifecycleOwner,
            previewView = preview
        )
    }

    Box(Modifier.fillMaxSize()) {
        // Camera preview
        androidx.compose.ui.viewinterop.AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { preview }
        )

        // Top glass bar (placeholder actions)
        com.example.myapplication.ui.components.GlassCard(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(12.dp)
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("AI Camera", style = MaterialTheme.typography.titleMedium)
            }
        }

        // Suggestion chips (glass buttons)
        val chips = suggestions.chips.collectAsState(initial = emptyList()).value
        AnimatedVisibility(visible = chips.isNotEmpty(), enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            androidx.compose.foundation.layout.Row {
chips.forEach { chip ->
                    com.example.myapplication.ui.components.GlassButton(
                        onClick = { /* future: prefill editor prompt */ },
                        modifier = Modifier.padding(end = 8.dp).semantics { this.contentDescription = chip.title }
                    ) {
                        Text(chip.title)
                    }
                }
            }
        }

        // Shutter dock (glass)
        com.example.myapplication.ui.components.GlassCard(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        ) {
            Row(Modifier.padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
com.example.myapplication.ui.components.ShutterButtonClassic(
                    isCapturing = state.isCapturing,
                    onClick = { haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress); controller.capture(ctx) { uri -> uri?.let(onCaptured) } },
                    modifier = Modifier
                        .padding(8.dp)
                        .then(Modifier.size(96.dp))
                )
            }
        }

        // Error bubble
        AnimatedVisibility(visible = state.error != null, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.TopCenter).padding(16.dp)) {
            com.example.myapplication.ui.components.GlassCard { Row(Modifier.padding(12.dp)) { Text(text = state.error ?: "", color = MaterialTheme.colorScheme.error) } }
        }
    }
}

@Composable
fun ReviewScreen(uri: Uri?, onEdit: () -> Unit, onSceneLift: () -> Unit) {
    val ctx = LocalContext.current
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    var original by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var enhanced by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var mode by remember { mutableStateOf(ReviewMode.Compare) }

    LaunchedEffect(uri) {
        if (uri != null) {
            val bmp = com.example.myapplication.utils.loadBitmap(ctx, uri.toString())
            original = bmp
            enhanced = bmp?.let { com.example.myapplication.utils.quickEnhance(it, 0.6f) }
        }
    }

    Column(Modifier.fillMaxSize()) {
        com.example.myapplication.ui.components.GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                com.example.myapplication.ui.components.GlassButton(
                    onClick = {
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        mode = ReviewMode.Compare
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Compare", color = if (mode == ReviewMode.Compare) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
                com.example.myapplication.ui.components.GlassButton(
                    onClick = {
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        mode = ReviewMode.SideBySide
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Side-by-side", color = if (mode == ReviewMode.SideBySide) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
            val o = original
            val e = enhanced
            if (o != null && e != null) {
                androidx.compose.animation.Crossfade(targetState = mode) { currentMode ->
                    when (currentMode) {
                        ReviewMode.Compare -> {
                            com.example.myapplication.ui.components.CompareSlider(
                                modifier = Modifier.fillMaxSize().padding(8.dp),
original = o.asImageBitmap(),
enhanced = e.asImageBitmap()
                            )
                        }
                        ReviewMode.SideBySide -> {
                            Row(Modifier.fillMaxSize().padding(8.dp)) {
                                androidx.compose.foundation.Image(
bitmap = o.asImageBitmap(),
                                    contentDescription = "Original",
                                    modifier = Modifier.weight(1f).fillMaxSize()
                                )
                                androidx.compose.foundation.Image(
bitmap = e.asImageBitmap(),
                                    contentDescription = "Enhanced",
                                    modifier = Modifier.weight(1f).fillMaxSize()
                                )
                            }
                        }
                    }
                }
            } else {
                com.example.myapplication.ui.components.GlassCard { Box(Modifier.padding(16.dp)) { Text("Loading preview…") } }
            }
        }

        com.example.myapplication.ui.components.GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                com.example.myapplication.ui.components.GlassButton(
                    onClick = {
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        onSceneLift()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Smart Enhance")
                }
                com.example.myapplication.ui.components.GlassButton(
                    onClick = {
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onEdit()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Open Editor")
                }
                com.example.myapplication.ui.components.GlassButton(
                    onClick = {
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        uri?.let { shareImage(ctx, it) }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Share Original")
                }
            }
        }
    }
}

enum class ReviewMode { Compare, SideBySide }


@Composable fun GalleryScreen() { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Gallery (todo)") } }

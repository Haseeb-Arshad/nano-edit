package com.example.myapplication

import android.Manifest
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.LifecycleOwner

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var editRepository: EditRepository
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AppRoot(editRepository) }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AppRoot(editRepository: EditRepository) {
    AppTheme {
        val nav = rememberNavController()
        val snackbar = remember { SnackbarHostState() }
        val cameraController = remember { CameraController() }
        val suggestionController = remember { SuggestionController() }
        val editController = remember { com.example.myapplication.controller.EditController(editRepository) }

        Scaffold(
            topBar = {
                TopAppBar(title = { Text("AI Camera") }, actions = {
                    IconButton(onClick = { nav.navigate(NavRoutes.Settings) }) { /* Settings icon omitted for brevity */ }
                })
            },
            snackbarHost = { SnackbarHost(snackbar) }
        ) { padding ->
            NavHost(
                navController = nav,
                startDestination = NavRoutes.Camera,
                modifier = Modifier.padding(padding)
            ) {
                composable(NavRoutes.Camera) {
                    CameraScreenHost(cameraController, suggestionController) { uri ->
                        nav.navigate(NavRoutes.review(Uri.encode(uri.toString())))
                    }
                }
                composable(
                    route = NavRoutes.Review,
                    arguments = listOf(navArgument("uri") { type = NavType.StringType })
                ) { backStackEntry ->
                    val arg = backStackEntry.arguments?.getString("uri")
                    val uri = arg?.let { Uri.parse(Uri.decode(it)) }
                    ReviewScreen(uri = uri, onEdit = {
                        nav.navigate(NavRoutes.editor(Uri.encode(uri.toString())))
                    })
                }
                composable(
                    route = NavRoutes.Editor,
                    arguments = listOf(navArgument("uri") { type = NavType.StringType })
                ) { backStackEntry ->
                    val arg = backStackEntry.arguments?.getString("uri")
                    val uri = arg?.let { Uri.parse(Uri.decode(it)) }
                    EditorScreenHost(editController = editController, src = uri)
                }
                composable(NavRoutes.Gallery) {
                    com.example.myapplication.ui.screens.GalleryScreen()
                }
                composable(NavRoutes.Settings) {
                    com.example.myapplication.ui.screens.SettingsScreen()
                }
            }
        }
    }
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
    LaunchedEffect(permission.status.isGranted) {
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
        androidx.compose.ui.viewinterop.AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { preview }
        )
        val chips = suggestions.chips.collectAsState(initial = emptyList()).value
        if (chips.isNotEmpty()) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
            ) {
                chips.forEach { chip ->
                    androidx.compose.material3.AssistChip(
                        onClick = { /* future: prefill editor prompt */ },
                        label = { Text(chip.title) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
        }
        androidx.compose.material3.ExtendedFloatingActionButton(
            modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
            onClick = {
                controller.capture(ctx) { uri -> uri?.let(onCaptured) }
            },
            text = { Text(if (state.isCapturing) "Capturing…" else "Shutter") }
        )
        AnimatedVisibility(visible = state.error != null, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.TopCenter).padding(16.dp)) {
            Text(text = state.error ?: "", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun ReviewScreen(uri: Uri?, onEdit: () -> Unit) {
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Review", style = MaterialTheme.typography.titleLarge)
        Text(text = uri?.toString() ?: "No image")
        Row(Modifier.padding(top = 16.dp)) {
            FilledTonalButton(onClick = onEdit) { Text("Edit") }
            androidx.compose.material3.OutlinedButton(onClick = { uri?.let { shareImage(ctx, it) } }, modifier = Modifier.padding(start = 12.dp)) { Text("Share") }
        }
    }
}

@Composable
fun EditorScreenHost(editController: com.example.myapplication.controller.EditController, src: Uri?) {
    val state = editController.state.collectAsState(initial = com.example.myapplication.data.EditUiState()).value
    val ctx = LocalContext.current
    val prefs = remember { PreferencesRepository(ctx) }
    val offline = prefs.offlineFlow.collectAsState(initial = false).value
    val scope = rememberCoroutineScope()
    LaunchedEffect(src) { src?.let { editController.setSource(it) } }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Editor", style = MaterialTheme.typography.titleLarge)
        androidx.compose.material3.OutlinedTextField(
            value = state.prompt,
            onValueChange = editController::setPrompt,
            modifier = Modifier.padding(top = 12.dp),
            label = { Text("Prompt") }
        )
        FilledTonalButton(onClick = { editController.applyEdit(offline = offline) }, modifier = Modifier.padding(top = 16.dp)) {
            Text(if (state.isLoading) "Applying…" else "Apply Edit")
        }
        if (offline) {
            Text("Offline mode: using local preview only", color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 8.dp))
        }
        state.error?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
        state.resultUrl?.let { url ->
            Text("Result ready", modifier = Modifier.padding(top = 12.dp))
            OutlinedButton(onClick = {
                scope.launch {
                    val bmp = loadBitmap(ctx, url)
                    if (bmp != null) {
                        saveBitmapToGallery(ctx, bmp, "AICam_Edit")
                    }
                }
            }, modifier = Modifier.padding(top = 8.dp)) { Text("Save to Gallery") }
        }
    }
}

@Composable fun GalleryScreen() { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Gallery (todo)") } }

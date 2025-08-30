# Android AI Camera App — Build Guide (MVC)

This guide walks you from zero to a polished Android camera + AI image editing app using an MVC architecture. It covers setup, UI/UX, camera pipeline, integrating Google's “nano-banana” image editing API (placeholder name), performance, testing, and release.

> Note: “nano-banana” here is treated as a remote image-editing API providing text-to-image and image-editing endpoints. If the official SDK differs, adapt the integration layer (endpoints/models) accordingly.

---

## Status Snapshot

- Project config (Compose, Material 3, minSdk 26, target/compile set): ✅ Configured in gradle
- Secrets plumbing (`BuildConfig.NB_API_KEY`, `NB_BASE_URL`): ✅ Present in `defaultConfig`
- Dependencies:
  - Compose/M3, Activity, BOM: ✅
  - CameraX: ✅
  - Retrofit/OkHttp + logging: ✅
  - Coroutines + ViewModel: ✅
  - Coil (image): ✅
- Navigation Compose: ✅
- DataStore: ✅ (basic settings screen wired)
- DI (Hilt): ✅ (Application + Network module)
  - ML Kit on-device labels: ❌ Not added (TFLite libs present)
- Source packages (`controller/`, `data/`, `network/`, `ui/`, `utils/`): ✅ Created
- Features:
  - Preview/capture: ✅
  - Editor (baseline prompt/apply + offline): ✅ (API calls stubbed to real endpoint)
  - Suggestions (ML Kit labels → chips): ✅ (baseline)
  - Save/share: ✅ (Editor save, Review share)
  - Gallery: ✅ (basic list)
- Tests/lint/release polish: ❌ Not started

See `docs/AI-Camera-Tasks.md` for the detailed, step-by-step execution list with checkboxes and prompts.

---

## TL;DR Checklist

- Project: Jetpack Compose + Material 3; Kotlin; minSdk 26+; target latest.
- Architecture (MVC):
  - Model: data models, repositories, network IO.
  - View: Compose UI (screens, components, theme).
  - Controller: feature controllers (camera, edit, suggestions) coordinating Model↔View.
- Key libs: CameraX, Retrofit/OkHttp, Kotlin Coroutines, Hilt (DI), Coil, Navigation, DataStore, ML Kit (optional on-device suggestions).
- Core features:
  - Camera preview/capture (CameraX) + instant suggestions (on-device or API).
  - AI edits via “nano-banana” (prompt, mask, filters, enhancements).
  - Minimal, elegant UI with smooth micro-interactions.
  - Save/share to MediaStore; in-app recent edits gallery.
- Guardrails: permission UX, privacy (local analysis first), caching, background retries, graceful error states.

---

## 1) Prerequisites

- Android Studio (latest stable), Android SDK, Android Gradle Plugin.
- Kotlin familiarity (we’ll keep examples simple).
- A “nano-banana” API key and base URL (placeholder).

---

## 2) Create the Project

1. New Project → Empty Activity (Compose) → Kotlin.
2. Set `minSdk` 26+, `compileSdk` latest, `targetSdk` latest.
3. Enable Compose, Material 3, Kotlin DSL (`build.gradle.kts`).
4. Package name: e.g., `com.example.aicamera`.

---

## 3) Dependencies (add to `app/build.gradle.kts`)

```kotlin
plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("com.google.dagger.hilt.android") version "2.51" apply true
  kotlin("kapt")
}

android {
  namespace = "com.example.aicamera"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.example.aicamera"
    minSdk = 26
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"
    vectorDrawables { useSupportLibrary = true }
  }

  buildFeatures { compose = true }
  composeOptions { kotlinCompilerExtensionVersion = "1.5.15" }
}

dependencies {
  // Compose + Material 3
  implementation(platform("androidx.compose:compose-bom:2024.10.01"))
  implementation("androidx.compose.material3:material3")
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-tooling-preview")
  debugImplementation("androidx.compose.ui:ui-tooling")
  implementation("androidx.activity:activity-compose:1.9.2")
  implementation("androidx.navigation:navigation-compose:2.8.2")

  // CameraX
  val camerax = "1.4.0"
  implementation("androidx.camera:camera-core:$camerax")
  implementation("androidx.camera:camera-camera2:$camerax")
  implementation("androidx.camera:camera-lifecycle:$camerax")
  implementation("androidx.camera:camera-view:$camerax")

  // Image loading
  implementation("io.coil-kt:coil-compose:2.6.0")

  // Networking
  implementation("com.squareup.retrofit2:retrofit:2.11.0")
  implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
  implementation("com.squareup.okhttp3:okhttp:4.12.0")
  implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

  // Coroutines
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

  // DI
  implementation("com.google.dagger:hilt-android:2.51")
  kapt("com.google.dagger:hilt-android-compiler:2.51")

  // Preferences
  implementation("androidx.datastore:datastore-preferences:1.1.1")

  // Optional: ML Kit on-device labeler for instant suggestions
  implementation("com.google.mlkit:image-labeling:17.0.8")
}
```

---

## 4) MVC Architecture

- Model: domain models (Photo, EditRequest, EditResult), repositories (CameraRepo, EditRepo), data sources (network, local), DTOs.
- View: Jetpack Compose UI screens/components, theming, animations.
- Controller: Feature controllers (Kotlin classes) that hold state and coordinate Model↔View. Activities/Fragments own controllers and pass callbacks/state to Composables.

Recommended packages:

```
com.example.aicamera
├─ model/            // domain models
├─ data/             // repositories, local (MediaStore), remote (nano-banana)
├─ network/          // Retrofit API, DTOs
├─ controller/       // CameraController, EditController, SuggestionController
├─ camera/           // CameraX utilities
├─ view/             // Compose screens & components
├─ di/               // Hilt modules
└─ util/             // common helpers
```

Why MVC here: clear separation, controllers are the single coordination point; views stay declarative; models/data stay testable.

---

## 5) UX Design (Minimal, Sleek)

- Colors: monochrome base with a single accent (e.g., deep teal). Support dark mode.
- Typography: large bold titles, medium labels, minimal chrome.
- Components: bottom-aligned shutter; floating suggestion chips; subtle depth and motion.
- Gestures: swipe up to edit; pinch to zoom; long-press to compare before/after.
- Animations: 
  - Capture ripple on shutter press.
  - Suggestion chips fade/slide in.
  - Editor toolbars slide with overscroll parallax.
- Accessibility: minimum 4.5:1 contrast for body text; content descriptions; large tap targets.

Screens:
- Camera: full-bleed preview, shutter, mode toggle, suggestion chips.
- Review: shows captured photo; actions: Edit, Save, Share.
- Editor: prompt input, mask brush, presets, intensity slider, apply; progress overlay.
- Gallery: recent edits; open any item to re-edit.
- Settings: offline mode, default quality, watermark toggle.

---

## 6) Camera Pipeline (CameraX)

Flow:
1. Bind `Preview` + `ImageCapture` use cases.
2. Optionally bind `ImageAnalysis` for instant suggestions (on-device labeler or low-res API ping post-capture).
3. On shutter: capture to file; write EXIF; add to MediaStore; navigate to Review.
4. Run suggestion analysis on the captured image; display chips.

Key notes:
- Use `CameraSelector.DEFAULT_BACK_CAMERA`.
- For analysis, keep format YUV_420_888 → convert to Bitmap only if needed.
- Keep analyzer at small resolution (e.g., 640px wide) for performance.
- Respect lifecycle: bind/unbind in `onResume`/`onPause` via `LifecycleOwner`.

---

## 7) “nano-banana” API Integration (placeholder)

Assumptions:
- REST endpoints, API key in `Authorization: Bearer <KEY>`.
- Core endpoints:
  - POST `/v1/edits` — apply text prompt + optional mask + options.
  - GET `/v1/jobs/{id}` — poll job status (if async).
  - GET `/v1/assets/{id}` — fetch result image.

Retrofit API (example):
```kotlin
// network/NanoBananaApi.kt
interface NanoBananaApi {
  @POST("/v1/edits")
  suspend fun createEdit(@Body body: EditRequestDto): EditJobDto

  @GET("/v1/jobs/{id}")
  suspend fun getJob(@Path("id") id: String): EditJobDto
}
```

DTOs (example):
```kotlin
data class EditRequestDto(
  val imageUrl: String?,        // or multipart if uploading
  val maskUrl: String?,         // optional
  val prompt: String,
  val strength: Float? = null,
  val upscaling: Boolean = false,
  val size: String = "original"
)

data class EditJobDto(
  val id: String,
  val status: String, // queued, running, done, failed
  val resultUrl: String? = null,
  val error: String? = null
)
```

OkHttp with logging + auth header:
```kotlin
// di/NetworkModule.kt
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
  @Provides @Singleton
  fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
    .addInterceptor(HttpLoggingInterceptor().apply { level = BODY })
    .addInterceptor { chain ->
      val req = chain.request().newBuilder()
        .addHeader("Authorization", "Bearer ${'$'}{BuildConfig.NB_API_KEY}")
        .build()
      chain.proceed(req)
    }
    .build()

  @Provides @Singleton
  fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
    .baseUrl(BuildConfig.NB_BASE_URL)
    .client(client)
    .addConverterFactory(MoshiConverterFactory.create())
    .build()

  @Provides @Singleton
  fun provideApi(retrofit: Retrofit): NanoBananaApi = retrofit.create(NanoBananaApi::class.java)
}
```

Repository:
```kotlin
class EditRepository @Inject constructor(
  private val api: NanoBananaApi
) {
  suspend fun submitEdit(req: EditRequestDto): String = api.createEdit(req).id
  suspend fun pollResult(id: String): EditJobDto = api.getJob(id)
}
```

Security:
- Store `NB_API_KEY` via `local.properties` → `buildConfigField` at build time; do not hardcode.
- Respect user privacy: only upload after user consent; anonymize if possible.

---

## 8) Controllers (MVC)

Purpose: hold UI state, orchestrate use cases, call Repositories, expose immutable view state + callbacks to Views.

- `CameraController`
  - State: camera facing, flash, zoom, isCapturing, lastSuggestion.
  - Actions: startPreview, capturePhoto, analyzeForSuggestion.
  - Coordinates with: `SuggestionController`.

- `SuggestionController`
  - State: List<SuggestionChip>, isLoading.
  - Actions: from on-device labeler or remote classify; map to preset edits.

- `EditController`
  - State: currentImage, mask, prompt, progress, resultImage, error.
  - Actions: applyPreset, updatePrompt, applyEdit (calls `EditRepository`), cancel, save.

Design tips:
- Expose `StateFlow`/`MutableStateFlow` internally; Views collect as Compose state.
- Keep controllers UI-free; only pure Kotlin and data calls.

---

## 9) Views (Compose)

Top-level navigation graph:
- `CameraScreen` → `ReviewScreen` → `EditorScreen` → `GalleryScreen` → `SettingsScreen`.

Example skeleton:
```kotlin
@Composable
fun CameraScreen(
  state: CameraUiState,
  onShutter: () -> Unit,
  onToggleFlash: () -> Unit,
  onOpenEditor: (Uri) -> Unit,
  suggestions: List<Suggestion>
) { /* PreviewView interop + chrome + chips */ }

@Composable
fun EditorScreen(
  state: EditUiState,
  onPromptChange: (String) -> Unit,
  onApplyEdit: () -> Unit,
  onSave: () -> Unit
) { /* Image + prompt + tools + progress overlay */ }
```

Theme:
- Material 3 dynamic color with a single custom accent.
- ElevatedCard for chips; small shadow levels; subtle motion specs.

---

## 10) Instant Suggestions

Option A: On-device with ML Kit Image Labeling
1. Run after capture on a downscaled bitmap.
2. Map labels → suggestion presets (e.g., "food" → Warm Tone + Clarity; "portrait" → Skin smoothing + Bokeh; "document" → Contrast + B/W).

Option B: Remote classify endpoint (if nano-banana supports)
1. Upload thumbnail.
2. Use result to prefill suggested edits.

UI surfacing:
- Show up to 3 chips; tap to preview preset; long-press to see rationale.

---

## 11) Editing Tools

- AI Edits (via API): prompt-based edit; optional mask; upscaling; enhance.
- Local Edits (fast, offline): crop/rotate, simple filters (exposure, contrast, saturation), blur vignette via `RenderEffect`.
- Diff viewer: long-press to toggle before/after.

Apply flow:
1. User sets prompt/mask → `EditController.applyEdit()`
2. Submit job; show progress (indeterminate → % if supported)
3. Poll result; load output into UI (Coil w/ crossfade)
4. Allow stacking edits with versioning (keep original URI)

---

## 12) Saving & Sharing

- Save as JPEG/PNG/WebP to `MediaStore.Images` with meaningful filename and EXIF copy.
- Keep app-private cache for intermediates; clean up with `WorkManager`.
- Share via Android Sharesheet with proper `FileProvider` URI.

---

## 13) Permissions & Privacy

- Camera: request just-in-time; explain rationale screen on first launch.
- Storage: use `MediaStore`; avoid broad storage permissions.
- Network: show data usage; offline toggle; clear what gets uploaded.
- Telemetry: opt-in only; no PII.

---

## 14) Performance

- Keep analyzer resolution low; throttle to ~10fps for labels.
- Avoid bitmap copies; reuse buffers; use `ImageCapture` to file instead of in-memory when possible.
- Use caching headers for API; backoff on failures.
- Pre-warm controllers and critical composables on app open.

---

## 15) Testing Strategy

- Unit: repositories (mock API), controllers (state reducers), mappers.
- UI: Compose tests for screen states; golden/screenshot tests for critical layouts.
- Instrumented: CameraX smoke test (where feasible), MediaStore save, navigation flows.
- Network: contract tests against a local mock server (e.g., MockWebServer).

---

## 16) Release Readiness

- App icon, splash, adaptive icons (light/dark).
- Play Console: privacy policy, data safety forms (explain network uploads for edits).
- ProGuard/R8: keep models for Retrofit/Moshi.
- Feature flags for experimental tools.

---

## 17) Milestones (Suggested)

1. Project skeleton, DI, theme, nav (0.5–1 day)
2. Camera preview/capture + save (1–2 days)
3. On-device suggestions + chips (1 day)
4. Editor scaffolding + local edits (2–3 days)
5. API integration (submit/poll/download) (2–3 days)
6. Polish: animations, accessibility, empty/error states (1–2 days)
7. Testing + release prep (2–3 days)

---

## 18) Implementation Notes & Snippets

PreviewView + Compose interop:
```kotlin
@Composable
fun CameraPreview(bindingLifecycleOwner: LifecycleOwner) {
  AndroidView(factory = { context ->
    PreviewView(context).apply { 
      scaleType = PreviewView.ScaleType.FILL_CENTER 
    }
  }, update = { previewView ->
    val cameraProvider = ProcessCameraProvider.getInstance(previewView.context).get()
    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
    val selector = CameraSelector.DEFAULT_BACK_CAMERA
    cameraProvider.unbindAll()
    cameraProvider.bindToLifecycle(bindingLifecycleOwner, selector, preview)
  })
}
```

Submitting an edit job (controller → repo):
```kotlin
class EditController @Inject constructor(private val repo: EditRepository) {
  private val _state = MutableStateFlow(EditUiState())
  val state: StateFlow<EditUiState> = _state

  suspend fun applyEdit(imageUrl: String, prompt: String) {
    _state.update { it.copy(isLoading = true, error = null) }
    runCatching {
      val id = repo.submitEdit(EditRequestDto(imageUrl = imageUrl, maskUrl = null, prompt = prompt))
      var job = repo.pollResult(id)
      while (job.status == "queued" || job.status == "running") {
        delay(1000)
        job = repo.pollResult(id)
      }
      if (job.status == "done" && job.resultUrl != null) {
        _state.update { it.copy(isLoading = false, resultUrl = job.resultUrl) }
      } else {
        error("${'$'}{job.error ?: "Unknown error"}")
      }
    }.onFailure { e ->
      _state.update { it.copy(isLoading = false, error = e.message) }
    }
  }
}
```

Saving to MediaStore:
```kotlin
fun saveBitmapToGallery(context: Context, bmp: Bitmap, displayName: String): Uri? {
  val values = ContentValues().apply {
    put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.jpg")
    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AI Camera")
  }
  val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
  uri?.let { outUri ->
    context.contentResolver.openOutputStream(outUri)?.use { os ->
      bmp.compress(Bitmap.CompressFormat.JPEG, 95, os)
    }
  }
  return uri
}
```

---

## 19) Next Steps (Actionable)

- Follow the checklist in `docs/AI-Camera-Tasks.md` in order.
- When a step lands, tick it in the checklist and keep moving.
- Prioritize Camera preview/capture, then Editor + API integration, then polish.

---

## 20) Design Language Snapshot

- Accent: #12C2A9 (Teal), Base: near-black/white.
- Iconography: simple duotone; minimal labels.
- Motion: 120–200ms ease-in-out; no abrupt transitions.
- Haptics: light on shutter and edit apply.

---

If you want, I can scaffold the packages, controllers, network layer, and Compose screens directly in this project next.

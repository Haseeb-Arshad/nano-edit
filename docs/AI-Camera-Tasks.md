# AI Camera App — Execution Tasks

Clean, ordered, and implementation-ready. Tick as you go. Keep commits small and focused.

Legend: [x] done • [ ] pending • 🛠 implementation • 🎯 acceptance • 💡 prompt

---

## 0) Foundation & Secrets

- [x] Add BuildConfig fields for secrets
  - 🛠 Present in `app/build.gradle.kts` (`NB_API_KEY`, `NB_BASE_URL`).
  - 🎯 Build generates `BuildConfig.NB_API_KEY` and `BuildConfig.NB_BASE_URL`.
  - 💡 Prompt: “Verify `defaultConfig` defines `buildConfigField` for `NB_API_KEY` and `NB_BASE_URL` pulling from `local.properties` or env.”

- [x] Add Navigation Compose dependency
  - 🛠 Add `androidx.navigation:navigation-compose` aligned to Compose BOM.
  - 🎯 Nav host available to wire screens end-to-end.
  - 💡 Prompt: “Edit `app/build.gradle.kts` dependencies to include `androidx.navigation:navigation-compose` using the project BOM.”

- [x] Add DataStore dependency (prefs)
  - 🛠 Add `androidx.datastore:datastore-preferences`.
  - 🎯 App can persist simple settings (offline mode, quality).
  - 💡 Prompt: “Add DataStore Preferences dependency to `app/build.gradle.kts` and sync.”

- [x] Add Hilt (DI)
  - 🛠 Apply plugin, add deps, create base `@HiltAndroidApp` Application and one `@Module`.
  - 🎯 DI graph compiles; app launches with Hilt.
  - 💡 Prompt: “Integrate Hilt: plugin, deps, Application class, `NetworkModule` stub with `OkHttpClient` + `Retrofit` providers.”

---

## 1) Project Bootstrap (UI + Structure)

- [x] Create package structure
  - 🛠 `com.example.myapplication/` with: `controller/`, `data/`, `network/`, `ui/`, `utils/`, `ai/`.
  - 🎯 Empty stubs compile; package layout matches plan.
  - 💡 Prompt: “Create Kotlin packages and placeholder files per architecture. No logic yet.”

- [x] App entry and theme
  - 🛠 `MainActivity` with Compose `setContent`, M3 theme, `NavHost` scaffold.
  - 🎯 App launches to a placeholder Camera screen route.
  - 💡 Prompt: “Create `MainActivity` using Compose M3 theme, add `NavHost(startDestination = "camera")` and an empty `CameraScreen()` composable.”

- [x] Define UI state data classes
  - 🛠 `CameraUiState`, `EditUiState`, `Suggestion` in `ui/` or `data/`.
  - 🎯 These are immutable and ready to collect in Compose.
  - 💡 Prompt: “Add minimal `data class` state models with sensible defaults (loading flags, URIs, errors).”

---

## 2) Camera Preview & Capture

- [x] Camera controller
  - 🛠 `controller/CameraController` exposes `StateFlow<CameraUiState>` + actions: `startPreview`, `capturePhoto`.
  - 🎯 Bind/unbind lifecycle; handles lens, flash, zoom state.
  - 💡 Prompt: “Implement `CameraController` that binds `Preview` + `ImageCapture` to a lifecycle and updates `CameraUiState`.”

- [x] Compose interop with PreviewView
  - 🛠 `CameraScreen` integrates `AndroidView { PreviewView }` and shutter UI.
  - 🎯 Preview renders full-bleed; shutter triggers `capturePhoto`.
  - 💡 Prompt: “Wire `CameraScreen` with `AndroidView(PreviewView)` and a Material 3 bottom-aligned shutter button.”

- [x] Capture to file and navigate to Review
  - 🛠 Use `ImageCapture.takePicture()` to app cache/file, return `Uri`.
  - 🎯 After capture, navigate to `review/{uri}` route with safe args.
  - 💡 Prompt: “Implement `ImageCapture` to file with proper executor, then navigate to Review with the returned `Uri`.”

---

## 3) MediaStore Save/Share

 - [x] Save to MediaStore
  - 🛠 Utility to write JPEG/WebP to `MediaStore.Images` with EXIF copy.
  - 🎯 Pressing Save in Review/Editor persists image and shows snackbar.
  - 💡 Prompt: “Add `utils/MediaStore.kt` with a `saveBitmapToGallery(Context, Bitmap, name)` function; call it from Review/Editor.”
  - Implemented in Editor: downloads result and saves via MediaStore.

- [x] Share via Sharesheet
  - 🛠 `FileProvider` + `Intent.createChooser`.
  - 🎯 Share action works from Review.
  - 💡 Prompt: “Configure `FileProvider` in manifest and implement share action returning a `content://` URI.”

---

## 4) Networking (nano-banana API)

- [x] Retrofit API + DTOs
  - 🛠 `network/NanoBananaApi`, `EditRequestDto`, `EditJobDto`.
  - 🎯 Can `POST /v1/edits` and `GET /v1/jobs/{id}`.
  - 💡 Prompt: “Define Retrofit interface and DTOs per the plan. Use Moshi converter.”

- [x] OkHttp + auth + logging
  - 🛠 Interceptor adds `Authorization: Bearer <NB_API_KEY>`.
  - 🎯 Auth applies to all API calls.
  - 💡 Prompt: “Provide `OkHttpClient` in a DI module that injects `BuildConfig.NB_API_KEY` header and logging in debug.”

- [x] Repository
  - 🛠 `data/EditRepository` with `submitEdit()` and `pollResult()`.
  - 🎯 Simple happy-path edit flow works with fake/mock server or real API.
  - 💡 Prompt: “Create `EditRepository` wrapping Retrofit calls; add suspend functions with error handling.”

---

## 5) Editor (Prompt + Apply)

- [ ] Edit controller
  - 🛠 `controller/EditController` manages `EditUiState`, `applyEdit(imageUri, prompt)`.
  - 🎯 Shows progress, populates result URL, handles errors/cancel.
  - 💡 Prompt: “Implement `EditController` using `EditRepository`; update state with progress and final result.”

- [ ] Editor UI
  - 🛠 `EditorScreen`: image preview, prompt field, Apply, Save.
  - 🎯 Crossfade result image; long-press toggles before/after.
  - 💡 Prompt: “Build a clean, Apple-like editor: large image, minimal controls, single accent color, subtle motion.”

---

## 6) Instant Suggestions

- [x] On-device (ML Kit or TFLite)
  - 🛠 Downscale bitmap after capture; run labeler; map labels → presets.
  - 🎯 Up to 3 suggestion chips appear; tap applies preset.
  - 💡 Prompt: “Integrate ML Kit image-labeling (preferred) or TFLite with a pre-trained labeler; produce a small set of `Suggestion` values.”

- [x] Suggestion controller
  - 🛠 `controller/SuggestionController` loads labels and exposes chips.
  - 🎯 `CameraScreen` shows chips with micro-animations.
  - 💡 Prompt: “Add a controller that converts labels to UI chips and exposes a flow for Compose.”

---

## 7) Gallery & Settings

 - [x] Gallery
  - 🛠 Simple grid of recent edits (URIs), open to re-edit.
  - 🎯 Smooth scroll; empty state.
  - 💡 Prompt: “Create `GalleryScreen` reading from MediaStore by relative path ‘Pictures/AI Camera’.”
  
  Status: Implemented simple list via `LazyColumn`; opens in-app.

- [x] Settings
  - 🛠 DataStore-backed toggles: offline mode, default quality, watermark.
  - 🎯 Preferences survive restart; editor respects them.
  - 💡 Prompt: “Implement a small `SettingsViewModel` using DataStore and a Compose preferences screen.”

---

## 8) Polish, Performance, and Testing

- [ ] Motion & haptics
  - 🛠 Shutter ripple, chip fade/slide, 120–200ms ease.
  - 🎯 Feels refined; no jank.
  - 💡 Prompt: “Add small `animate*AsState` and `AnimatedVisibility` touches; trigger haptics on capture and apply.”

- [ ] Performance
  - 🛠 Analyzer at low resolution; throttle; reuse buffers.
  - 🎯 No dropped frames on preview or editor.
  - 💡 Prompt: “Keep `ImageAnalysis` at ~640px width and ~10fps; avoid unnecessary bitmap copies.”

- [ ] Tests
  - 🛠 Unit tests for repositories/controllers; minimal Compose UI tests.
  - 🎯 Green unit tests; smoke UI test passes.
  - 💡 Prompt: “Add MockWebServer tests for API, and controller state reducer tests.”

---

## 9) Release Readiness

- [ ] Icons & splash
  - 🛠 Adaptive icon light/dark; simple splash screen.
  - 🎯 Brand-consistent, minimal.

- [ ] ProGuard/R8
  - 🛠 Keep rules for Retrofit/Moshi models as needed.
  - 🎯 Release build shrinks and runs.

- [ ] Privacy & disclosures
  - 🛠 Data Safety form notes network uploads for edits; in-app privacy toggle.
  - 🎯 Store-compliant.

---

## Visual & UX Notes (Apple-inspired minimalism)

- Color: near-white/near-black base with one teal accent (#12C2A9).
- Layout: full-bleed imagery; bottom-aligned primary actions; generous spacing.
- Motion: short, subtle; no abrupt transitions; natural easing.
- Touch: large targets; haptics on critical actions (capture/apply).
- Accessibility: 4.5:1 contrast; content descriptions; dynamic type friendly.

---

Tip: Work top-to-bottom. If blocked on the API, complete camera, editor scaffolding, and local save/share while stubbing the repository with fake data.

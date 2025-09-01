# Android AI Camera App — Implementation Status

Scope: excludes `backend/`. Source of truth is `app/` Kotlin + resources.

## Overview
- Pattern: MVC with MVVM elements (controllers + simple ViewModels)
- UI: Jetpack Compose + Material 3, custom glass/modern components
- Camera: CameraX (Preview, ImageCapture)
- AI: Interfaces + mock processor; Retrofit client + DI wiring for NanoBanana
- Data: MediaStore saves + simple repositories for edits; Data models via data classes
- DI: Hilt annotations present (modules + @AndroidEntryPoint) but not fully wired to all classes

## App Shell & Navigation
- Entry: `MainActivity` sets edge-to-edge, transparent system bars
- Theme: `ui/theme` with `AppTheme`
- Navigation: `NavHost` routes in `MainActivity`
  - Camera → Review → Editor; plus Gallery, Settings
  - Modern wrappers delegate to existing screens (`ui/modern/ModernScreens.kt`)
- Snackbar: custom glassmorphic host in root

Implemented
- `MainActivity.kt`: NavHost with routes `Camera`, `Review/{uri}`, `Editor/{uri}`, `Gallery`, `Settings`
- Modern wrappers: `ModernReviewScreen`, `ModernEditorScreen`, `ModernGalleryScreen`, `ModernSettingsScreen`
- Navigation helpers: `ui/navigation/NavRoutes.kt`

Missing/Partial
- Back navigation on Editor top bar uses placeholder comment (no `navController` wired there)

## Camera
- Screens
  - `ui/camera/ModernCameraScreen.kt`: rich camera UI (modes, filters UI, flash toggle, timer, zoom, AR placeholders)
  - `ui/camera/CameraScreen.kt`: older/simple variant (kept)
- Control
  - `controller/CameraController.kt`: binds preview, captures to MediaStore, state via `CameraUiState`
  - `ui/camera/CameraViewModel.kt`: wraps ImageCapture flows + mock AI analysis
- Capabilities
  - Permission via Accompanist
  - Bind Preview + ImageCapture
  - Switch front/back (via `cameraSelector` state in ModernCameraScreen)
  - Flash modes OFF/ON/AUTO (UI/state present; capture sync handled)
  - Zoom state and gestures placeholder (UI states; some input hooks)
  - Capture to cache (Modern flow) and to MediaStore (Controller flow)

Implemented
- CameraX preview and capture setup
- Flash toggle + icon feedback
- Timer UI/state and countdown variables
- Filter carousel UI with preset list (icons, colors, types)

Missing/Partial
- Video recording path (state present; no Recorder implementation)
- AR masks/face morph are UI placeholders; no rendering pipeline
- Zoom pinch-to-zoom not fully bound to camera control in Modern screen
- Some effects are visual chips only; real-time GLSL/OpenGL pipeline not present here (a separate `filters/RealTimeFilterEngine.kt` exists with algorithms but is not integrated into camera preview pipeline)

## Editor
- Screen: `ui/screens/EditorScreen.kt`
  - Loads `Uri` into Bitmap, computes thumbnails for presets
  - Compare gesture (press/long-press) toggles original vs working image
  - Prompt input tied to `EditController` state
  - Save button writes edited bitmap to gallery (`saveBitmapToGallery`)
  - Presets carousel uses `FilterPresets` and `applyPreset`
  - Enhancement dialog and action placeholders exist in code (e.g., label text)
- Controller: `controller/EditController.kt`
  - Holds `EditUiState` via `StateFlow`
  - `applyEdit(offline: Boolean)` submits to repo and polls result; offline path echoes source
- Repository: `data/EditRepository.kt`
  - Uses Retrofit `ApiService` (backend) to submit edit and poll job

Implemented
- Local filtering pipeline via `FilterPresets.applyPreset` for previews
- Compare slider/gesture with haptics
- Save to MediaStore
- Async state management around edit jobs (loading/error/result)

Missing/Partial
- Actual AI image transformation in-app; final image application from remote `resultUrl` to working bitmap is not shown
- Back button in Editor UI does not navigate (placeholder comment)
- Enhancement dialog triggers are present, but actual AI-enhance invocation is not wired to controller in this screen

## Gallery
- Screen: `ui/screens/GalleryScreen.kt`
  - Queries `MediaStore` for images saved under `Pictures/AI Camera`
  - Displays grid with `coil` `AsyncImage`
  - Click to open routes back to Review

Implemented
- Grid gallery with adaptive columns and spacing
- Empty state message

Missing/Partial
- Selection/multi-select, share/delete actions not implemented here (share util exists separately)

## Settings
- Screen: `ui/screens/SettingsScreen.kt`
  - Basic settings UI (minimal; present in project)

Implemented
- Placeholder/settings scaffold using Compose Material 3

Missing/Partial
- Real preference storage via DataStore is not wired (a `PreferencesRepository.kt` exists but usage is limited/unclear)

## AI Integration
- Interfaces: `ai/AIImageProcessor.kt` defines `analyzeImage`, `applyFilter`, `enhanceImage`
- Mock: `MockAIImageProcessor` returns simulated results, delays
- NanoBanana: `network/NanoBananaApi.kt` models and endpoints, `NetworkModule` provides Retrofit with `BuildConfig.NB_API_KEY` and `NB_BASE_URL`
- App client: `network/ApiService.kt` for backend gateway (present in tree)
- Controllers/VMs: `CameraViewModel` uses `MockAIImageProcessor`; `EditController` uses `EditRepository` which calls backend

Implemented
- Retrofit/Moshi/OkHttp with auth header from BuildConfig
- DI Module for OkHttp/Retrofit/Apis
- Mock AI flows for local analysis/filter/enhance

Missing/Partial
- Real NanoBanana calls from the app not used yet (no injection of `NanoBananaApi` into a real implementation of `AIImageProcessor` is wired)
- Download/apply of remote `resultUrl` into editor pipeline

## Data & Utils
- MediaStore save: `utils/MediaStoreUtils.kt` saves JPEG to Pictures/AI Camera
- Load bitmap: `utils/ImageLoadUtils.kt` loads from Uri/path
- Quick enhance: `utils/Enhance.kt` simple bitmap enhancement
- Share: `utils/ShareUtils.kt` for sharing image URI
- Models: `data/Models.kt`, `data/ImageAnalysisResult.kt`, DTOs in `data/EditDtos.kt`
- Preferences: `data/PreferencesRepository.kt` present (usage limited)

Implemented
- Save/load helpers, basic image enhancement util
- Data models and DTOs

Missing/Partial
- Persistent user settings wiring (DataStore flow into UI)

## UI Components & Theme
- Theme: `ui/theme/{Color,Theme,Type}.kt`
- Components: `ui/components/{Glass.kt, ModernComponents.kt, CompareSlider.kt}`
  - Glassmorphic surfaces, cards, buttons
  - Compare slider component for before/after
- Modern camera/editor chrome in `ModernComponents`

Implemented
- Reusable composables for glass UI and controls
- Compare slider present and used in Editor

Missing/Partial
- Some components are not yet integrated across all screens

## DI (Hilt)
- Present: `@AndroidEntryPoint` on `MainActivity`, `NetworkModule` with `@Module @InstallIn(SingletonComponent::class)`
- Absent/Partial: ViewModels not annotated/injected; controllers created via `remember { ... }` rather than DI; no `@HiltAndroidApp` `Application` class found in `app/`

## Build Config & Secrets
- `app/build.gradle.kts` reads `NB_API_KEY` and `NB_BASE_URL` from `local.properties` or env, defines `BuildConfig` fields
- Dependencies include CameraX, Compose, Navigation, Retrofit, OkHttp, Moshi, Hilt, Coil, Coroutines, DataStore (declared)

## Testing
- Default template unit and instrumented tests only
- No feature/unit tests for camera/editor/network yet

## Summary Matrix
- Camera preview/capture: Implemented (photo); video/ar: partial
- Editor with presets/compare/save: Implemented; AI apply-from-remote: partial
- Gallery grid: Implemented basic
- Settings: Minimal
- AI client/DI: Implemented; actual usage in app: partial
- Data/Prefs: Helpers in place; wiring: partial
- Theming/components: Implemented and used
- Navigation: Implemented; a few back actions not wired

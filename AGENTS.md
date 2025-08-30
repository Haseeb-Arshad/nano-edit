# Android AI Camera App - Agent Guidelines

## Build/Test Commands
- Build: `gradlew build` or `gradlew assembleDebug`
- Test: `gradlew test` (unit tests), `gradlew connectedAndroidTest` (instrumented)
- Single test: `gradlew :app:testDebugUnitTest --tests "*.ClassName.testMethodName"`
- Lint: `gradlew lint`
- Clean: `gradlew clean`

## Architecture
- **Pattern**: MVC with MVVM elements (ViewModels + Controllers)
- **UI**: Jetpack Compose with Material 3
- **Camera**: CameraX for preview/capture
- **AI**: nano-banana API integration (BuildConfig.NB_API_KEY, NB_BASE_URL)
- **Data**: Local MediaStore + Retrofit/OkHttp for networking
- **DI**: Not yet implemented (consider Hilt)

## Package Structure
```
com.example.myapplication/
├─ ai/                  # AI processing interfaces
├─ controller/          # MVC controllers
├─ data/               # Models, repositories, DTOs
├─ network/            # API clients
├─ ui/                 # Compose screens & components
├─ utils/              # Common utilities
```

## Code Style
- Kotlin: Official style guide, camelCase naming
- Compose: Stateless composables, hoist state up
- ViewModels: StateFlow for state management
- Network: Suspend functions with proper error handling
- Imports: Group by package (androidx, kotlin, app packages)
- No code comments unless complex logic

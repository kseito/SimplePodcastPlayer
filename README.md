# SimplePodcastPlayer

A Kotlin Multiplatform project using Compose Multiplatform, targeting Android and iOS platforms.

## Project Overview

SimplePodcastPlayer is a cross-platform podcast application built with Kotlin Multiplatform and Compose Multiplatform. The project uses a shared UI approach where most code is in the `commonMain` source set.

### Requirements Documentation
- **Requirements specification**: See `docs/specs/features/requirements.md` for detailed functional and technical requirements
- **Supported platforms**: Android 14+ (API Level 34) and iOS 16+
- **Core features**: Podcast search, audio playback, subscription management, playback history

## Getting Started

### Building and Running

- **Build project**: `./gradlew build`
- **Clean build**: `./gradlew clean build`
- **Run Android app**: `./gradlew :androidApp:installDebug` (requires connected device/emulator)
- **Run tests**: `./gradlew :composeApp:testAndroidHostTest` (shared unit tests; plain `./gradlew test` does not run them under the KMP library plugin)

### Code Quality

- **Run Detekt**: `./gradlew detekt`
- **Run Detekt with auto-correct**: `./gradlew detektFormat` (if available)
- **Generate Detekt reports**: Reports are automatically generated in `build/reports/detekt/`
- **Run Spotless check**: `./gradlew spotlessCheck`
- **Apply Spotless formatting**: `./gradlew spotlessApply`

### iOS Development

- Open `iosApp/iosApp.xcodeproj` in Xcode to run iOS version
- iOS framework is built automatically when building from Xcode

## Architecture

### Project Structure

- **Root project**: Contains Gradle configuration and overall project setup
- **composeApp**: Shared KMP library module containing multiplatform code
  - `commonMain`: Shared code for all platforms (UI, business logic)
  - `androidMain`: Android-specific `actual` implementations and Android components reused by shared code (`PlaybackService`, `PlayerActivity`)
  - `iosMain`: iOS-specific implementations and MainViewController
  - `commonTest`: Shared test code
- **androidApp**: Android application module — entry point (`MainActivity`), `AndroidManifest.xml`, resources/icons, signing/versioning. Depends on `:composeApp`.
- **iosApp**: iOS native app wrapper (Xcode project)

### Key Dependencies

- Kotlin 2.4.0 with Compose Multiplatform 1.11.1
- Navigation Compose for screen navigation
- Material3 for UI components
- Lifecycle ViewModel and Runtime Compose for state management
- Target: Android SDK 36, minimum SDK 34 (Android 14+)
- iOS minimum deployment target: iOS 16.0

### Application Flow

- `App.kt` sets up a `NavHost` to manage navigation between screens. Destinations include `list`, `search`, `detail`, `list_detail`, `player`, and `in_progress`.
- `PodcastListScreen` displays the podcast list and navigates to the search, detail, or in-progress episodes screen.
- `PodcastSearchScreen` provides search functionality and navigates to the detail screen.
- `PodcastDetailScreen` shows podcast details and episodes, and navigates to the player.
- Navigation is managed using a `NavController`, with state passed between composables to determine which podcast or episode to display.

### Platform-Specific Entry Points

- **Android**: `MainActivity.kt` (in the `androidApp` module) extends ComponentActivity and calls `App()`
- **iOS**: Uses `MainViewController.kt` to bridge to the shared `App()` composable

## Testing

- Tests use kotlin-test with kotlinx-coroutines-test, Turbine for Flow testing, and Koin for dependency injection
- Run all tests: `./gradlew :composeApp:testAndroidHostTest`
- Common tests are in `composeApp/src/commonTest` (utilities, repositories, ViewModels, with fakes in the `fake` package)
- Test reports are generated in `composeApp/build/reports/tests/`

## Code Organization

- Package structure: `jp.kztproject.simplepodcastplayer`
- Screens are organized in `screen` package
- Platform implementations use `Platform.kt` interface pattern
- Shared resources in `composeApp/src/commonMain/composeResources`

## Code Quality and CI/CD

- **Detekt** is configured for static code analysis across all source sets
- GitHub Actions workflow runs Detekt on every PR and push to main
- Configuration file: `detekt-config.yml` with Compose-specific rules
- HTML and XML reports are generated and uploaded as artifacts
- Detekt analyzes `commonMain`, `androidMain`, and `iosMain` source sets

## Code Style Guidelines

### Comments and Documentation

- **TODO comments**: Keep TODO comments in the code as they indicate future improvements and implementation plans
- TODO comments should be descriptive and provide context about what needs to be implemented
- Use "TODO:" prefix followed by a clear description of the task
- Examples of appropriate TODO comments:
  - `// TODO: Implement actual repository integration when backend is ready`
  - `// TODO: Add error handling for network failures`
  - `// TODO: Replace with real authentication logic`

## Learn More

- [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)

## Development Guidelines

For detailed development guidelines and instructions for Claude Code, see [CLAUDE.md](CLAUDE.md).

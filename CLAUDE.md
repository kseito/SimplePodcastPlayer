# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview
SimplePodcastPlayer is a Kotlin Multiplatform project using Compose Multiplatform, targeting Android and iOS platforms. The project uses a shared UI approach where most code is in the `commonMain` source set.

### Requirements Documentation
- **Requirements specification**: See `document/requirements.md` for detailed functional and technical requirements
- **Supported platforms**: Android 14+ (API Level 34) and iOS 16+
- **Core features**: Podcast search, audio playback, subscription management, playback history

## Common Development Commands

### Building and Running
- **Build project**: `./gradlew build`
- **Clean build**: `./gradlew clean build`
- **Run Android app**: `./gradlew :composeApp:installDebug` (requires connected device/emulator)
- **Run tests**: `./gradlew test`
- **Run tests for specific module**: `./gradlew :composeApp:testDebugUnitTest`

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
- **composeApp**: Main module containing multiplatform code
  - `commonMain`: Shared code for all platforms (UI, business logic)
  - `androidMain`: Android-specific implementations and MainActivity
  - `iosMain`: iOS-specific implementations and MainViewController
  - `commonTest`: Shared test code
- **iosApp**: iOS native app wrapper (Xcode project)

### Key Dependencies
- Kotlin 2.2.0 with Compose Multiplatform 1.8.2
- Navigation Compose for screen navigation
- Material3 for UI components
- Lifecycle ViewModel and Runtime Compose for state management
- Target: Android SDK 35, minimum SDK 34 (Android 14+)
- iOS minimum deployment target: iOS 16.0

### Application Flow
The app follows a simple navigation pattern:
- `App.kt` sets up NavHost with two destinations: "list" and "search"
- `PodcastListScreen` displays podcast list with navigation to search
- `PodcastSearchScreen` provides search functionality with navigation back to list
- Navigation is handled through lambda callbacks passed between screens

### Platform-Specific Entry Points
- **Android**: `MainActivity.kt` extends ComponentActivity and calls `App()`
- **iOS**: Uses `MainViewController.kt` to bridge to the shared `App()` composable

## Testing
- Tests use kotlin-test framework
- Run all tests: `./gradlew test`
- Common tests are in `commonTest` source set
- Currently has basic example test in `ComposeAppCommonTest.kt`

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

# important-instruction-reminders
Do what has been asked; nothing more, nothing less.
NEVER create files unless they're absolutely necessary for achieving your goal.
ALWAYS prefer editing an existing file to creating a new one.
NEVER proactively create documentation files (*.md) or README files. Only create documentation files if explicitly requested by the User.
Only use emojis if the user explicitly requests it. Avoid writing emojis to files unless asked.
IMPORTANT: Keep TODO comments in the code. TODO comments are valuable for indicating future improvements and should not be removed during refactoring or cleanup.
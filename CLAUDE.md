# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview
SimplePodcastPlayer is a Kotlin Multiplatform project using Compose Multiplatform, targeting Android and iOS platforms. The project uses a shared UI approach where most code is in the `commonMain` source set.

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
- Target: Android SDK 35, minimum SDK 24

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
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
- `App.kt` sets up a `NavHost` to manage navigation between screens. Key destinations include `list`, `search`, `detail`, `list_detail`, and `player`.
- `PodcastListScreen` displays the podcast list and navigates to the search or detail screen.
- `PodcastSearchScreen` provides search functionality and navigates to the detail screen.
- Navigation is managed using a `NavController`, with state passed between composables to determine which podcast or episode to display.

### Platform-Specific Entry Points
- **Android**: `MainActivity.kt` extends ComponentActivity and calls `App()`
- **iOS**: Uses `MainViewController.kt` to bridge to the shared `App()` composable

## Testing

### Unit Testing
- **Test Framework**: kotlin-test with kotlinx-coroutines-test for async testing
- **Test Utilities**: Turbine for Flow testing, Koin for dependency injection
- **Run all tests**: `./gradlew test`
- **Run specific test suite**: `./gradlew :composeApp:testDebugUnitTest`
- **Test location**: `composeApp/src/commonTest` for shared tests
- **Test reports**: Generated in `composeApp/build/reports/tests/`

### Test Coverage
Current test coverage includes:
- **Utilities**: RssParser, EpisodeUtils
- **Repositories**: PodcastRepository
- **ViewModels**: PodcastListViewModel, PodcastSearchViewModel, PodcastDetailViewModel
- **Test Infrastructure**: Fake implementations (DAOs, API clients, services) in `fake` package

### Dependency Injection for Testing
- **DI Framework**: Koin is used for dependency injection
- **Test Doubles**: Fake implementations provided for repositories, DAOs, and API clients
- **Test Data**: `TestDataFactory` provides consistent test data generation
- All production classes accept dependencies via constructor injection for testability

### CI/CD Testing
- **GitHub Actions**: Unit tests run automatically on every PR and push to main
- **Workflow**: `.github/workflows/unit-tests.yml`
- **Test Reports**: Uploaded as artifacts (14-day retention)
- See `document/unit-test-implementation-plan.md` for detailed testing strategy

## Code Organization
- Package structure: `jp.kztproject.simplepodcastplayer`
- Screens are organized in `screen` package
- Platform implementations use `Platform.kt` interface pattern
- Shared resources in `composeApp/src/commonMain/composeResources`

## Code Quality and CI/CD

### Automated Checks
GitHub Actions workflows run automatically on every PR and push to main:

- **Unit Tests** (`.github/workflows/unit-tests.yml`):
  - Runs all unit tests with `./gradlew test`
  - Uploads test reports as artifacts

- **Detekt** (`.github/workflows/detekt.yml`):
  - Static code analysis across all source sets
  - Configuration: `detekt-config.yml` with Compose-specific rules
  - Analyzes `commonMain`, `androidMain`, and `iosMain` source sets
  - Uploads HTML and XML reports as artifacts

- **Code Formatting** (`.github/workflows/ktlint-spotless.yml`):
  - Checks and applies Spotless formatting
  - Provides automated suggestions via reviewdog

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

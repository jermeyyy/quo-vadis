# Codebase Structure and Organization

## Project Root Structure

```
NavPlayground/
├── .git/                    # Git repository
├── .gradle/                 # Gradle cache and metadata
├── .idea/                   # IntelliJ IDEA project files
├── .kotlin/                 # Kotlin compiler cache
├── .serena/                 # Serena AI assistant configuration
├── build/                   # Build outputs (root)
├── gradle/                  # Gradle wrapper and version catalog
├── composeApp/              # Demo application module
├── quo-vadis-core/          # Navigation library module
├── iosApp/                  # iOS application wrapper
├── build.gradle.kts         # Root build configuration
├── settings.gradle.kts      # Multi-module project settings
├── gradle.properties        # Gradle and build properties
├── gradlew                  # Gradle wrapper (Unix)
├── gradlew.bat             # Gradle wrapper (Windows)
├── local.properties        # Local SDK paths (not in git)
└── README.md               # Project documentation
```

## composeApp Module

**Purpose**: Demo application showcasing all navigation patterns

```
composeApp/
├── build.gradle.kts
├── build/                   # Build outputs (gitignored)
└── src/
    ├── androidMain/         # Android-specific code
    │   └── kotlin/
    ├── commonMain/          # Shared multiplatform code
    │   └── kotlin/com/jermey/navplayground/
    │       ├── App.kt       # Main entry point
    │       ├── Platform.kt  # Platform-specific utilities
    │       └── demo/        # Comprehensive navigation demos
    │           ├── DemoApp.kt                  # Main demo app
    │           ├── README.md                   # Demo documentation
    │           ├── destinations/
    │           │   └── Destinations.kt         # All destination definitions
    │           ├── graphs/
    │           │   └── NavigationGraphs.kt     # Navigation graph configs
    │           └── ui/
    │               ├── BottomNavigationBar.kt  # Bottom nav component
    │               └── screens/
    │                   ├── MainScreens.kt      # Home, Explore, etc.
    │                   ├── Item.kt  # List/Detail pattern
    │                   ├── TabsScreens.kt      # Nested tabs pattern
    │                   ├── ProcessScreens.kt   # Wizard/process pattern
    │                   └── DeepLinkDemoScreen.kt  # Deep link demos
    └── iosMain/             # iOS-specific code
        └── kotlin/
```

### Demo Patterns Implemented
1. **Bottom Navigation**: 4 main tabs with persistent bottom bar
2. **Master-Detail**: List view with detail screens and deep navigation
3. **Tabs**: Nested tabs with sub-navigation
4. **Process/Wizard**: Multi-step flow with branching logic
5. **Modal Drawer**: Side navigation drawer

## quo-vadis-core Module

**Purpose**: Core navigation library for Kotlin Multiplatform

```
quo-vadis-core/
├── build.gradle.kts
├── docs/                    # Comprehensive documentation
│   ├── ARCHITECTURE.md      # Architecture overview and principles
│   ├── API_REFERENCE.md     # Complete API documentation
│   ├── MULTIPLATFORM_PREDICTIVE_BACK.md  # Predictive back support
│   └── NAVIGATION_IMPLEMENTATION.md      # Implementation details
├── build/                   # Build outputs (gitignored)
└── src/
    ├── commonMain/          # Core multiplatform code
    │   └── kotlin/com/jermey/quo/vadis/core/
    │       ├── Platform.kt  # Platform abstractions
    │       └── navigation/
    │           ├── README.md                   # Library overview
    │           ├── package-info.kt             # Package documentation
    │           ├── core/                       # Core navigation components
    │           │   ├── Navigator.kt            # Central navigation controller
    │           │   ├── Destination.kt          # Navigation targets
    │           │   ├── BackStack.kt            # Stack management
    │           │   ├── NavigationGraph.kt      # Modular graphs
    │           │   ├── NavigationTransition.kt # Animation support
    │           │   └── DeepLink.kt             # Deep link handling
    │           ├── compose/                    # Compose integration
    │           │   ├── NavHost.kt              # Navigation host composable
    │           │   ├── PlatformAwareNavHost.kt # Platform-specific hosting
    │           │   └── PredictiveBackNavigation.kt  # Android predictive back
    │           ├── mvi/                        # MVI architecture support
    │           │   ├── NavigationIntent.kt     # MVI intents
    │           │   └── NavigationViewModel.kt  # Base ViewModel
    │           ├── integration/                # DI framework integration
    │           │   └── KoinIntegration.kt      # Koin support
    │           ├── serialization/              # State serialization
    │           │   └── StateSerializer.kt      # Save/restore state
    │           ├── testing/                    # Testing utilities
    │           │   └── FakeNavigator.kt        # Test double
    │           ├── utils/                      # Extension functions
    │           │   └── NavigationExtensions.kt # Utility extensions
    │           └── example/                    # Example implementations
    │               ├── SampleNavigation.kt     # Sample code
    │               └── MVIExample.kt           # MVI example
    ├── androidMain/         # Android-specific implementations
    │   └── kotlin/com/jermey/quo/vadis/core/
    ├── iosMain/             # iOS-specific implementations
    │   └── kotlin/com/jermey/quo/vadis/core/
    ├── androidHostTest/     # Android host tests
    │   └── kotlin/
    └── androidDeviceTest/   # Android instrumented tests
        └── kotlin/
```

### Library Package Organization

- **core**: Core navigation components (Navigator, BackStack, Destination, etc.)
- **compose**: Compose Multiplatform UI integration
- **mvi**: MVI architecture pattern support
- **integration**: DI framework integrations (Koin, etc.)
- **serialization**: State persistence and restoration
- **testing**: Test utilities and fakes
- **utils**: Extension functions and helpers
- **example**: Example code and patterns

## iosApp Module

**Purpose**: iOS application wrapper for Compose Multiplatform

```
iosApp/
├── iosApp.xcodeproj/        # Xcode project
│   ├── project.pbxproj      # Project configuration
│   └── xcuserdata/          # User-specific settings
├── Configuration/
│   └── Config.xcconfig      # Build configuration
└── iosApp/
    ├── iOSApp.swift         # Main app entry (SwiftUI)
    ├── ContentView.swift    # Compose integration view
    ├── Info.plist          # App metadata
    ├── Assets.xcassets/    # App icons and assets
    └── Preview Content/    # SwiftUI preview data
```

### iOS Integration Pattern
- SwiftUI app uses `UIViewControllerRepresentable` to embed Compose UI
- Compose framework built as static framework
- Framework name: `ComposeApp`

## gradle Module

**Purpose**: Gradle configuration and dependencies

```
gradle/
├── libs.versions.toml       # Version catalog (centralized dependencies)
└── wrapper/
    ├── gradle-wrapper.jar   # Gradle wrapper binary
    └── gradle-wrapper.properties  # Wrapper configuration
```

### Version Catalog Structure
- `[versions]`: Version numbers for all dependencies
- `[libraries]`: Library dependency declarations
- `[plugins]`: Plugin dependency declarations

## Build Outputs (gitignored)

### Typical Build Directory Contents
```
build/
├── reports/                 # Build and test reports
│   ├── configuration-cache/ # Configuration cache data
│   └── problems/           # Build problem reports
├── classes/                # Compiled classes
├── generated/              # Generated sources
├── intermediates/          # Intermediate build artifacts
├── outputs/                # Final build outputs (APKs, AARs, etc.)
└── tmp/                    # Temporary files
```

## Key Files

### Root Level
- **build.gradle.kts**: Root build configuration, applies plugins to subprojects
- **settings.gradle.kts**: Defines project structure, includes modules
- **gradle.properties**: Build properties (JVM args, code style, Android settings)
- **local.properties**: Local SDK paths (Android SDK, NDK)
- **README.md**: Project overview and getting started guide

### Module Level
- **build.gradle.kts**: Module-specific build configuration
- **src/**: Source code organized by source sets

## Source Set Organization

### Multiplatform Source Sets
Each module uses Kotlin Multiplatform source sets:

1. **commonMain**: Shared code for all platforms
2. **androidMain**: Android-specific code
3. **iosMain**: iOS-specific code (shared by all iOS targets)
4. **commonTest**: Shared test code
5. **androidTest**: Android-specific tests
6. **androidHostTest**: Android JVM tests
7. **androidDeviceTest**: Android instrumented tests

### Naming Convention
- Source sets follow KMP convention: `<target><SourceSet>`
- Example: `commonMain`, `androidMain`, `iosMain`

## Package Naming

### Demo Application
- Root: `com.jermey.navplayground`
- Features: `com.jermey.navplayground.demo.<feature>`

### Navigation Library
- Root: `com.jermey.quo.vadis.core`
- Navigation: `com.jermey.quo.vadis.core.navigation.<package>`

## Generated Code

### Locations (gitignored)
- `build/generated/`: Kapt, KSP, resource generation
- `build/kotlin/`: Kotlin compiler outputs
- `.gradle/`: Gradle cache and metadata
- `.kotlin/`: Kotlin compiler cache

## Important Files to Check

### For Building Issues
1. `gradle.properties` - Build settings
2. `build.gradle.kts` - Build scripts
3. `gradle/libs.versions.toml` - Dependency versions

### For Navigation Features
1. `quo-vadis-core/src/commonMain/kotlin/.../core/` - Core implementation
2. `quo-vadis-core/docs/` - Comprehensive documentation
3. `composeApp/src/commonMain/kotlin/.../demo/` - Usage examples

### For Platform-Specific Code
1. `src/androidMain/` - Android implementations
2. `src/iosMain/` - iOS implementations
3. `iosApp/` - iOS app wrapper

## Excluded from Git

Based on standard `.gitignore`:
- `build/` directories
- `.gradle/` directory
- `.kotlin/` directory
- `*.iml` files
- `local.properties`
- `.DS_Store` (macOS)
- Generated code directories

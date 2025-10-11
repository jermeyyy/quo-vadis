# NavPlayground - Project Overview

## Purpose
NavPlayground is a **Kotlin Multiplatform** navigation library demonstration project called **"Quo Vadis"** (Latin for "Where are you going?"). The project serves dual purposes:

1. **Core Library (`quo-vadis-core`)**: A comprehensive, independent navigation library for Kotlin Multiplatform projects with Compose Multiplatform UI
2. **Demo Application (`composeApp`)**: A showcase application demonstrating all navigation patterns and features of the library

## Key Features of the Navigation Library

- **Modularization Support**: Gray box pattern for feature modules with clear boundaries
- **Direct Backstack Access**: Full control over navigation stack manipulation
- **Type-Safe Navigation**: Compile-time safety for navigation targets
- **Deep Link Support**: URI-based and custom deep linking
- **Transition Animations**: Including support for shared element transitions
- **MVI Architecture Integration**: First-class support for MVI pattern
- **DI Framework Ready**: Easy integration with Koin and other DI frameworks
- **No External Dependencies**: Independent from other navigation libraries
- **Multiplatform**: Works on Android, iOS, Desktop, and Web

## Project Structure

```
NavPlayground/
├── composeApp/              # Demo application showcasing navigation patterns
│   └── src/commonMain/kotlin/com/jermey/navplayground/
│       ├── App.kt           # Main app entry point
│       └── demo/            # Comprehensive navigation demos
│           ├── DemoApp.kt   # Main demo with drawer & bottom nav
│           ├── destinations/ # All destination definitions
│           ├── graphs/      # Navigation graph definitions
│           └── ui/screens/  # Demo screens for all patterns
├── quo-vadis-core/          # Navigation library module
│   ├── docs/                # Architecture and API documentation
│   └── src/
│       ├── commonMain/kotlin/com/jermey/quo/vadis/core/navigation/
│       │   ├── core/        # Core navigation components (Navigator, BackStack, etc.)
│       │   ├── compose/     # Compose integration (NavHost)
│       │   ├── mvi/         # MVI pattern support
│       │   ├── integration/ # DI framework integration
│       │   ├── serialization/ # State serialization
│       │   ├── testing/     # Testing utilities (FakeNavigator)
│       │   └── utils/       # Extension functions
│       ├── androidMain/     # Android-specific implementations
│       └── iosMain/         # iOS-specific implementations
└── iosApp/                  # iOS application wrapper
```

## Modules

1. **composeApp**: Main application module demonstrating navigation patterns
   - Package: `com.jermey.navplayground`
   - Targets: Android, iOS
   
2. **quo-vadis-core**: Navigation library module
   - Package: `com.jermey.quo.vadis.core`
   - Targets: Android, iOS (can support Desktop, Web)

## Demo Application Patterns

The demo app showcases the following navigation patterns:

1. **Bottom Navigation**: 4 main tabs (Home, Explore, Profile, Settings)
2. **Master-Detail Navigation**: List with detail views and deep navigation
3. **Tabs Navigation**: Nested tabs with sub-navigation
4. **Process/Wizard Navigation**: Multi-step flow with branching logic
5. **Modal Drawer Navigation**: Side drawer with menu items

## Target Platforms

- **Android**: Min SDK 24, Target SDK 36, Compile SDK 36
- **iOS**: iOS Arm64, iOS Simulator Arm64, iOS x64
- **Potential**: Desktop (JVM), Web (Wasm)

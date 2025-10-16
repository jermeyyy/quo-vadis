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
- **Full Multiplatform**: Works on Android, iOS, Desktop (JVM), Web (JS/Wasm)

## Project Structure

```
NavPlayground/
├── composeApp/              # Demo application showcasing navigation patterns
│   └── src/
│       ├── commonMain/kotlin/com/jermey/navplayground/
│       │   ├── App.kt       # Main app entry point
│       │   └── demo/        # Comprehensive navigation demos
│       │       ├── DemoApp.kt   # Main demo with drawer & bottom nav
│       │       ├── destinations/ # All destination definitions
│       │       ├── graphs/      # Navigation graph definitions
│       │       └── ui/screens/  # Demo screens for all patterns
│       ├── androidMain/     # Android app entry point
│       ├── iosMain/         # iOS app entry point
│       ├── jsMain/          # JavaScript/Web entry point
│       ├── wasmJsMain/      # WebAssembly entry point
│       └── desktopMain/     # Desktop (JVM) entry point
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
│       ├── iosMain/         # iOS-specific implementations
│       ├── jsMain/          # JavaScript-specific implementations
│       ├── wasmJsMain/      # WebAssembly-specific implementations
│       └── desktopMain/     # Desktop-specific implementations
└── iosApp/                  # iOS application wrapper
```

## Modules

1. **composeApp**: Main application module demonstrating navigation patterns
   - Package: `com.jermey.navplayground`
   - Targets: Android, iOS, JavaScript, WebAssembly, Desktop
   
2. **quo-vadis-core**: Navigation library module
   - Package: `com.jermey.quo.vadis.core`
   - Targets: Android, iOS, JavaScript, WebAssembly, Desktop

## Demo Application Patterns

The demo app showcases the following navigation patterns:

1. **Bottom Navigation**: 4 main tabs (Home, Explore, Profile, Settings)
2. **Master-Detail Navigation**: List with detail views and deep navigation
3. **Tabs Navigation**: Nested tabs with sub-navigation
4. **Process/Wizard Navigation**: Multi-step flow with branching logic
5. **Modal Drawer Navigation**: Side drawer with menu items

## Target Platforms (7 Total)

### Mobile
- **Android**: Min SDK 24, Target SDK 36, Compile SDK 36
- **iOS Arm64**: Physical devices
- **iOS Simulator Arm64**: M1/M2 Mac simulators
- **iOS x64**: Intel Mac simulators

### Web
- **JavaScript (IR)**: Browser-based with Canvas rendering, broader compatibility
- **WebAssembly**: Near-native performance, modern browsers only

### Desktop
- **JVM (Desktop)**: Native applications for macOS, Windows, Linux

## Publishing

The library supports Maven Local publishing for all platforms:

```bash
./gradlew :quo-vadis-core:publishToMavenLocal
```

Published artifacts:
- `quo-vadis-core-android-*.aar` - Android library
- `quo-vadis-core-iosx64-*.klib` - iOS x64 framework
- `quo-vadis-core-iosarm64-*.klib` - iOS Arm64 framework
- `quo-vadis-core-iossimulatorarm64-*.klib` - iOS Simulator Arm64
- `quo-vadis-core-js-*.klib` - JavaScript library
- `quo-vadis-core-wasm-js-*.klib` - WebAssembly library
- `quo-vadis-core-desktop-*.jar` - Desktop JVM library

## Platform Features

### Android
- System back button integration
- Predictive back gestures (Android 13+)
- Deep link handling
- Activity lifecycle integration

### iOS
- Swipe-back navigation
- Navigation bar customization
- Universal links support
- iOS-specific transitions

### Web (JS/Wasm)
- Browser back button support
- URL-based routing (can be implemented)
- Canvas-based rendering via ComposeViewport
- Single-page application (SPA) architecture
- Progressive Web App (PWA) compatible

### Desktop (JVM)
- Native window controls (macOS, Windows, Linux)
- Keyboard shortcuts support
- Menu bar integration
- Multi-window support
- Native installers (DMG, MSI, DEB)

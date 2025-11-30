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
- **Transition Animations**: Custom enter/exit animations with full control
- **Shared Element Transitions**: ✨ Material Design-compliant shared elements (NEW!)
  - Works in BOTH forward AND backward navigation
  - Compatible with predictive back gestures
  - Per-destination opt-in via `destinationWithScopes()`
  - Full multiplatform support
- **Predictive Back Navigation**: Smooth gesture-based back navigation (Android 13+, iOS)
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
│       │       └── ui/
│       │           ├── components/  # Reusable components (ItemCard, etc.)
│       │           └── screens/     # Demo screens for all patterns
│       │               ├── masterdetail/  # Master-detail with shared elements
│       │               ├── process/       # Wizard/process flow
│       │               ├── tabs/          # Nested tabs
│       │               └── ...
│       ├── androidMain/     # Android app entry point
│       ├── iosMain/         # iOS app entry point
│       ├── jsMain/          # JavaScript/Web entry point
│       ├── wasmJsMain/      # WebAssembly entry point
│       └── desktopMain/     # Desktop (JVM) entry point
├── quo-vadis-core/          # Navigation library module
│   ├── docs/                # Architecture and API documentation
│   │   ├── API_REFERENCE.md                    # Complete API docs
│   │   ├── ARCHITECTURE.md                     # Design patterns
│   │   ├── NAVIGATION_IMPLEMENTATION.md        # Implementation guide
│   │   ├── MULTIPLATFORM_PREDICTIVE_BACK.md   # Predictive back details
│   │   └── SHARED_ELEMENT_TRANSITIONS.md      # ✨ Shared elements guide (NEW!)
│   └── src/
│       ├── commonMain/kotlin/com/jermey/quo/vadis/core/navigation/
│       │   ├── core/        # Core navigation components
│       │   │   ├── Destination.kt
│       │   │   ├── BackStack.kt
│       │   │   ├── Navigator.kt
│       │   │   ├── NavigationGraph.kt
│       │   │   └── NavigationTransition.kt
│       │   ├── compose/     # Compose integration
│       │   │   ├── NavHost.kt
│       │   │   ├── GraphNavHost.kt
│       │   │   ├── PredictiveBackNavigation.kt
│       │   │   ├── SharedElementScope.kt         # ✨ NEW
│       │   │   └── SharedElementModifiers.kt     # ✨ NEW
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
   - ✨ **NEW**: Features shared element transitions (icon + title)
   - Icon animates from 56dp to 80dp during transition
   - Title animates position with crossfade
3. **Tabs Navigation**: Nested tabs with sub-navigation
4. **Process/Wizard Navigation**: Multi-step flow with branching logic
5. **Modal Drawer Navigation**: Side drawer with menu items

## Target Platforms (7 Total)

### Mobile
- **Android**: Min SDK 24, Target SDK 36, Compile SDK 36
  - Predictive back gestures (API 33+)
  - Shared element transitions
- **iOS Arm64**: Physical devices
  - Swipe-back gestures
  - Shared element transitions
- **iOS Simulator Arm64**: M1/M2 Mac simulators
- **iOS x64**: Intel Mac simulators

### Web
- **JavaScript (IR)**: Browser-based with Canvas rendering, broader compatibility
  - Shared element transitions
- **WebAssembly**: Near-native performance, modern browsers only
  - Shared element transitions

### Desktop
- **JVM (Desktop)**: Native applications for macOS, Windows, Linux
  - Shared element transitions

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
- Predictive back gestures (Android 13+) with shared elements
- Deep link handling
- Activity lifecycle integration
- Shared element transitions

### iOS
- Swipe-back navigation with shared elements
- Navigation bar customization
- Universal links support
- iOS-specific transitions
- Shared element transitions

### Web (JS/Wasm)
- Browser back button support
- URL-based routing (can be implemented)
- Canvas-based rendering via ComposeViewport
- Single-page application (SPA) architecture
- Progressive Web App (PWA) compatible
- Shared element transitions

### Desktop (JVM)
- Native window controls (macOS, Windows, Linux)
- Keyboard shortcuts support
- Menu bar integration
- Multi-window support
- Native installers (DMG, MSI, DEB)
- Shared element transitions

## Recent Updates

### December 2024 - Shared Element Transitions
✅ **Fully implemented** shared element transitions using Compose SharedTransitionLayout
✅ **Bidirectional support** - works in forward AND backward navigation
✅ **Predictive back integration** - shared elements follow gestures smoothly
✅ **Per-destination opt-in** - use `destinationWithScopes()` for granular control
✅ **Complete documentation** - SHARED_ELEMENT_TRANSITIONS.md guide
✅ **Demo implementation** - master-detail flow with icon + title transitions

See `shared_element_transitions` memory for implementation details.
# Quo Vadis Navigation Library

<p align="center">
  <img src="art/logo.jpg" alt="NavPlayground Logo" width="200"/>
</p>

**"Quo Vadis"** (Latin for "Where are you going?") - A comprehensive, type-safe navigation library for Kotlin Multiplatform and Compose Multiplatform.

## 🎯 Project Overview

This project consists of **two main components**:

1. **`quo-vadis-core`** - The navigation library itself (reusable, independent)
2. **`composeApp`** - Demo application showcasing all navigation patterns

## ✨ Key Features

- ✅ **Type-Safe Navigation** - Compile-time safety with no string-based routing
- ✅ **Multiplatform** - Works on Android, iOS, Desktop, and Web
- ✅ **Modular Architecture** - Gray box pattern for feature modules
- ✅ **Direct BackStack Access** - Full control over navigation stack
- ✅ **Deep Link Support** - URI-based navigation with pattern matching
- ✅ **Predictive Back Navigation** - Smooth animated back gestures (Android 13+ & iOS)
- ✅ **Shared Element Transitions** - Material Design shared elements (forward & back!)
- ✅ **MVI Architecture** - First-class MVI pattern integration
- ✅ **Transitions & Animations** - Built-in and custom transitions
- ✅ **DI Framework Support** - Easy integration with Koin, Kodein, etc.
- ✅ **Testable** - FakeNavigator for unit testing
- ✅ **No External Dependencies** - Independent navigation library

## 📦 Project Structure

```
NavPlayground/
├── quo-vadis-core/              # Navigation library
│   ├── src/
│   │   ├── commonMain/          # Core navigation logic
│   │   ├── androidMain/         # Android-specific features
│   │   └── iosMain/             # iOS-specific features
│   └── docs/                    # Library documentation
│       ├── ARCHITECTURE.md
│       ├── API_REFERENCE.md
│       ├── NAVIGATION_IMPLEMENTATION.md
│       ├── MULTIPLATFORM_PREDICTIVE_BACK.md
│       └── SHARED_ELEMENT_TRANSITIONS.md
├── composeApp/                  # Demo application
│   └── src/
│       ├── commonMain/          # Demo screens & examples
│       ├── androidMain/         # Android app entry point
│       └── iosMain/             # iOS app entry point
└── iosApp/                      # iOS app wrapper
```

## 🚀 Quick Start

### Using the Library

```kotlin
// 1. Define type-safe destinations
sealed class AppDestination : Destination {
    object Home : AppDestination() {
        override val route = "home"
    }
    
    data class Details(val id: String) : AppDestination() {
        override val route = "details"
        override val arguments = mapOf("id" to id)
    }
}

// 2. Create a navigation graph
val appGraph = navigationGraph("app") {
    startDestination(AppDestination.Home)
    
    destination(AppDestination.Home) { _, navigator ->
        HomeScreen(onNavigateToDetails = { id ->
            navigator.navigate(AppDestination.Details(id))
        })
    }
    
    destination(SimpleDestination("details")) { dest, navigator ->
        DetailsScreen(
            itemId = dest.arguments["id"] as String,
            onBack = { navigator.navigateBack() }
        )
    }
}

// 3. Setup navigation
@Composable
fun App() {
    val navigator = rememberNavigator()
    
    LaunchedEffect(Unit) {
        navigator.registerGraph(appGraph)
        navigator.setStartDestination(AppDestination.Home)
    }
    
    GraphNavHost(
        graph = appGraph,
        navigator = navigator,
        defaultTransition = NavigationTransitions.SlideHorizontal
    )
}
```

### Predictive Back Navigation

```kotlin
PredictiveBackNavigation(
    navigator = navigator,
    graph = appGraph,
    animationType = PredictiveBackAnimationType.Material3,
    enabled = true
)
```

## 📚 Documentation

Comprehensive documentation is available in the `quo-vadis-core/docs/` directory:

- **[Architecture Overview](quo-vadis-core/docs/ARCHITECTURE.md)** - Design principles, patterns, and architecture layers
- **[API Reference](quo-vadis-core/docs/API_REFERENCE.md)** - Complete API documentation with examples
- **[Navigation Implementation](quo-vadis-core/docs/NAVIGATION_IMPLEMENTATION.md)** - Implementation details and features
- **[Multiplatform Predictive Back](quo-vadis-core/docs/MULTIPLATFORM_PREDICTIVE_BACK.md)** - Advanced gesture navigation
- **[Shared Element Transitions](quo-vadis-core/docs/SHARED_ELEMENT_TRANSITIONS.md)** - Material Design shared elements guide

### 📖 Generating API Documentation

You can generate comprehensive HTML API documentation using Dokka:

```bash
# Generate HTML documentation
./gradlew :quo-vadis-core:dokkaGenerate

# View the generated documentation
open quo-vadis-core/build/dokka/html/index.html
```

The documentation includes:
- All public APIs with KDoc comments
- Source code links to GitHub
- External documentation links (Android, Coroutines)
- Package-level documentation
- Automatic suppression of internal packages

## 🎮 Demo Application

The **`composeApp`** module contains a comprehensive demo showcasing all navigation patterns:

### Demo Features

- **Bottom Navigation** - Tab-based navigation between main sections
- **Drawer Navigation** - Side drawer with feature access
- **Deep Stack Navigation** - Multi-level navigation with backstack
- **Modular Navigation** - Feature module pattern demonstration
- **Predictive Back Gestures** - Animated back navigation (Android 13+ & iOS)
- **Transitions** - Various animation styles (Fade, Slide, Scale, Material3)
- **Deep Links** - URI-based navigation examples
- **MVI Pattern** - MVI architecture integration
- **BackStack Manipulation** - Direct stack access examples

### Running the Demo

**Android:**
```bash
./gradlew :composeApp:assembleDebug
# Or install directly on connected device
./gradlew :composeApp:installDebug
```

**iOS:**
Open the `iosApp` directory in Xcode and run, or use:
```bash
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
# Then open in Xcode
open iosApp/iosApp.xcodeproj
```

**Web (JavaScript):**
```bash
./gradlew :composeApp:jsBrowserDevelopmentRun --continuous
# Opens at http://localhost:8080 with hot reload
```

**Web (WebAssembly):**
```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun --continuous
# Opens at http://localhost:8080 with hot reload
```

**Desktop:**
```bash
./gradlew :composeApp:run
# Launches native window application
```

## 🛠 Technology Stack

- **Kotlin**: 2.2.20 (Multiplatform)
- **Compose Multiplatform**: 1.9.0
- **Android**: Min SDK 24, Target/Compile SDK 36
- **iOS**: iosArm64, iosSimulatorArm64, iosX64
- **JavaScript**: IR compiler with Canvas rendering
- **WebAssembly**: Wasm-JS target with Canvas rendering
- **Desktop**: JVM target (Java 11+)
- **Gradle**: 8.14.3 (8GB heap)
- **AGP**: 8.11.2

### Build Configuration

- **Gradle Daemon**: 6GB (-Xmx6144M)
- **Gradle Build**: 8GB (-Xmx8192M)
- **Configuration Cache**: Enabled
- **Build Cache**: Enabled

## 🏗 Architecture Highlights

### Type-Safe Navigation
```kotlin
// No string-based routing!
navigator.navigate(DetailsDestination("123"))
```

### Direct BackStack Access
```kotlin
navigator.backStack.pop()
navigator.backStack.popTo("route")
navigator.backStack.clear()
```

### Modular Feature Navigation
```kotlin
class FeatureNavigation : BaseModuleNavigation() {
    override fun buildGraph() = navigationGraph("feature") {
        startDestination(Screen1)
        destination(Screen1) { _, nav -> Screen1UI(nav) }
    }
}
```

### MVI Integration
```kotlin
class MyViewModel(navigator: Navigator) : NavigationViewModel(navigator) {
    fun onAction() {
        handleNavigationIntent(NavigationIntent.Navigate(destination))
    }
}
```

## 🧪 Testing

The library includes comprehensive testing support:

```kotlin
@Test
fun `navigate to details screen`() {
    val fakeNavigator = FakeNavigator()
    val viewModel = MyViewModel(fakeNavigator)
    
    viewModel.onItemClicked("123")
    
    assertTrue(fakeNavigator.verifyNavigateTo("details"))
}
```

## 📱 Platform Support

The Quo Vadis library supports **7 platforms** across mobile, desktop, and web:

| Platform | Target                          | Status | Features |
|----------|---------------------------------|--------|----------|
| **Android** | `androidLibrary`                | ✅ Production | Predictive back, deep links, system integration |
| **iOS** | `iosArm64` `iosSimulatorArm64` `iosX64` | ✅ Production | Predictive back, universal links, navigation bar |
| **JavaScript** | `js(IR)`                        | ✅ Production | Browser history, Canvas rendering, PWA-ready |
| **WebAssembly** | `wasmJs`                        | ✅ Production | Near-native performance, modern browsers |
| **Desktop** | `jvm("desktop")`                | ✅ Production | Native windows (macOS, Windows, Linux) |

### Platform-Specific Features

**Android:**
- System back button integration
- Predictive back gestures (Android 13+)
- Deep link handling
- Activity lifecycle integration

**iOS:**
- Swipe-back navigation
- Navigation bar customization
- Universal links
- iOS-specific transitions

**Web (JS/Wasm):**
- Browser back button support
- URL-based routing
- Canvas-based rendering
- Single-page application (SPA) support
- Progressive Web App (PWA) compatible

**Desktop (JVM):**
- Native window controls (macOS, Windows, Linux)
- Keyboard shortcuts support
- Menu bar integration
- Multi-window support
- Native installers (DMG, MSI, DEB)

## 🎨 Animation Types

The library supports multiple animation styles:

- **None** - Instant navigation
- **Fade** - Smooth fade transition
- **SlideHorizontal** - Slide left/right
- **SlideVertical** - Slide up/down
- **ScaleIn** - Scale with fade
- **Material3** - Material Design 3 with scale, translate, and rounded corners
- **Custom** - Build your own with Compose animations

## 🔧 Build Commands

### Core Commands

```bash
# Clean build
./gradlew clean build

# Run all tests
./gradlew test

# Build library only
./gradlew :quo-vadis-core:build

# Lint check
./gradlew lint
```

### Platform-Specific Builds

**Android:**
```bash
./gradlew :composeApp:assembleDebug        # Debug APK
./gradlew :composeApp:assembleRelease      # Release APK
./gradlew :composeApp:installDebug         # Install on device
```

**iOS:**
```bash
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64  # M1/M2 simulator
./gradlew :composeApp:linkDebugFrameworkIosArm64           # Physical device
./gradlew :composeApp:linkDebugFrameworkIosX64             # Intel simulator
```

**Web:**
```bash
# Development (with hot reload)
./gradlew :composeApp:jsBrowserDevelopmentRun              # JavaScript
./gradlew :composeApp:wasmJsBrowserDevelopmentRun          # WebAssembly

# Production build
./gradlew :composeApp:jsBrowserDistribution                # JS bundle
./gradlew :composeApp:wasmJsBrowserDistribution            # Wasm bundle

# Build library
./gradlew :quo-vadis-core:jsJar                            # JS library
./gradlew :quo-vadis-core:wasmJsJar                        # Wasm library
```

**Desktop:**
```bash
./gradlew :composeApp:run                                  # Run app
./gradlew :composeApp:createDistributable                  # App bundle
./gradlew :composeApp:packageDistributionForCurrentOS      # Native installer

# Platform-specific installers
./gradlew :composeApp:packageDmg                           # macOS DMG
./gradlew :composeApp:packageMsi                           # Windows MSI
./gradlew :composeApp:packageDeb                           # Linux DEB

# Build library
./gradlew :quo-vadis-core:desktopJar                       # Desktop library
```

### Publishing

```bash
# Publish to Maven Local
./gradlew :quo-vadis-core:publishToMavenLocal

# Published artifacts location
ls ~/.m2/repository/com/jermey/quo/vadis/quo-vadis-core/0.1.0-SNAPSHOT/
```

## � Using the Library

### Add Dependency

First, publish to Maven Local:

```bash
./gradlew :quo-vadis-core:publishToMavenLocal
```

Then add to your project:

**build.gradle.kts:**
```kotlin
repositories {
    mavenLocal()  // Add this
    mavenCentral()
    google()
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.jermey.quo.vadis:quo-vadis-core:0.1.0-SNAPSHOT")
        }
    }
}
```

### Platform-Specific Setup

The library automatically includes platform-specific implementations:

- **Android**: AAR artifact with Activity integration
- **iOS**: Framework with UIKit integration
- **JavaScript**: JS bundle with Canvas rendering
- **WebAssembly**: Wasm binary with JS glue code
- **Desktop**: JAR with native window support

No additional configuration required - it just works! ✨

## �📖 Examples

Check out the demo app for complete working examples:

- **[DemoApp.kt](composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/DemoApp.kt)** - Main demo with drawer & bottom nav
- **[destinations/](composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/destinations/)** - All destination definitions
- **[graphs/](composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/graphs/)** - Navigation graph examples
- **[ui/screens/](composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/)** - Demo screens

### Platform Entry Points

- **Android**: `composeApp/src/androidMain/kotlin/MainActivity.kt`
- **iOS**: `composeApp/src/iosMain/kotlin/MainViewController.kt`
- **JavaScript**: `composeApp/src/jsMain/kotlin/main.js.kt`
- **WebAssembly**: `composeApp/src/wasmJsMain/kotlin/main.wasmJs.kt`
- **Desktop**: `composeApp/src/desktopMain/kotlin/main.desktop.kt`

## 🤝 Contributing

This is a demonstration project showcasing navigation patterns. Feel free to explore, learn, and adapt for your own projects!

## 📄 License

This project is licensed under the **GNU Lesser General Public License v2.1 (LGPL-2.1)**.

The LGPL allows you to:
- Use the library in proprietary applications
- Modify the library for your own use
- Distribute modified versions under the LGPL

However, if you modify the library and distribute it, you must:
- Make your modifications available under the LGPL
- Provide complete source code for the modified library
- Allow users to relink with modified versions

See the [LICENSE](LICENSE) file for the complete license text.

**Note**: This license applies to the `quo-vadis-core` navigation library. The demo application (`composeApp`) is provided as-is for educational purposes.

---

**Learn more:**
- [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)

---

Made with ❤️ using Kotlin Multiplatform and Compose Multiplatform

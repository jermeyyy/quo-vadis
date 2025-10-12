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
│       └── MULTIPLATFORM_PREDICTIVE_BACK.md
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
```shell
./gradlew :composeApp:assembleDebug
```

**iOS:**
Open the `iosApp` directory in Xcode and run, or use:
```shell
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

## 🛠 Technology Stack

- **Kotlin**: 2.2.20 (Multiplatform)
- **Compose Multiplatform**: 1.9.0
- **Android**: Min SDK 24, Target/Compile SDK 36
- **iOS**: iosArm64, iosSimulatorArm64, iosX64
- **Gradle**: 8.11+
- **AGP**: 8.11.2

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

| Platform | Support | Features |
|----------|---------|----------|
| Android  | ✅ Full  | Predictive back, deep links, system integration |
| iOS      | ✅ Full  | Predictive back, universal links, navigation bar |
| Desktop  | 🚧 Ready | Window-based navigation, keyboard shortcuts |
| Web      | 🚧 Ready | Browser history, URL routing (framework ready) |

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

```bash
# Clean build
./gradlew clean build

# Run all tests
./gradlew test

# Build library only
./gradlew :quo-vadis-core:build

# Build Android demo
./gradlew :composeApp:assembleDebug

# Build iOS framework
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64

# Lint check
./gradlew lint
```

## 📖 Examples

Check out the demo app for complete working examples:

- **[DemoApp.kt](composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/DemoApp.kt)** - Main demo with drawer & bottom nav
- **[destinations/](composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/destinations/)** - All destination definitions
- **[graphs/](composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/graphs/)** - Navigation graph examples
- **[ui/screens/](composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/)** - Demo screens

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

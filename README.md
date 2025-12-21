# Quo Vadis Navigation Library

<p align="center">
  <img src="art/logo.jpg" alt="Quo Vadis Logo" width="200"/>
</p>

[![Maven Central](https://img.shields.io/maven-central/v/io.github.jermeyyy/quo-vadis-core)](https://central.sonatype.com/artifact/io.github.jermeyyy/quo-vadis-core)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/kotlin-2.2.21-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.10.0--rc02-4285F4.svg?logo=jetpackcompose)](https://www.jetbrains.com/lp/compose-multiplatform/)

**"Quo Vadis"** (Latin for "Where are you going?") - A comprehensive, type-safe navigation library for Kotlin Multiplatform and Compose Multiplatform using a **tree-based navigation architecture**.

## 🎯 Project Overview

Quo Vadis provides a powerful navigation solution with:

1. **`quo-vadis-core`** - The navigation library (reusable, no external dependencies)
2. **`quo-vadis-annotations`** - KSP annotations (`@Stack`, `@Destination`, `@Screen`, `@Tabs`, `@Pane`)
3. **`quo-vadis-ksp`** - Code generator for zero-boilerplate navigation
4. **`quo-vadis-core-flow-mvi`** - Optional FlowMVI integration
5. **`composeApp`** - Demo application showcasing all navigation patterns

## ✨ Key Features

- ✅ **NavNode Tree Architecture** - Hierarchical navigation state as a tree of nodes
- ✅ **Annotation-based API** - KSP code generation for zero-boilerplate navigation
- ✅ **Type-Safe Navigation** - Compile-time safety with no string-based routing
- ✅ **Multiple Container Types** - `@Stack`, `@Tabs`, and `@Pane` for different navigation patterns
- ✅ **Type-Safe Arguments** - `@Argument` annotation with automatic deep link serialization
- ✅ **Multiplatform** - Works on Android, iOS, Desktop, Web (JS & WASM)
- ✅ **Predictive Back Navigation** - Smooth animated back gestures (Android 13+ & iOS)
- ✅ **Shared Element Transitions** - Material Design shared elements (forward & back!)
- ✅ **Tabbed Navigation** - Independent backstacks per tab with `@Tabs` + `@TabItem`
- ✅ **Adaptive Layouts** - Multi-pane layouts with `@Pane` + `@PaneItem`
- ✅ **Custom Transitions** - `@Transition` annotation with preset and custom animations
- ✅ **Deep Link Support** - URI-based navigation with automatic parameter extraction
- ✅ **Hierarchical Rendering** - True parent-child composition with coordinated animations
- ✅ **Navigation Results** - Type-safe result passing between screens
- ✅ **DI Framework Support** - Easy integration with Koin, Kodein, etc.
- ✅ **Testable** - `FakeNavigator` for unit testing

## 📦 Project Structure

```
NavPlayground/
├── quo-vadis-core/              # Core navigation library
│   └── src/
│       ├── commonMain/          # Core navigation logic (Navigator, NavNode, TreeNavigator)
│       ├── androidMain/         # Android-specific features (predictive back)
│       └── iosMain/             # iOS-specific features (swipe back)
├── quo-vadis-annotations/       # Annotation definitions
│   └── src/commonMain/          # @Stack, @Destination, @Screen, @Tabs, @Pane, etc.
├── quo-vadis-ksp/               # KSP code generator
│   └── src/main/                # Processor implementation
├── quo-vadis-core-flow-mvi/     # FlowMVI integration (optional)
│   └── src/commonMain/          # MVI navigation integration
├── composeApp/                  # Demo application
│   └── src/
│       ├── commonMain/          # Demo screens & examples
│       ├── androidMain/         # Android app entry point
│       └── iosMain/             # iOS app entry point
├── iosApp/                      # iOS app wrapper
└── docs/
    ├── refactoring-plan/        # Architecture documentation
    └── site/                    # Documentation website
```

## 🚀 Quick Start

### Installation

Add the library to your Kotlin Multiplatform project:

```kotlin
// build.gradle.kts
plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp") version "2.3.0"
}

repositories {
    mavenCentral()
    google()
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.jermeyyy:quo-vadis-core:0.2.0")
            implementation("io.github.jermeyyy:quo-vadis-annotations:0.2.0")
        }
    }
}

dependencies {
    // KSP code generator (all targets)
    add("kspCommonMainMetadata", "io.github.jermeyyy:quo-vadis-ksp:0.2.0")
}
```

### Basic Stack Navigation

Define destinations using `@Stack` and `@Destination` annotations:

```kotlin
import com.jermey.quo.vadis.annotations.*
import com.jermey.quo.vadis.core.navigation.core.NavDestination

// 1. Define a navigation stack with destinations
@Stack(name = "home", startDestination = Feed::class)
sealed class HomeDestination : NavDestination {

    @Destination(route = "home/feed")
    data object Feed : HomeDestination()

    @Destination(route = "home/article/{articleId}")
    data class Article(
        @Argument val articleId: String,
        @Argument(optional = true) val showComments: Boolean = false
    ) : HomeDestination()

    @Destination(route = "home/settings")
    data object Settings : HomeDestination()
}
```

### Screen Binding with `@Screen`

Bind Composable functions to destinations:

```kotlin
import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.core.Navigator

// Simple screen (data object destination)
@Screen(HomeDestination.Feed::class)
@Composable
fun FeedScreen(navigator: Navigator) {
    Column {
        Text("Feed")
        Button(onClick = { 
            navigator.navigate(HomeDestination.Article(articleId = "123"))
        }) {
            Text("View Article")
        }
    }
}

// Screen with arguments (data class destination)
@Screen(HomeDestination.Article::class)
@Composable
fun ArticleScreen(destination: HomeDestination.Article, navigator: Navigator) {
    Column {
        Text("Article: ${destination.articleId}")
        if (destination.showComments) {
            Text("Comments visible")
        }
        Button(onClick = { navigator.navigateBack() }) {
            Text("Back")
        }
    }
}
```

### Setting Up Navigation

```kotlin
import com.jermey.quo.vadis.core.navigation.core.TreeNavigator
import com.jermey.quo.vadis.generated.GeneratedNavigationConfig

@Composable
fun App() {
    val config = GeneratedNavigationConfig
    
    // Build the initial NavNode tree from your root destination
    val initialState = remember {
        config.buildNavNode(
            destinationClass = HomeDestination::class,
            parentKey = null
        )!!
    }
    
    // Create the navigator with config
    val navigator = remember {
        TreeNavigator(
            config = config,
            initialState = initialState
        )
    }
    
    // Render the navigation tree - config read from navigator
    QuoVadisHost(
        navigator = navigator,
        screenRegistry = config.screenRegistry
    )
}
```

## 📱 Navigation Patterns

### Tabbed Navigation with `@Tabs`

Create bottom navigation or tab bars with independent backstacks:

```kotlin
// Define each tab as @TabItem + @Stack
@TabItem(label = "Home", icon = "home")
@Stack(name = "homeStack", startDestination = HomeTab.Feed::class)
sealed class HomeTab : NavDestination {
    @Destination(route = "home/feed")
    data object Feed : HomeTab()
    
    @Destination(route = "home/article/{id}")
    data class Article(@Argument val id: String) : HomeTab()
}

@TabItem(label = "Explore", icon = "explore")
@Stack(name = "exploreStack", startDestination = ExploreTab.Root::class)
sealed class ExploreTab : NavDestination {
    @Destination(route = "explore/root")
    data object Root : ExploreTab()
}

// Define the tabs container
@Tabs(
    name = "mainTabs",
    initialTab = HomeTab::class,
    items = [HomeTab::class, ExploreTab::class]
)
object MainTabs
```

#### Tab Container UI

Provide the tab bar UI with `@TabsContainer`:

```kotlin
@TabsContainer(MainTabs::class)
@Composable
fun MainTabsWrapper(scope: TabsContainerScope, content: @Composable () -> Unit) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                scope.tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = index == scope.activeIndex,
                        onClick = { scope.switchTab(index) },
                        icon = { Icon(tabIcon(tab.icon), tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            content()
        }
    }
}
```

### Adaptive Pane Layouts with `@Pane`

Create responsive list-detail layouts:

```kotlin
@Pane(name = "catalog", backBehavior = PaneBackBehavior.PopUntilContentChange)
sealed class CatalogPane : NavDestination {

    @PaneItem(role = PaneRole.PRIMARY, rootGraph = ProductListGraph::class)
    @Destination(route = "catalog/list")
    data object List : CatalogPane()

    @PaneItem(
        role = PaneRole.SECONDARY,
        adaptStrategy = AdaptStrategy.OVERLAY,
        rootGraph = ProductDetailGraph::class
    )
    @Destination(route = "catalog/detail/{id}")
    data class Detail(@Argument val id: String) : CatalogPane()
}
```

### Custom Transitions with `@Transition`

Specify transition animations per destination:

```kotlin
@Stack(name = "home", startDestination = "List")
sealed class HomeDestination : NavDestination {

    // Default transition
    @Destination(route = "list")
    data object List : HomeDestination()

    // Horizontal slide for detail screens
    @Transition(type = TransitionType.SlideHorizontal)
    @Destination(route = "details/{id}")
    data class Details(@Argument val id: String) : HomeDestination()

    // Vertical slide for modals
    @Transition(type = TransitionType.SlideVertical)
    @Destination(route = "filter")
    data object Filter : HomeDestination()

    // Fade for overlays
    @Transition(type = TransitionType.Fade)
    @Destination(route = "help")
    data object Help : HomeDestination()
}
```

## 🏗 Architecture: NavNode Tree

Quo Vadis uses a tree-based navigation architecture where the navigation state is represented as a tree of nodes:

```
NavNode (root)
├── StackNode (main stack)
│   ├── ScreenNode (Home)
│   ├── ScreenNode (List)
│   └── ScreenNode (Detail)
├── TabNode (bottom tabs)
│   ├── StackNode (Tab 1 stack)
│   │   └── ScreenNode
│   └── StackNode (Tab 2 stack)
│       └── ScreenNode
└── PaneNode (adaptive layout)
    ├── StackNode (primary)
    └── StackNode (detail)
```

### Node Types

| Node | Purpose | Annotation |
|------|---------|------------|
| `ScreenNode` | Single screen/destination | `@Destination` |
| `StackNode` | Stack of screens (push/pop) | `@Stack` |
| `TabNode` | Tab container with independent stacks | `@Tabs` |
| `PaneNode` | Adaptive multi-pane layout | `@Pane` |

### Navigator Interface

```kotlin
interface Navigator {
    val state: StateFlow<NavNode>
    val currentDestination: StateFlow<NavDestination?>
    val canNavigateBack: StateFlow<Boolean>
    
    // Basic navigation
    fun navigate(destination: NavDestination)
    fun navigateBack(): Boolean
    
    // Advanced navigation
    fun navigateAndClearTo(destination: NavDestination)
    fun navigateAndReplace(destination: NavDestination)
    
    // Pane navigation
    fun navigateToPane(role: PaneRole, destination: NavDestination)
    fun switchPane(role: PaneRole)
    
    // Deep links
    fun handleDeepLink(uri: String): Boolean
}
```

## 🎨 Shared Element Transitions

Enable beautiful shared element animations:

```kotlin
@Screen(HomeDestination.Article::class)
@Composable
fun ArticleScreen(
    destination: HomeDestination.Article,
    navigator: Navigator,
    sharedTransitionScope: SharedTransitionScope?,
    animatedVisibilityScope: AnimatedVisibilityScope?
) {
    // Use shared elements when scopes are available
    if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        Image(
            modifier = Modifier.quoVadisSharedElement(
                key = "article-image-${destination.articleId}",
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )
        )
    }
}
```

**Key rules:**
- Use `quoVadisSharedElement()` for icons/images
- Use `quoVadisSharedBounds()` for text/containers
- Keys must match exactly between source and destination screens
- Works in both forward and backward navigation

## 🛠 Technology Stack

- **Kotlin**: 2.2.21 (Multiplatform)
- **Compose Multiplatform**: 1.10.0-rc02
- **KSP**: 2.3.0
- **Android**: Min SDK 24, Target/Compile SDK 36
- **iOS**: iosArm64, iosSimulatorArm64, iosX64
- **JavaScript**: IR compiler with Canvas rendering
- **WebAssembly**: Wasm-JS target with Canvas rendering
- **Desktop**: JVM target (Java 11+)
- **Gradle**: 8.14.3
- **AGP**: 8.13.2

### Key Dependencies

- `kotlinx-serialization-json`: 1.9.0 - Deep link serialization
- `kotlinx-coroutines`: 1.10.2 - Async navigation
- `FlowMVI`: 3.2.1 - MVI integration (optional)
- `Koin`: 4.2.0-beta2 - DI support (optional)

## 📱 Platform Support

| Platform | Target | Status | Features |
|----------|--------|--------|----------|
| **Android** | `androidLibrary` | ✅ Production | Predictive back, deep links, system integration |
| **iOS** | `iosArm64` `iosSimulatorArm64` `iosX64` | ✅ Production | Swipe back, universal links |
| **JavaScript** | `js(IR)` | ✅ Production | Browser history, Canvas rendering |
| **WebAssembly** | `wasmJs` | ✅ Production | Near-native performance |
| **Desktop** | `jvm("desktop")` | ✅ Production | Native windows (macOS, Windows, Linux) |

## 🎮 Demo Application

The `composeApp` module showcases all navigation patterns:

- **Tabbed Navigation** - Bottom navigation with `@Tabs`
- **Stack Navigation** - Push/pop with `@Stack`
- **Master-Detail** - Adaptive layouts with `@Pane`
- **Auth Flows** - Scope-aware navigation
- **Deep Links** - URI parameter handling
- **Transitions** - Custom animations
- **Result Handling** - Navigation results

### Running the Demo

```bash
# Android
./gradlew :composeApp:installDebug

# iOS (Apple Silicon simulator)
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
open iosApp/iosApp.xcodeproj

# Web (JavaScript)
./gradlew :composeApp:jsBrowserDevelopmentRun --continuous

# Web (WebAssembly)
./gradlew :composeApp:wasmJsBrowserDevelopmentRun --continuous

# Desktop
./gradlew :composeApp:run
```

## 🧪 Testing

```kotlin
@Test
fun `navigate to details screen`() {
    val config = GeneratedNavigationConfig
    val initialState = config.buildNavNode(HomeDestination::class, null)!!
    // For testing, config can be passed or use defaults (NavigationConfig.Empty)
    val navigator = TreeNavigator(config = config, initialState = initialState)
    
    navigator.navigate(HomeDestination.Article(articleId = "123"))
    
    assertEquals(
        HomeDestination.Article(articleId = "123"),
        navigator.currentDestination.value
    )
}
```

## 📚 Documentation

- **Website**: [https://jermeyyy.github.io/quo-vadis/](https://jermeyyy.github.io/quo-vadis/)
- **API Reference**: Auto-generated via Dokka

```bash
# Generate API docs
./gradlew :quo-vadis-core:dokkaGenerate
open quo-vadis-core/build/dokka/html/index.html
```

## 🔧 Build Commands

```bash
# Full build
./gradlew clean build

# Run tests
./gradlew test

# Build library only
./gradlew :quo-vadis-core:build

# Lint check
./gradlew lint
```

### Platform-Specific Builds

```bash
# Android
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:installDebug

# iOS
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64

# Web
./gradlew :composeApp:jsBrowserDevelopmentRun
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# Desktop
./gradlew :composeApp:run
```

## 🤝 Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

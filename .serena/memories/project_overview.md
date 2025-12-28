# Quo Vadis Navigation Library - Project Overview

## What is Quo Vadis?

**Quo Vadis** (Latin for "Where are you going?") is a comprehensive, type-safe navigation library for **Kotlin Multiplatform** and **Compose Multiplatform**.

## Repository Information

- **Repository**: `jermeyyy/quo-vadis`
- **License**: MIT
- **Published**: Maven Central (`io.github.jermeyyy:quo-vadis-*`)

## Core Components

### 1. `quo-vadis-core` - Navigation Library
The core navigation library with full multiplatform support:
- Core navigation interfaces (`Navigator`, `Destination`, `NavNode`)
- Graph builders and DSL
- Compose integration (`NavigationHost`)
- Animation and transition support
- Predictive back navigation
- **Lifecycle management** via `LifecycleAwareNode`
- **No external dependencies** - can be used standalone

### 2. `quo-vadis-annotations` - Annotation Definitions
Annotation definitions for KSP code generation:
- `@Stack` - Marks a sealed class as a navigation stack
- `@Destination` - Defines the route for a destination
- `@Argument` - Specifies type-safe arguments
- `@Screen` - Associates content composables with destinations
- `@Tabs` / `@TabItem` - Tabbed navigation containers
- `@Pane` / `@PaneItem` - Adaptive pane layouts
- `@TabsContainer` / `@PaneContainer` - Container wrapper composables

### 3. `quo-vadis-ksp` - Code Generator
KSP processor that generates:
- Navigation config (`*NavigationConfig`)
- Deep link handler (`*DeepLinkHandler`)
- Screen registry
- Container registry

### 4. `quo-vadis-core-flow-mvi` - FlowMVI Integration
Optional module for MVI architecture integration:
- `NavigationContainer` - Screen-scoped MVI containers
- `SharedNavigationContainer` - Tab/Pane-scoped shared MVI containers
- `rememberContainer` / `rememberSharedContainer` composables
- Koin integration with `navigationContainer` / `sharedNavigationContainer`

### 5. `composeApp` - Demo Application
Comprehensive demo showcasing all navigation patterns:
- Bottom navigation, drawer navigation
- Deep stack navigation
- Predictive back gestures
- Various transitions
- Deep links
- FlowMVI pattern with screen and shared containers

## Key Features

- ✅ **Tree-based Navigation** - NavNode tree architecture
- ✅ **Annotation-based API** - KSP code generation for zero-boilerplate navigation
- ✅ **Type-Safe Arguments** - Serializable data classes with automatic wiring
- ✅ **Type-Safe Navigation** - Compile-time safety, no string-based routing
- ✅ **Multiplatform** - Android, iOS, Desktop, Web (JS & WASM)
- ✅ **Lifecycle Management** - `LifecycleAwareNode` for proper lifecycle state
- ✅ **FlowMVI Integration** - Screen and container-scoped MVI containers
- ✅ **Deep Link Support** - URI-based navigation with pattern matching
- ✅ **Predictive Back Navigation** - Smooth animated back gestures
- ✅ **Shared Element Transitions** - Material Design shared elements
- ✅ **Tabbed Navigation** - Independent backstacks per tab
- ✅ **Hierarchical Rendering** - Parent-child composition with coordinated animations
- ✅ **Navigation Results** - Type-safe result passing with `navigateForResult`
- ✅ **Testable** - `FakeNavigator` for unit testing

## Supported Platforms

| Platform | Target | Status |
|----------|--------|--------|
| Android | `androidLibrary` | ✅ Production |
| iOS | `iosArm64`, `iosSimulatorArm64`, `iosX64` | ✅ Production |
| JavaScript | `js(IR)` | ✅ Production |
| WebAssembly | `wasmJs` | ✅ Production |
| Desktop | `jvm("desktop")` | ✅ Production |

## Package Structure

```
com.jermey.quo.vadis.core.navigation/
├── core/          # Core interfaces (Navigator, NavNode, TreeMutator, LifecycleAwareNode)
├── compose/       # Compose integration
│   ├── animation/ # Transitions and animations
│   ├── navback/   # Predictive back navigation
│   ├── registry/  # Route, Screen, Container registries
│   ├── render/    # Tree rendering and composition
│   └── wrapper/   # Tab and Pane wrapper scopes
├── serialization/ # State serialization
├── testing/       # FakeNavigator, FakeNavRenderScope
└── utils/         # Extension functions

com.jermey.quo.vadis.flowmvi/
├── NavigationContainer.kt        # Screen-scoped MVI container
├── NavigationContainerScope.kt   # Koin scope for screen containers
├── SharedNavigationContainer.kt  # Tab/Pane-scoped shared container
├── SharedContainerScope.kt       # Koin scope for shared containers
└── ContainerComposables.kt       # rememberContainer, rememberSharedContainer
```

## Documentation

- **Website**: https://jermeyyy.github.io/quo-vadis/
- **API Reference**: Auto-generated via Dokka

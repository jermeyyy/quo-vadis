# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Quo Vadis** is a type-safe, tree-based navigation library for Kotlin Multiplatform and Compose Multiplatform. It uses KSP for code generation from annotations (`@Stack`, `@Destination`, `@Screen`, `@Tabs`, `@Pane`).

## Build Commands

```bash
# Full build
./gradlew clean build

# Run tests (all platforms)
./gradlew test

# Desktop tests only (fastest for core library)
./gradlew :quo-vadis-core:desktopTest

# Run single test class
./gradlew :quo-vadis-core:desktopTest --tests "com.jermey.quo.vadis.core.YourTestClass"

# Lint with detekt (auto-correct enabled)
./gradlew detekt

# Build library only
./gradlew :quo-vadis-core:build

# Generate API docs
./gradlew :quo-vadis-core:dokkaGenerate
```

### Platform-Specific Builds

```bash
# Android
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:installDebug

# iOS (Apple Silicon simulator)
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64

# Web (JS)
./gradlew :composeApp:jsBrowserDevelopmentRun --continuous

# Web (WASM)
./gradlew :composeApp:wasmJsBrowserDevelopmentRun --continuous

# Desktop
./gradlew :composeApp:run
```

## Module Structure

| Module | Purpose |
|--------|---------|
| `quo-vadis-core` | Core navigation library (Navigator, NavNode tree, Compose rendering) |
| `quo-vadis-annotations` | KSP annotations (`@Stack`, `@Destination`, `@Screen`, `@Tabs`, `@Pane`) |
| `quo-vadis-ksp` | KSP processor for code generation |
| `quo-vadis-gradle-plugin` | Gradle plugin for simplified KSP configuration |
| `quo-vadis-core-flow-mvi` | Optional FlowMVI integration |
| `composeApp` | Demo application showcasing all navigation patterns |
| `feature1`, `feature2` | Multi-module navigation examples |

## Architecture

### NavNode Tree

Navigation state is represented as a tree of immutable nodes:

```
NavNode (sealed interface)
├── ScreenNode     - Single screen destination
├── StackNode      - Stack of screens (push/pop)
├── TabNode        - Tab container with independent stacks
└── PaneNode       - Adaptive multi-pane layout
```

### Key Package Structure (quo-vadis-core)

```
com.jermey.quo.vadis.core/
├── navigation/           # Pure Kotlin navigation logic (Compose-free)
│   ├── node/             # NavNode types (ScreenNode, StackNode, TabNode, PaneNode)
│   ├── destination/      # NavDestination, DeepLink
│   ├── navigator/        # Navigator interface, PaneNavigator
│   ├── result/           # Navigation results
│   ├── transition/       # Transition definitions (interface only)
│   ├── pane/             # Pane configuration (PaneRole, AdaptStrategy)
│   ├── config/           # NavigationConfig interface
│   └── internal/         # TreeNavigator, TreeMutator, operations (@InternalQuoVadisApi)
│
├── compose/              # Compose rendering layer
│   ├── NavigationHost.kt # Main entry point
│   ├── transition/       # NavTransition, TransitionBuilder, shared elements
│   ├── animation/        # SharedElementModifiers (quoVadisSharedElement)
│   ├── navback/          # NavBackHandler for back navigation
│   ├── scope/            # CompositionLocals, container scopes
│   └── internal/         # Renderers, animation controllers (@InternalQuoVadisApi)
│
├── registry/             # Extensibility interfaces
│   ├── ScreenRegistry, ContainerRegistry, TransitionRegistry, etc.
│   └── internal/         # Composite* implementations
│
└── dsl/                  # Manual configuration DSL (navigationConfig {})
    └── internal/         # DSL implementations
```

### Domain Separation

- **`navigation/`**: Pure Kotlin, no Compose dependencies. Contains navigation state model and operations.
- **`compose/`**: All Compose-specific code. Renders the NavNode tree to UI.
- **`registry/`**: Interfaces for extending navigation (screens, containers, transitions).
- **`dsl/`**: Fluent API for manual navigation configuration.

### Internal API Marker

Classes marked with `@InternalQuoVadisApi` are implementation details for KSP-generated code or advanced use cases. They require opt-in and may change between versions.

## Key Abstractions

### Navigator Interface

```kotlin
interface Navigator {
    val state: StateFlow<NavNode>
    val currentDestination: StateFlow<NavDestination?>
    val canNavigateBack: StateFlow<Boolean>

    fun navigate(destination: NavDestination)
    fun navigateBack(): Boolean
    fun navigateAndClearTo(destination: NavDestination)
    fun navigateAndReplace(destination: NavDestination)
}
```

### TreeNavigator

The main `Navigator` implementation. Uses `TreeMutator` to perform immutable tree transformations. Located at `navigation/internal/tree/TreeNavigator.kt`.

### TreeMutator

Facade for all tree operations (push, pop, back, tab switch, pane navigation). Delegates to specialized operation objects in `navigation/internal/tree/operations/`.

## Code Generation (KSP)

KSP generates:
- `{Prefix}NavigationConfig` - Implements `NavigationConfig` with screen/container registries
- Screen registry entries from `@Screen` annotations
- Deep link handlers from `@Destination(route = "...")` routes
- Tab metadata from `@Tabs` + `@TabItem` annotations

Module prefix is configured via:
```kotlin
quoVadis {
    modulePrefix = "MyApp"  // Generates MyAppNavigationConfig
}
```

## Technology Stack

- Kotlin: 2.3.0
- Compose Multiplatform: 1.10.0-rc02
- KSP: 2.3.0
- Targets: Android (24+), iOS, Desktop (JVM 11+), JS, WASM

## Active Refactoring Plans

See `docs/refactoring-plan/` for ongoing architectural improvements:
- `quo-vadis-core-package-refactoring.md` - Package structure reorganization
- `navigator-interface-refactoring.md` - Interface segregation
- `tree-mutator-refactoring.md` - TreeMutator decomposition
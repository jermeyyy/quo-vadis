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
- **No external dependencies** - can be used standalone

### 2. `quo-vadis-annotations` - Annotation Definitions
Annotation definitions for KSP code generation:
- `@Graph` - Marks a sealed class as a navigation graph
- `@Route` - Defines the route for a destination
- `@Argument` - Specifies type-safe arguments
- `@Content` - Associates content composables with destinations

### 3. `quo-vadis-ksp` - Code Generator
KSP processor that generates:
- Route registration (`*RouteInitializer`)
- Graph builder functions (`build*Graph()`)
- Typed destination extensions

### 4. `quo-vadis-core-flow-mvi` - FlowMVI Integration
Optional module for MVI architecture integration with FlowMVI library.

### 5. `composeApp` - Demo Application
Comprehensive demo showcasing all navigation patterns:
- Bottom navigation, drawer navigation
- Deep stack navigation
- Predictive back gestures
- Various transitions
- Deep links
- FlowMVI pattern

## Key Features

- ✅ **Annotation-based API** - KSP code generation for zero-boilerplate navigation
- ✅ **Type-Safe Arguments** - Serializable data classes with automatic wiring
- ✅ **Type-Safe Navigation** - Compile-time safety, no string-based routing
- ✅ **Multiplatform** - Android, iOS, Desktop, Web (JS & WASM)
- ✅ **Modular Architecture** - Gray box pattern for feature modules
- ✅ **Deep Link Support** - URI-based navigation with pattern matching
- ✅ **Predictive Back Navigation** - Smooth animated back gestures
- ✅ **Shared Element Transitions** - Material Design shared elements
- ✅ **Tabbed Navigation** - Independent backstacks per tab
- ✅ **Hierarchical Rendering** - Parent-child composition with coordinated animations
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
├── core/          # Core interfaces (Navigator, Destination, NavNode, TreeMutator)
├── compose/       # Compose integration
│   ├── animation/ # Transitions and animations
│   ├── navback/   # Predictive back navigation
│   ├── registry/  # Route, Screen, Container registries
│   ├── render/    # Tree rendering and composition
│   └── wrapper/   # Tab and Pane wrapper scopes
├── integration/   # External integrations (Koin)
├── serialization/ # State serialization
├── testing/       # FakeNavigator, FakeNavRenderScope
└── utils/         # Extension functions
```

## Documentation

- **Website**: https://jermeyyy.github.io/quo-vadis/
- **Local docs**: `quo-vadis-core/docs/` directory (currently empty in new architecture)
- **API Reference**: Auto-generated via Dokka

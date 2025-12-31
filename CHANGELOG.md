# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.3.2] - 2025-12-31

### Added

- **Navigator Interface Segregation**: Refactored Navigator interface following Interface Segregation Principle (ISP):
  - `PaneNavigator` - Extension interface for pane-specific operations (`navigateToPane`, `navigateBackInPane`, `isPaneAvailable`)
  - `TransitionController` - Internal interface for animation control (`updateTransitionProgress`, `startPredictiveBack`, etc.)
  - `ResultCapable` - Internal interface for result passing (`resultManager`)
  - `InternalQuoVadisApi` - Opt-in annotation for internal APIs
- **navigateToPane API**: New method for replacing pane content instead of pushing, ideal for list-detail patterns.

### Changed

- **Navigator Interface Slimmed**: Core Navigator interface reduced from 26 to 14 public members (46% reduction). Advanced operations now accessed via extension interfaces.
- **PaneNode Cache Keys**: Now include `hashCode()` to prevent cache conflicts during predictive back animations.

### Fixed

- **Predictive Back State Tracking**: Fixed `AnimatedNavContent` to use object reference comparison (`===`) instead of key comparison for proper state tracking during predictive back gestures.
- **Transition Direction Detection**: Fixed transition direction detection during back navigation.
- **Predictive Back Animation**: Fixed animation jerkiness and incorrect node rendering during predictive back.

## [0.3.1] - 2025-12-29

### Fixed

- **Gradle Plugin Publication**: Added `quo-vadis-gradle-plugin` to Maven Central publication workflow. The Gradle plugin is now published alongside other artifacts.
- **Import Fix**: Fixed malformed import in `ItemCard.kt` demo component.

### Changed

- **Updated publish-local.sh**: Script now publishes all artifacts including the Gradle plugin.

## [0.3.0] - 2025-12-29

### ⚠️ Breaking Changes

- **Package Restructure**: The library has been reorganized for improved modularity. Imports may need to be updated:
  - `navigation.compose.*` → `compose.*` (NavigationHost, animations, rendering)
  - `navigation.compose.wrapper` → `compose.wrapper` (container scopes)
  - `navigation.compose.navback` → `compose.navback` (back handling)
  - Core navigation types remain in `navigation.*`

### Added

- **Shared Element Transitions**: First-class support for Compose shared element transitions via `LocalTransitionScope` and `currentSharedTransitionScope()`. Enables smooth hero animations between screens using native Compose APIs.
- **State-Driven Navigation Demo**: New demo showcasing state-driven navigation patterns with `SharedContainer` integration.
- **Enhanced MVI Container Lifecycle**: Improved lifecycle management for FlowMVI containers with proper result delivery.

### Changed

- **Package Structure Reorganization**: Code reorganized into logical subpackages for better discoverability:
  - `compose/` - All Compose UI components (NavigationHost, animations, rendering)
  - `compose/animation/` - Transitions, shared elements, animation coordination
  - `compose/render/` - Screen, Stack, Tab, Pane renderers
  - `compose/navback/` - Back navigation handling, predictive back
  - `compose/wrapper/` - Container scopes (tabs, panes)
  - `navigation/` - Core navigation types (Navigator, NavNode, NavDestination)
  - `navigation/tree/` - TreeNavigator and TreeMutator
  - `navigation/pane/` - Pane configuration and roles
  - `navigation/config/` - Navigation configuration
  - `dsl/` - DSL builders and registries
- **AGP 9.0 Migration**: Updated to Android Gradle Plugin 9.0 with new KMP Android plugin.
- **Compose Resources Configuration**: Updated resource handling for multiplatform.

### Fixed

- **NavigateForResult Issues**: Resolved container lifecycle and result delivery problems.
- **MVI Container Lifecycle**: Fixed lifecycle management ensuring proper cleanup and state preservation.

### Documentation

- Added comprehensive lifecycle and savedstate refactoring documentation.
- Updated shared element transition examples and best practices.

## [0.2.1] - 2025-12-23

### ⚠️ Breaking Changes - Deprecated API Removal

The following deprecated APIs have been removed as part of the planned cleanup:

#### Navigator Interface
- **Removed**: `Navigator.navigateToPane(role, destination, switchFocus, transition)` - Use `navigate(destination)` instead. The navigation system automatically targets the correct pane based on the destination.
- **Removed**: `Navigator.switchPane(role)` - Navigate to a destination in the target pane instead.

#### Type Aliases
- **Removed**: `PaneWrapperScope` - Use `PaneContainerScope` instead.
- **Removed**: `TabWrapperScope` - Use `TabsContainerScope` instead.

#### Functions
- **Removed**: `createTabWrapperScope()` - Use `createTabsContainerScope()` instead.

#### NavigationHost
- **Removed**: `NavigationHost(navigator, config, ...)` overload - Config should only be passed to `rememberQuoVadisNavigator()`. Use `NavigationHost(navigator)` instead.

#### Annotations
- **Removed**: `@TabItem.rootGraph` parameter - Use the new pattern with separate `@TabItem` and `@Stack` annotations instead.

### Migration Guide

```kotlin
// Before (removed)
navigator.navigateToPane(PaneRole.Supporting, DetailDestination(itemId))
navigator.switchPane(PaneRole.Primary)

// After
navigator.navigate(DetailDestination(itemId))  // Automatically targets correct pane

// Before (removed)
val navigator = rememberQuoVadisNavigator(MainTabs::class, config)
NavigationHost(navigator, config)  // Config passed twice

// After
val navigator = rememberQuoVadisNavigator(MainTabs::class, config)
NavigationHost(navigator)  // Config read from navigator

// Before (removed - @TabItem.rootGraph)
@TabItem(label = "Home", icon = "home", rootGraph = HomeDestination::class)
data object Home : MainTabs()

// After (use @TabItem + @Stack pattern)
@TabItem(label = "Home", icon = "home")
@Stack(name = "homeStack", startDestination = Feed::class)
sealed class HomeTab : Destination { ... }
```

## [0.2.0] - 2025-12-19

### ⚠️ Breaking Changes

This release introduces a complete architecture refactor from linear backstack to tree-based navigation.

- **NavNode Tree Architecture**: Navigation state is now represented as a tree of nodes (`ScreenNode`, `StackNode`, `TabNode`, `PaneNode`) instead of a flat list of destinations.
- **Renamed Interface**: `Destination` interface renamed to `NavDestination` to avoid naming conflicts.
- **New Navigator**: `TreeNavigator` replaces the previous Navigator implementation with tree-based state management.
- **Unified @Stack Annotation**: The `@Stack` annotation now uses `startDestination` with `KClass` instead of separate string/class parameters.
- **Removed Legacy APIs**: Deprecated APIs from 0.1.x have been removed.

### Added

- **Tree-Based Navigation Architecture**: Complete rewrite with hierarchical `NavNode` structure.
- **New Annotations**:
  - `@Destination` - Define navigation destinations
  - `@Screen` - Bind Composable functions to destinations
  - `@Stack` - Define navigation stacks with `startDestination`
  - `@Tabs` / `@TabItem` - Define tabbed navigation containers
  - `@Pane` / `@PaneItem` - Define adaptive multi-pane layouts
  - `@Transition` - Specify custom transition animations per destination
  - `@Argument` - Type-safe navigation arguments with automatic deep link serialization
- **KSP Code Generation**: Complete rewrite of KSP processors:
  - `NavNode` builder generator
  - Screen registry generator
  - Navigator extensions generator
  - Deep link handler generator
  - Enhanced validation and error reporting
- **Hierarchical Rendering**: True parent-child composition with coordinated animations via `NavigationHost`.
- **Container Registry**: Unified container-aware navigation with `ContainerRegistry`.
- **Navigation with Results**: Type-safe `NavigationResult` API for passing data back from screens.
- **Navigation Lifecycle**: `NavigationLifecycle` API for observing navigation events.
- **TreeMutator**: Core tree manipulation operations (`push`, `pop`, `replace`, `clear`).
- **Predictive Back Integration**: Seamless integration with Android 13+ predictive back gesture and speculative pop.
- **Tab-Aware Navigation**: Automatic tab switching when navigating to destinations in different tabs (`switchTab()` deprecated).
- **Comprehensive Test Suite**: Unit tests for core navigation logic.
- **Demo App Rewrite**: Complete rewrite demonstrating all navigation patterns.

### Changed

- **Navigator Interface**: Now exposes `StateFlow<NavNode>` for tree-based state observation.
- **Migration to Koin**: Demo app migrated to `KoinApplication` pattern.
- **Rendering Architecture**: Moved from flattened rendering to hierarchical composition.

### Removed

- **Legacy APIs**: Removed deprecated `BasicDestination`, `RenderingMode.Flattened`, and legacy tab graph extractor.
- **String-Based Routing**: Removed string-based navigation in favor of type-safe `NavDestination` classes.

### Documentation

- Updated README with NavNode tree architecture and new annotations.
- Added comprehensive migration examples and recipes.

## [0.1.1] - 2025-11-23

### Added
- **Tabbed Navigation**: Complete implementation of tabbed navigation support.
- **FlowMVI Integration**: Added `quo-vadis-core-flow-mvi` module for MVI pattern support.
- **Nested Screens**: Added nested settings screens (Profile, Notifications, About) to the demo app.
- **Tests**: Added tests for new functionality.

### Changed
- **License**: Changed project license from LGPL-2.1 to MIT License.

### Documentation
- Added comprehensive documentation for Tabbed Navigation and Flow MVI integration.
- Updated project documentation and webpage.

# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
- **Hierarchical Rendering**: True parent-child composition with coordinated animations via `QuoVadisHost`.
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

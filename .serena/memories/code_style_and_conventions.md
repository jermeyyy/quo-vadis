# Code Style and Conventions

## General Style
- **Code Style**: Official Kotlin code style (set in `gradle.properties`)
- **File Encoding**: UTF-8

## Naming Conventions

### Packages
- **Reverse domain notation**: `com.jermey.<module>.<feature>`
- **Demo app**: `com.jermey.navplayground`
- **Core library**: `com.jermey.quo.vadis.core.navigation`
- **Subpackages**: organized by functionality (core, compose, mvi, integration, testing, utils, serialization)

### Classes and Interfaces
- **PascalCase** for class/interface names
- **Descriptive names**: `Navigator`, `BackStack`, `Destination`, `NavigationGraph`
- **Interface prefix**: No "I" prefix (use plain names like `Navigator`)
- **Implementation suffix**: `Default` prefix for default implementations (e.g., `DefaultNavigator`)
- **Fake prefix**: For test implementations (e.g., `FakeNavigator`)

### Destinations
- **Sealed classes/objects**: Used for grouping related destinations
- **Suffix pattern**: `Destination` suffix (e.g., `HomeDestination`, `DetailDestination`)
- **Data classes**: For destinations with parameters

### Functions
- **camelCase** for function names
- **Descriptive verbs**: `navigate()`, `navigateBack()`, `registerGraph()`, `handleDeepLink()`

### Properties
- **camelCase** for properties
- **Private backing fields**: Use underscore prefix for mutable backing fields (e.g., `_backStack`)

### Constants
- **PascalCase** for object declarations used as constants
- **ALL_CAPS** not commonly used in this codebase

## Documentation

### KDoc Comments
- **All public APIs** should have KDoc documentation
- **Format**:
  ```kotlin
  /**
   * Brief description of the function/class.
   * 
   * More detailed explanation if needed.
   * 
   * @param paramName description
   * @return description
   */
  ```
- **Architecture docs**: Comprehensive markdown files in `docs/` directory

### Code Comments
- Minimal inline comments (code should be self-documenting)
- Comments used for clarifying complex logic or architectural decisions
- Section headers for grouping related methods in large classes

## Kotlin Language Features

### Type Safety
- **Explicit types** when it improves readability
- **Type inference** used when obvious from context
- **Null safety**: Full use of Kotlin's null-safety features
- **No nullable types** in public APIs unless necessary

### Sealed Classes
- Used extensively for destination definitions
- Used for intent patterns in MVI

### Data Classes
- Preferred for simple data holders (destinations with arguments)
- Override `arguments` property to expose data

### Extension Functions
- Used extensively in utils package
- Organized in dedicated files (e.g., `NavigationExtensions.kt`)

### DSL Builders
- Used for navigation graph construction
- Lambda with receiver pattern for configuration

### Coroutines & Flow
- **StateFlow** for reactive state management
- **SharedFlow** for one-time events (effects)
- **CoroutineScope** for scoped operations
- **Dispatchers.Default** for background work

## Compose Conventions

### Composable Functions
- **PascalCase** naming (like React components)
- **@Composable** annotation required
- **Modifier parameter**: Always include as last parameter with default value
- **State hoisting**: State managed at appropriate level
- **remember**: Used for state that should survive recomposition
- **LaunchedEffect**: For side effects tied to composition lifecycle

### Composable Organization
- Screen-level composables in separate files
- Reusable components in shared files
- UI logic separated from business logic

## Architecture Patterns

### Interfaces First
- Define interfaces for core components
- Provide default implementations
- Enables testing with fakes

### Immutability
- Prefer `val` over `var`
- Immutable data structures when possible
- Mutable backing fields kept private

### Reactive Programming
- StateFlow for observable state
- Avoid callbacks in favor of flows
- Collect state in composables with `collectAsState()`

### Separation of Concerns
- Core logic independent of UI framework
- Compose layer separate from core
- Platform-specific code in respective source sets

## File Organization

### File Names
- Match primary class name
- One primary class per file (except related sealed class hierarchies)

### File Structure
1. Package declaration
2. Imports (organized automatically by IDE)
3. File-level documentation (if applicable)
4. Class/interface declarations
5. Companion objects (if any)

## Best Practices Observed

1. **No reflection**: All navigation is compile-time safe
2. **Thread safety**: Using thread-safe primitives (StateFlow, etc.)
3. **Testability**: FakeNavigator for unit testing
4. **Modularity**: Feature modules can define their own graphs
5. **Documentation**: Comprehensive docs in dedicated files
6. **Minimal dependencies**: Library has no external nav dependencies
7. **Progressive disclosure**: Simple cases are simple, complex cases possible
8. **Compose-first**: Designed for Compose UI paradigm

## Android-Specific

- **AndroidX**: All Android dependencies use AndroidX
- **Non-transitive R class**: Enabled for better build performance
- **Resource excludes**: META-INF files excluded from packaging

## iOS-Specific

- **Static frameworks**: iOS binaries are static frameworks
- **Framework naming**: Descriptive names (ComposeApp, quo-vadis-coreKit)
- **SwiftUI integration**: UIViewControllerRepresentable for Compose integration

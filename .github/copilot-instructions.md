# GitHub Copilot Instructions for NavPlayground

## Project Overview
This is **NavPlayground** - a Kotlin Multiplatform navigation library demonstration project featuring **"Quo Vadis"** (Latin for "Where are you going?"), a comprehensive navigation library for Compose Multiplatform.

**CRITICAL**: This project consists of TWO main components:
1. **`quo-vadis-core`** - The ACTUAL navigation library (independent, reusable)
2. **`composeApp`** - Demo application showcasing ALL navigation patterns

## ESSENTIAL WORKFLOW RULES

### ALWAYS Before Making Changes:
1. **READ RELEVANT DOCUMENTATION** - Check `quo-vadis-core/docs/` for architecture details:
   - `ARCHITECTURE.md` - Design principles and patterns
   - `API_REFERENCE.md` - Complete API documentation
   - `NAVIGATION_IMPLEMENTATION.md` - Implementation details
   - `MULTIPLATFORM_PREDICTIVE_BACK.md` - Platform-specific features

2. **CHECK PROJECT MEMORIES** - Use `read_memory` tool to access:
   - `project_overview` - Project structure and purpose
   - `codebase_structure` - File organization and modules
   - `tech_stack` - Dependencies and versions
   - `architecture_patterns` - Design patterns and best practices
   - `code_style_and_conventions` - Naming and style rules
   - `suggested_commands` - Common build/test commands
   - `task_completion_checklist` - Quality assurance steps

3. **USE SYMBOL DISCOVERY** - Leverage `jet_brains_find_symbol` and `jet_brains_get_symbols_overview` BEFORE reading entire files

4. **VERIFY CHANGES** - ALWAYS run error checking after edits using `get_errors` or `mcp_intellij_get_file_problems`

### NEVER:
- Make string-based navigation (use type-safe destinations)
- Add external navigation library dependencies to `quo-vadis-core`
- Put platform-specific code in `commonMain`
- Commit without running tests
- Skip documentation updates for public API changes

## Technology Stack

**MUST USE EXACT VERSIONS** from `gradle/libs.versions.toml`:
- Kotlin: 2.2.20 (Multiplatform)
- Compose Multiplatform: 1.9.0
- Android Min SDK: 24, Target/Compile: 36
- Gradle: 8.11+, AGP: 8.11.2

**Key Libraries**:
- Compose (Runtime, Foundation, Material3, UI)
- AndroidX (Activity, Lifecycle, ViewModel)
- StateFlow/SharedFlow for reactive state
- NO external navigation libraries in core module

## Architecture & Design Patterns

**CORE PRINCIPLES** (See `quo-vadis-core/docs/ARCHITECTURE.md`):
1. **Type Safety First** - Compile-time safe navigation, NO string routes in app code
2. **Modularization** - Gray box pattern for feature modules
3. **Reactive State** - StateFlow for observable state, SharedFlow for events
4. **MVI Support** - First-class MVI architecture integration
5. **Testability** - Use `FakeNavigator` for unit testing
6. **Platform Agnostic Core** - Platform-specific code ONLY in platform source sets

**Key Components**:
- `Destination` - Navigation target (sealed classes preferred)
- `Navigator` - Central controller with reactive state
- `BackStack` - Direct stack manipulation
- `NavigationGraph` - Modular graph definitions (DSL builder pattern)
- `NavHost` - Compose rendering with transition support

**State Management Flow**:
```
User Action → Intent → Navigator → BackStack Update → State Change → UI Recomposition
```

## Code Style & Conventions

**Naming** (STRICTLY ENFORCE):
- Classes/Interfaces: `PascalCase` (e.g., `Navigator`, `BackStack`)
- Functions/Properties: `camelCase` (e.g., `navigate()`, `navigateBack()`)
- Destinations: `PascalCase` + `Destination` suffix (e.g., `HomeDestination`)
- Test Fakes: `Fake` prefix (e.g., `FakeNavigator`)
- Default Implementations: `Default` prefix (e.g., `DefaultNavigator`)

**Package Structure**:
- Demo: `com.jermey.navplayground.demo.<feature>`
- Library: `com.jermey.quo.vadis.core.navigation.<package>`
  - Subpackages: `core`, `compose`, `mvi`, `integration`, `testing`, `utils`, `serialization`

**Documentation**:
- ALL public APIs MUST have KDoc documentation
- Format: `/** Brief. Details. @param @return */`
- Update markdown docs when architecture/API changes

**Kotlin Features**:
- Sealed classes for destination hierarchies
- Data classes for destinations with arguments
- Extension functions in `utils` package
- DSL builders with lambda receivers
- StateFlow/SharedFlow (NOT callbacks)
- Explicit null safety (avoid nullables in public APIs)

**Compose Conventions**:
- Composables use `PascalCase`
- `Modifier` parameter last with default value
- State hoisting at appropriate level
- `remember` for recomposition-stable state
- `LaunchedEffect` for side effects

## Multiplatform Organization

**Source Sets** (CRITICAL):
- `commonMain` - ALL core logic, Compose UI, platform-agnostic code
- `androidMain` - Android-only (system back, deep links, Activity integration)
- `iosMain` - iOS-only (navigation bar, universal links)
- NEVER use platform APIs in `commonMain`
- Use expect/actual for platform differences

**Build Targets**:
- Android: Min SDK 24, Target 36
- iOS: iosArm64, iosSimulatorArm64, iosX64

## Essential Commands

**Building** (Use these via `execute_shell_command` or `mcp_intellij_execute_terminal_command`):
```bash
# Clean build everything
./gradlew clean build

# Android debug APK
./gradlew :composeApp:assembleDebug

# iOS framework (M1/M2 simulator)
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64

# Library module only
./gradlew :quo-vadis-core:build
```

**Testing**:
```bash
# All tests
./gradlew test

# Android unit tests
./gradlew :quo-vadis-core:testDebugUnitTest

# Android instrumented tests
./gradlew :quo-vadis-core:connectedAndroidTest
```

**Verification**:
```bash
# Lint check
./gradlew lint

# Dependencies
./gradlew :composeApp:dependencies
```

**Cleaning** (when build cache issues):
```bash
./gradlew --stop
rm -rf .gradle/configuration-cache
./gradlew clean
```

## Task Completion Checklist

**BEFORE COMMITTING** (Reference: `task_completion_checklist` memory):
1. ✅ Code follows Kotlin official style
2. ✅ ALL public APIs have KDoc
3. ✅ Run: `./gradlew clean build` (MUST pass)
4. ✅ Run: `./gradlew test` (MUST pass)
5. ✅ Check compilation: Use `get_errors` or `mcp_intellij_get_file_problems`
6. ✅ Update documentation if API/architecture changed
7. ✅ Verify multiplatform compatibility (no platform code in commonMain)
8. ✅ Test on BOTH Android and iOS if UI changes
9. ✅ Use `FakeNavigator` for navigation unit tests
10. ✅ No debug prints, commented code, or unresolved TODOs

**Quality Gates**:
- No compilation errors or warnings (address relevant ones)
- All tests pass
- Proper null safety
- Thread-safe implementations (use StateFlow, not mutable state)
- No memory leaks in navigation state

## Common Patterns & Examples

**Destination Definition** (Type-Safe):
```kotlin
sealed class FeatureDestination : Destination {
    object Home : FeatureDestination() {
        override val route = "home"
    }
    
    data class Details(val id: String) : FeatureDestination() {
        override val route = "details"
        override val arguments = mapOf("id" to id)
    }
}
```

**Navigation Graph** (DSL):
```kotlin
navigationGraph("feature") {
    startDestination(Screen1)
    destination(Screen1) { _, nav -> Screen1UI(nav) }
    destination(Screen2) { _, nav -> Screen2UI(nav) }
}
```

**Navigation Operations**:
```kotlin
// Simple navigation
navigator.navigate(DetailsDestination("123"))

// With transition
navigator.navigate(Details, NavigationTransitions.SlideHorizontal)

// Clear stack
navigator.navigateAndClearTo(Home, "login", inclusive = true)

// Back navigation
navigator.navigateBack()
```

**Testing with FakeNavigator**:
```kotlin
val fakeNavigator = FakeNavigator()
viewModel.navigate(Details) // Uses fake
assert(fakeNavigator.lastDestination == Details)
```

## File Organization

**Demo App Structure** (`composeApp/src/commonMain/kotlin/`):
```
com/jermey/navplayground/
├── App.kt                    # Main entry point
└── demo/
    ├── DemoApp.kt           # Main demo with drawer & bottom nav
    ├── destinations/        # All destination definitions
    ├── graphs/              # Navigation graph definitions
    └── ui/screens/          # Demo screens for all patterns
```

**Library Structure** (`quo-vadis-core/src/commonMain/kotlin/`):
```
com/jermey/quo/vadis/core/navigation/
├── core/            # Core components (Navigator, BackStack, Destination)
├── compose/         # Compose integration (NavHost, rendering)
├── mvi/             # MVI pattern support
├── integration/     # DI framework integration (Koin, etc.)
├── serialization/   # State save/restore
├── testing/         # Test utilities (FakeNavigator)
└── utils/           # Extension functions
```

## Tool Usage Guidelines

**Symbol Discovery** (PREFER THESE):
- `jet_brains_find_symbol` - Find classes/functions by name path
- `jet_brains_get_symbols_overview` - Get file structure overview
- `jet_brains_find_referencing_symbols` - Find usages

**File Operations**:
- `mcp_intellij_get_file_text_by_path` - Read file contents
- `mcp_intellij_replace_text_in_file` - Targeted replacements (PREFERRED)
- `replace_symbol_body` - Replace function/class bodies
- `insert_after_symbol` / `insert_before_symbol` - Add new code

**Verification**:
- `mcp_intellij_get_file_problems` - Check errors/warnings
- `mcp_intellij_execute_terminal_command` - Run builds/tests

**Search**:
- `mcp_intellij_search_in_files_by_text` - Fast text search
- `mcp_intellij_search_in_files_by_regex` - Pattern matching
- `mcp_intellij_find_files_by_name_keyword` - Find files by name

## Best Practices (ENFORCE STRICTLY)

**DO**:
✅ Use sealed classes for destination hierarchies
✅ Keep destinations simple (data only, no logic)
✅ One navigation graph per feature module
✅ Test navigation with `FakeNavigator`
✅ Observe state reactively with `collectAsState()`
✅ Use type-safe navigation (compile-time safety)
✅ Document ALL public APIs with KDoc
✅ Handle deep links early in app lifecycle
✅ Keep platform-specific code minimal and isolated
✅ Verify changes with error checking tools

**DON'T**:
❌ String-based navigation in application code
❌ Circular navigation dependencies
❌ Storing UI state in Navigator
❌ Blocking operations in navigation callbacks
❌ Exposing mutable state from Navigator
❌ Creating new StateFlows in Composables
❌ Using global singletons for navigation state
❌ Mixing navigation and business logic
❌ Adding external navigation dependencies to `quo-vadis-core`
❌ Reflection (keep everything compile-time safe)

## Problem-Solving Approach

1. **Understand Context**: Read relevant docs/memories FIRST
2. **Use Symbol Tools**: Navigate code via symbols, not grep
3. **Make Minimal Changes**: Precise edits using `replace_text_in_file`
4. **Verify Immediately**: Check errors after EVERY file edit
5. **Test Thoroughly**: Run unit tests and build
6. **Document Changes**: Update KDoc and markdown files
7. **Reference, Don't Duplicate**: Point to existing docs instead of repeating

## Key Documentation References

**ALWAYS check these before making changes**:
- Architecture: `quo-vadis-core/docs/ARCHITECTURE.md`
- API Reference: `quo-vadis-core/docs/API_REFERENCE.md`
- Navigation Details: `quo-vadis-core/docs/NAVIGATION_IMPLEMENTATION.md`
- Predictive Back: `quo-vadis-core/docs/MULTIPLATFORM_PREDICTIVE_BACK.md`
- Demo Patterns: `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/README.md`

## Memory Files

**Access via `read_memory` tool**:
- `project_overview` - High-level project structure
- `codebase_structure` - Detailed file organization
- `tech_stack` - Dependencies and versions
- `architecture_patterns` - Design patterns explained
- `code_style_and_conventions` - Naming and formatting rules
- `suggested_commands` - Build/test/clean commands
- `task_completion_checklist` - Pre-commit verification steps

## FINAL REMINDERS

🔴 **CRITICAL RULES**:
1. READ DOCUMENTATION BEFORE CODING
2. USE SYMBOL DISCOVERY TOOLS (not blind file reading)
3. VERIFY ALL CHANGES with error checking
4. RUN TESTS before marking complete
5. UPDATE DOCUMENTATION for API changes
6. MAINTAIN TYPE SAFETY (no string routing)
7. KEEP `commonMain` PLATFORM-AGNOSTIC
8. ALL PUBLIC APIs NEED KDoc
9. USE `FakeNavigator` for testing navigation
10. REFERENCE existing docs instead of duplicating content

**When in doubt**: Check memories, read docs, use symbol tools, verify with tests or ask user.


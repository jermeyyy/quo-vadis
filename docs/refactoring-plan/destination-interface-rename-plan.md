# Destination Interface Rename Plan

**Status**: Ready for Implementation  
**Date**: 2025-12-17  
**Branch**: `architecture-refactor`

## Overview

Rename the `Destination` interface to `NavDestination` to resolve a naming conflict with the `@Destination` annotation.

### Problem

Both types have the same name:
- **Interface**: `Destination` in `quo-vadis-core` - marker interface for navigation destinations
- **Annotation**: `@Destination` in `quo-vadis-annotations` - KSP annotation for code generation

This forces users to use fully qualified names:
```kotlin
@Destination(route = "home")  // annotation
data object Home : com.jermey.quo.vadis.core.navigation.core.Destination  // interface FQN
```

### Solution

Rename the interface to `NavDestination`:
```kotlin
@Destination(route = "home")  // annotation (unchanged)
data object Home : NavDestination  // interface (renamed)
```

### Rationale

- **Industry standard**: Matches Jetpack Navigation's naming pattern
- **Annotation consistency**: Keeps `@Destination` alongside `@Screen`, `@Stack`, `@Tabs`
- **Better DX**: Simple imports, no FQN needed

---

## Implementation Tasks

### Phase 1: Core Library (`quo-vadis-core`)

#### Task 1.1: Rename Interface Definition

**File**: `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/Destination.kt`

**Changes**:
1. Rename file to `NavDestination.kt`
2. Rename `interface Destination` → `interface NavDestination`
3. Rename extension `val Destination.route` → `val NavDestination.route`
4. Update KDoc to remove `TypedDestination` reference

```kotlin
// BEFORE
interface Destination {
    val data: Any? get() = null
    val transition: NavigationTransition? get() = null
}

val Destination.route: String
    get() = RouteRegistry.getRoute(this::class)

// AFTER
interface NavDestination {
    val data: Any? get() = null
    val transition: NavigationTransition? get() = null
}

val NavDestination.route: String
    get() = RouteRegistry.getRoute(this::class)
```

#### Task 1.2: Update Core Source Files

Update all imports and type references in `quo-vadis-core/src/commonMain`:

| File | Line Numbers | Changes |
|------|--------------|---------|
| `navigation/core/NavNodeState.kt` | Multiple | `currentDestination: StateFlow<NavDestination?>`, `previousDestination: StateFlow<NavDestination?>` |
| `navigation/core/NavNode.kt` | L115, L122, L124, L134, L974 | Update `Destination` → `NavDestination` |
| `navigation/core/NavTreeUtils.kt` | L56, L103 | `currentDestination(root: NavNode): NavDestination?` |
| `navigation/core/DeepLinkResolver.kt` | L56, L103 | `resolve(deepLink): NavDestination?` |
| `navigation/core/RouteRegistry.kt` | Multiple | `destinationClass: KClass<out NavDestination>`, Map types |
| `navigation/core/ScreenRegistry.kt` | L159, L197 | `destinationClass: KClass<out NavDestination>` |
| `navigation/core/TransitionRegistry.kt` | L111, L265 | Update type references |
| `navigation/dsl/NavGraphBuilder.kt` | L66, L72, L78, L84, L443, L450, L465 | Map types |
| `navigation/dsl/NavGraphDsl.kt` | L26, L46, L62, L78 | Properties |
| `navigation/dsl/NavHostConfig.kt` | L111, L112 | Data class properties |
| `navigation/dsl/DSLTypes.kt` | L157, L231, L247, L261 | Multiple usages |
| `navigation/dsl/DSLBuilder.kt` | L24 | `rootDestination: NavDestination?` |
| `navigation/dsl/TabDslTypes.kt` | L24, L42 | Data class properties |
| `navigation/dsl/ScreenEntryBuilder.kt` | L47, L56, L87, L88 | `Map<KClass<out NavDestination>, ScreenEntry>` |
| `navigation/dsl/DSLScreenRenderer.kt` | Multiple (L71-L377) | Type parameters and map types |
| `navigation/dsl/TransitionConfig.kt` | Multiple | `Map<KClass<out NavDestination>, NavTransition>` |
| `navigation/compose/NavHost.kt` | L83, L241 | `rootDestination: KClass<out NavDestination>` |
| `navigation/compose/NavigationHost.kt` | L229-240 | Property access |
| `navigation/compose/NavHostComposable.kt` | Import | Update import |
| `navigation/compose/AnimatedNavContent.kt` | Import | Update import |
| `navigation/compose/PredictiveBackHandler.kt` | Import | Update import |
| `testing/FakeNavigator.kt` | L53-57 | StateFlow types |
| `platform/PlatformUtils.kt` | Import | Update import |

#### Task 1.3: Update Test Files

Update all test files in `quo-vadis-core/src/commonTest`:

| File | Changes |
|------|---------|
| `NavNodeTest.kt` | L7, L35, L225 - imports and test destinations |
| `NavNodeStateTest.kt` | L3 - import |
| `NavGraphDslTest.kt` | L4 - import |
| `NavHostConfigTest.kt` | L5 - import |
| `DeepLinkResolverTest.kt` | L4 - import |
| `NavigatorIntegrationTest.kt` | L4 - import |
| `RouteRegistryTest.kt` | L5 - import |
| `ScreenRegistryTest.kt` | L6 - import |
| `TransitionRegistryTest.kt` | L4 - import |
| `BackHandlerRegistryTest.kt` | L5 - import |
| `ScopeRegistryTest.kt` | L5 - import |
| `FakeNavigatorTest.kt` | L28, L52, L69, L608 - type usages |
| `destinations/AppDestination.kt` | L29, L35, L41, L47 - sealed class extends |
| `destinations/AuthDestination.kt` | L22, L28, L34 - sealed class extends |
| `destinations/OnboardingDestination.kt` | L27, L33, L39 - sealed class extends |
| `destinations/SettingsDestination.kt` | L35, L41, L47, L53 - sealed class extends |

---

### Phase 2: KSP Processor (`quo-vadis-ksp`)

#### Task 2.1: Update Type References

| File | Changes |
|------|---------|
| `CodegenConstants.kt` | Update `DESTINATION: ClassName` and imports |
| `StringTemplates.kt` | Update template strings: `KClass<out NavDestination>` |
| `NavigationConfigGenerator.kt` | Update `DESTINATION_CLASS` constant |
| `ScreenWrapperGenerator.kt` | Update ClassName references |
| `CodeGenerationExtensions.kt` | Update addImport statements |
| `DestinationScanner.kt` | Update `const val DESTINATION = "Destination"` → keep as annotation name check |

**Important**: Only update references to the **interface**. The annotation name remains `@Destination`.

```kotlin
// KSP CONSTANT - BEFORE
val DESTINATION: ClassName = ClassName("com.jermey.quo.vadis.core.navigation.core", "Destination")

// KSP CONSTANT - AFTER  
val NAV_DESTINATION: ClassName = ClassName("com.jermey.quo.vadis.core.navigation.core", "NavDestination")
```

---

### Phase 3: Demo App (`composeApp`)

#### Task 3.1: Update Destination Implementations

| File | Changes |
|------|---------|
| `destinations/MasterDetailDestination.kt` | L5 import, L23 `sealed class MasterDetailDestination : NavDestination` |
| `destinations/ProcessDestination.kt` | L5 import, L11 `sealed class ProcessDestination : NavDestination` |
| `destinations/DeepLinkDestination.kt` | L4 import, L13 `sealed class DeepLinkDestination : NavDestination` |
| `destinations/AuthFlowDestination.kt` | L4 import, L18 `sealed class AuthFlowDestination : NavDestination` |
| `destinations/StateDrivenDestinations.kt` | L3 import, L12 extends |
| `destinations/StateDrivenDemoDestination.kt` | FQN → import + simple name |
| `destinations/TabsDestination.kt` | L28, L34, L67, L69, L73, L76, L79 - FQN → import |

**Example transformation**:
```kotlin
// BEFORE
sealed class DemoTabs : com.jermey.quo.vadis.core.navigation.core.Destination

// AFTER
import com.jermey.quo.vadis.core.navigation.core.NavDestination

sealed class DemoTabs : NavDestination
```

#### Task 3.2: Update UI Components

| File | Changes |
|------|---------|
| `components/NavigationBottomSheetContent.kt` | L3 import, `onNavigate: (NavDestination) -> Unit` |
| `ui/DemoList.kt` | L3 import, L18 `val destination: NavDestination` |

---

### Phase 4: Documentation

#### Task 4.1: Update Code Documentation

| File | Changes |
|------|---------|
| `Destination.kt` → `NavDestination.kt` | Remove `TypedDestination` reference from KDoc |
| `architecture_patterns.md` memory | Update destination pattern examples |

#### Task 4.2: Update README/Docs (if applicable)

Search for and update any references to `Destination` interface in:
- `README.md`
- `docs/` folder
- KDoc comments

---

## Execution Order

```
1. quo-vadis-core/src/commonMain/.../core/Destination.kt → NavDestination.kt (rename file + interface)
   ↓
2. quo-vadis-core/src/commonMain/**/*.kt (update all imports/usages)
   ↓
3. quo-vadis-core/src/commonTest/**/*.kt (update test files)
   ↓
4. quo-vadis-ksp/src/main/**/*.kt (update KSP processor)
   ↓
5. composeApp/src/commonMain/**/*.kt (update demo app)
   ↓
6. Build & test to verify
```

---

## Verification Checklist

- [ ] `./gradlew build` passes
- [ ] All tests pass
- [ ] No remaining `import ...core.Destination` (should be `NavDestination`)
- [ ] No remaining `: Destination` interfaces (should be `: NavDestination`)  
- [ ] KSP generates correct code with `NavDestination` type
- [ ] Demo app compiles and runs
- [ ] No FQN usage for the interface (clean imports)

---

## Files Summary

| Module | Files to Update |
|--------|-----------------|
| `quo-vadis-core/src/commonMain` | ~25 files |
| `quo-vadis-core/src/commonTest` | ~15 files |
| `quo-vadis-ksp/src/main` | ~6 files |
| `composeApp/src/commonMain` | ~10 files |
| **Total** | **~56 files** |

---

## Delegation

This task can be executed by the **Simple-Developer** agent with the prompt:

```
[TASK]: Rename Destination interface to NavDestination
Spec: `docs/refactoring-plan/destination-interface-rename-plan.md`
Files: All files listed in the plan
Context: Resolve naming conflict with @Destination annotation
Success: Build passes, tests pass, no FQN usage
Return: Summary of changes, any issues encountered
```

# Deprecation Cleanup Refactoring Plan

## Overview

This document outlines the refactoring plan for cleaning up deprecated APIs and simplifying the navigation library. The changes focus on three main areas:

1. **Deep Link Handling API Simplification** - Remove `BasicDestination` usage
2. **TabItemInfo Cleanup** - Remove deprecated `rootGraphClass` and `destination` fields
3. **Navigator API Cleanup** - Remove deprecated `switchTab()` and `setStartDestination()` methods

---

## 1. Deep Link Handling API Simplification

### Current State

The deep link handling system uses a deprecated `BasicDestination` class as a placeholder:

```kotlin
// In DeepLink.kt
@Deprecated(
    message = "BasicDestination is replaced by sealed class members.",
    level = DeprecationLevel.WARNING
)
internal data class BasicDestination(
    internal val routeString: String,
    override val transition: NavigationTransition? = null
) : Destination
```

**Problem**: The `DefaultDeepLinkHandler.register()` method creates `BasicDestination` instances as placeholders:

```kotlin
override fun register(
    pattern: String,
    action: (destination: Destination, navigator: Navigator, parameters: Map<String, String>) -> Unit
) {
    val destinationFactory: (Map<String, String>) -> Destination = { _ ->
        // This is a placeholder destination for resolution purposes
        BasicDestination(pattern)
    }
    registrations.add(DeepLinkRegistration(pattern, destinationFactory, action))
}
```

### Proposed Changes

#### Option A: Remove destination parameter from action callback (Recommended)

**Rationale**: The `destination` parameter in the action callback is always a `BasicDestination` placeholder - it's never the actual destination the user wants to navigate to. Users must construct their own destination anyway.

**New API Design**:

```kotlin
interface DeepLinkHandler {
    /**
     * Register a deep link pattern with an action to execute when matched.
     * 
     * @param pattern The URI pattern to match (e.g., "app://demo/item/{id}")
     * @param action The action to execute when pattern matches.
     *               Receives navigator and extracted parameters.
     */
    fun register(
        pattern: String, 
        action: (navigator: Navigator, parameters: Map<String, String>) -> Unit
    )
    
    /**
     * Handle a deep link by executing the registered action.
     */
    fun handle(deepLink: DeepLink, navigator: Navigator): Boolean
}
```

**Migration Path**:
1. Add new `register` overload without destination parameter
2. Deprecate old `register` method with destination parameter
3. Update `DefaultDeepLinkHandler` implementation
4. Remove `BasicDestination` class entirely

#### Option B: Require destination factory at registration

Alternative approach where users provide the destination factory upfront:

```kotlin
fun register(
    pattern: String,
    destinationFactory: (parameters: Map<String, String>) -> Destination,
    action: ((destination: Destination, navigator: Navigator, parameters: Map<String, String>) -> Unit)? = null
)
```

This keeps backward compatibility but adds complexity.

### Implementation Tasks

| Task | File(s) | Effort | Priority |
|------|---------|--------|----------|
| 1.1 Add new `register` overload | `DeepLink.kt` | S | High |
| 1.2 Deprecate old `register` method | `DeepLink.kt` | S | High |
| 1.3 Update `DefaultDeepLinkHandler` | `DeepLink.kt` | M | High |
| 1.4 Update `resolve()` to not use BasicDestination | `DeepLink.kt` | S | High |
| 1.5 Remove `BasicDestination` from `Destination.kt` | `Destination.kt` | S | Medium |
| 1.6 Update demo app deep link setup | `DeepLinkSetup.kt` | S | Medium |
| 1.7 Update KSP-generated deep link handler | `quo-vadis-ksp` | M | Medium |
| 1.8 Update tests | Tests | M | High |

### Files Affected

- `quo-vadis-core/src/commonMain/kotlin/.../core/DeepLink.kt`
- `quo-vadis-core/src/commonMain/kotlin/.../core/Destination.kt`
- `composeApp/src/commonMain/kotlin/.../demo/DeepLinkSetup.kt`
- `quo-vadis-ksp/src/main/kotlin/.../generators/DeepLinkGenerator.kt` (if exists)

---

## 2. TabItemInfo Deprecated Fields Cleanup

### Current State

`TabItemInfo` in the KSP module has deprecated fields:

```kotlin
data class TabItemInfo(
    val label: String,
    val icon: String,
    val classDeclaration: KSClassDeclaration,
    val tabType: TabItemType = TabItemType.FLAT_SCREEN,
    val destinationInfo: DestinationInfo? = null,
    val stackInfo: StackInfo? = null,
    // DEPRECATED:
    @Deprecated("Use tabType, destinationInfo, or stackInfo instead")
    val rootGraphClass: KSClassDeclaration? = null,
    @Deprecated("Use tabType and destinationInfo instead")
    val destination: DestinationInfo? = null,
)
```

### Analysis of Usage

The deprecated fields are used in:

1. **`NavNodeBuilderGenerator.kt`**:
   ```kotlin
   val rootGraph = tabItem.rootGraphClass ?: tabItem.classDeclaration
   ```
   - Used for NESTED_STACK tabs when the tab class itself IS the stack

2. **`TabExtractor.kt`**:
   ```kotlin
   val rootGraphClass = extractRootGraphClass(tabItemAnnotation)
   // ...
   rootGraphClass = rootGraphClass,
   destination = null // Not required for new pattern
   ```

3. **`ValidationEngine.kt`**:
   ```kotlin
   tabItem.rootGraphClass?.let { rootGraph ->
       validateRootGraphClass(rootGraph, usageSite)
   }
   ```

### Proposed Changes

#### Phase 1: Consolidate Logic

The current pattern uses `rootGraphClass ?: classDeclaration` - this should be refactored to use the modern pattern exclusively:

```kotlin
// BEFORE
val rootGraph = tabItem.rootGraphClass ?: tabItem.classDeclaration

// AFTER - Use stackInfo for NESTED_STACK tabs
val rootGraph = when (tabItem.tabType) {
    TabItemType.NESTED_STACK -> tabItem.stackInfo?.classDeclaration ?: tabItem.classDeclaration
    TabItemType.FLAT_SCREEN -> tabItem.classDeclaration
}
```

#### Phase 2: Remove Deprecated Fields

After all usages are migrated:

```kotlin
data class TabItemInfo(
    val label: String,
    val icon: String,
    val classDeclaration: KSClassDeclaration,
    val tabType: TabItemType = TabItemType.FLAT_SCREEN,
    val destinationInfo: DestinationInfo? = null,
    val stackInfo: StackInfo? = null,
    // rootGraphClass and destination REMOVED
)
```

### Implementation Tasks

| Task | File(s) | Effort | Priority |
|------|---------|--------|----------|
| 2.1 Audit all `rootGraphClass` usages | Multiple KSP files | S | High |
| 2.2 Update `NavNodeBuilderGenerator` to use `stackInfo` | `NavNodeBuilderGenerator.kt` | M | High |
| 2.3 Update `TabExtractor` - stop setting deprecated fields | `TabExtractor.kt` | S | High |
| 2.4 Update `ValidationEngine` to use new pattern | `ValidationEngine.kt` | S | High |
| 2.5 Remove deprecated fields from `TabItemInfo` | `TabInfo.kt` | S | Medium |
| 2.6 Verify KSP generated code compiles | Build | S | High |

### Files Affected

- `quo-vadis-ksp/src/main/kotlin/.../models/TabInfo.kt`
- `quo-vadis-ksp/src/main/kotlin/.../generators/NavNodeBuilderGenerator.kt`
- `quo-vadis-ksp/src/main/kotlin/.../generators/ScopeRegistryGenerator.kt`
- `quo-vadis-ksp/src/main/kotlin/.../extractors/TabExtractor.kt`
- `quo-vadis-ksp/src/main/kotlin/.../validation/ValidationEngine.kt`

### Note on PaneItemInfo

`PaneItemInfo` also has a `rootGraphClass` field that is **NOT deprecated** - this is still the standard pattern for panes:

```kotlin
data class PaneItemInfo(
    val destination: DestinationInfo,
    val role: PaneRole,
    val adaptStrategy: AdaptStrategy,
    val rootGraphClass: KSClassDeclaration  // NOT deprecated for panes
)
```

This should NOT be removed as panes work differently from tabs.

---

## 3. Navigator Deprecated Methods Cleanup

### Current State

#### `Navigator.switchTab(index: Int)`

```kotlin
@Deprecated(
    message = "switchTab() is deprecated. Use navigate() with a destination instead. " +
        "Navigate will automatically switch to the tab containing the destination.",
    replaceWith = ReplaceWith("navigate(destination)"),
    level = DeprecationLevel.WARNING
)
fun switchTab(index: Int)
```

#### `Navigator.setStartDestination(destination: Destination)`

```kotlin
@Deprecated(
    message = "setStartDestination() is no longer needed. Start destination is defined in @Stack/@Tab/@Pane annotations.",
    level = DeprecationLevel.WARNING
)
fun setStartDestination(destination: Destination)
```

### Analysis

#### `switchTab()` Usages

| Location | Type | Action Needed |
|----------|------|---------------|
| `Navigator.kt` (interface) | Definition | Remove |
| `TreeNavigator.kt` | Implementation | Remove |
| `FakeNavigator.kt` | Implementation | Remove |
| `TreeMutator.kt` | Internal method | **KEEP** - Different function, used by `PushStrategy.SwitchToTab` |
| `TabWrapperScope.kt` | User API | **KEEP** - Not deprecated, different purpose |
| `TabWrapperScopeImpl.kt` | Implementation | **KEEP** - Calls internal `onSwitchTab` |
| `NavTreeRenderer.kt` | Wiring | Update to use `onSwitchTab` callback |
| `NavigatorExtGenerator.kt` (KSP) | Generated extensions | Update or remove |
| Tests | Various | Update/remove |

**Important Distinction**:
- `Navigator.switchTab(index)` - **DEPRECATED** - direct index-based navigation bypass
- `TabWrapperScope.switchTab(index)` - **NOT DEPRECATED** - UI layer for custom tab bars
- `TreeMutator.switchTab(root, key, index)` - **INTERNAL** - tree mutation primitive

#### `setStartDestination()` Usages

| Location | Type | Action Needed |
|----------|------|---------------|
| `Navigator.kt` (interface) | Definition | Remove |
| `TreeNavigator.kt` | Implementation | Remove |
| `FakeNavigator.kt` | Implementation | Keep internally, remove from interface |
| Tests | Many usages | Update to use `initialState` constructor |

### Proposed Changes

#### Phase 1: Update Internal Wiring

1. **NavTreeRenderer** - Already uses callback pattern:
   ```kotlin
   onSwitchTab = { index -> scope.navigator.switchTab(index) }
   ```
   Change to internal mechanism that doesn't go through Navigator interface.

2. **KSP NavigatorExtGenerator** - Generated `switchTo{Tab}Tab()` extensions:
   ```kotlin
   fun Navigator.switchToHomeTab() = switchTab(0)
   ```
   These should be updated to use `navigate()` instead or removed entirely.

#### Phase 2: Remove from Interface

1. Remove `switchTab(index: Int)` from `Navigator` interface
2. Remove `setStartDestination(destination: Destination)` from `Navigator` interface
3. Update implementations to remove these methods
4. Update `FakeNavigator` to keep internal `setStartDestination` for test convenience but remove from interface override

#### Phase 3: Update Tests

Most test files use `navigator.setStartDestination(HomeDestination)` pattern. Update to:

```kotlin
// BEFORE
val navigator = TreeNavigator()
navigator.setStartDestination(HomeDestination)

// AFTER
val initialState = StackNode(
    key = "root",
    parentKey = null,
    children = listOf(
        ScreenNode(key = "screen", parentKey = "root", destination = HomeDestination)
    )
)
val navigator = TreeNavigator(initialState = initialState)
```

Or create a test helper:

```kotlin
// In testing utilities
fun TreeNavigator.Companion.withStartDestination(destination: Destination): TreeNavigator {
    return TreeNavigator(initialState = StackNode.single(destination))
}
```

### Implementation Tasks

| Task | File(s) | Effort | Priority |
|------|---------|--------|----------|
| 3.1 Update `NavTreeRenderer` internal wiring | `NavTreeRenderer.kt` | S | High |
| 3.2 Update/remove KSP generated extensions | `NavigatorExtGenerator.kt` | M | Medium |
| 3.3 Create test helper for initial state | `FakeNavigator.kt` or new file | S | High |
| 3.4 Update all tests using `setStartDestination` | Test files | L | High |
| 3.5 Update all tests using `switchTab` | Test files | M | Medium |
| 3.6 Remove `switchTab` from `Navigator` interface | `Navigator.kt` | S | Medium |
| 3.7 Remove `setStartDestination` from `Navigator` interface | `Navigator.kt` | S | Medium |
| 3.8 Update `TreeNavigator` implementation | `TreeNavigator.kt` | S | Medium |
| 3.9 Update `FakeNavigator` | `FakeNavigator.kt` | S | Medium |

### Files Affected

- `quo-vadis-core/src/commonMain/kotlin/.../core/Navigator.kt`
- `quo-vadis-core/src/commonMain/kotlin/.../core/TreeNavigator.kt`
- `quo-vadis-core/src/commonMain/kotlin/.../testing/FakeNavigator.kt`
- `quo-vadis-core/src/commonMain/kotlin/.../compose/render/NavTreeRenderer.kt`
- `quo-vadis-ksp/src/main/kotlin/.../generators/NavigatorExtGenerator.kt`
- `quo-vadis-core/src/commonTest/kotlin/.../TreeNavigatorTest.kt`
- `quo-vadis-core/src/commonTest/kotlin/.../TreeMutatorTabTest.kt`
- `quo-vadis-core/src/commonTest/kotlin/.../TreeMutatorEdgeCasesTest.kt`

---

## Implementation Order

### Recommended Sequence

```
Phase 1: Preparation (Non-Breaking)
├── 2.1-2.4 TabItemInfo: Update usages to new pattern
├── 3.1 Update NavTreeRenderer internal wiring
├── 3.3 Create test helpers
└── 1.1-1.2 Add new DeepLink register API, deprecate old

Phase 2: Test Migration
├── 3.4-3.5 Update all tests
└── 1.8 Update deep link tests

Phase 3: Breaking Changes
├── 2.5 Remove deprecated TabItemInfo fields
├── 3.6-3.9 Remove Navigator deprecated methods
├── 1.3-1.5 Update DefaultDeepLinkHandler, remove BasicDestination
└── 3.2 Update/remove KSP generated extensions

Phase 4: Cleanup
├── 1.6-1.7 Update demo app and KSP generator
└── Verify full build and tests pass
```

### Estimated Total Effort

| Area | Effort |
|------|--------|
| Deep Link API | ~4-6 hours |
| TabItemInfo Cleanup | ~2-3 hours |
| Navigator Cleanup | ~6-8 hours |
| **Total** | **~12-17 hours** |

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Breaking existing user code | Medium | High | Provide clear migration guide |
| KSP generated code breaks | Medium | High | Test generated code thoroughly |
| Test migration takes longer than expected | High | Low | Parallelize test updates |
| Missing usage of deprecated APIs | Low | Medium | Full grep search before removal |

---

## Success Criteria

1. ✅ No `BasicDestination` usage anywhere in codebase
2. ✅ `TabItemInfo.rootGraphClass` and `TabItemInfo.destination` fields removed
3. ✅ `Navigator.switchTab()` and `Navigator.setStartDestination()` removed from interface
4. ✅ All tests pass
5. ✅ Demo app compiles and runs correctly
6. ✅ KSP generated code compiles without warnings

---

## Open Questions

1. **Q**: Should `TabWrapperScope.switchTab()` be renamed to avoid confusion with the removed `Navigator.switchTab()`?
   **A**: No - it serves a different purpose (UI layer vs navigation layer) and is not deprecated.

2. **Q**: Should we keep `Navigator.switchTab()` as internal for the renderer?
   **A**: No - use callback pattern via `TabWrapperScopeImpl.onSwitchTab` instead.

3. **Q**: What about generated `switchTo{Tab}Tab()` extension functions?
   **A**: These should be removed or updated to use `navigate()` with destination-based routing.

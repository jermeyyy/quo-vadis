# DeepLink Deprecated API Cleanup Plan

## Overview

This document outlines the cleanup plan for removing deprecated DeepLink handling APIs following the recent API refactoring. The new `DeepLinkRegistry` API is stable and in use, making it safe to remove the deprecated predecessor APIs.

**Cleanup Type**: Complete removal (breaking change)  
**Affected Module**: `quo-vadis-core`, `quo-vadis-ksp`  
**Created**: 2024-12-21

---

## Background

The deeplink handling system was refactored to introduce a cleaner, unified API:
- **Old API**: `DeepLinkHandler`, `GeneratedDeepLinkHandler`, `DefaultDeepLinkHandler`, `getDeepLinkHandler()`
- **New API**: `DeepLinkRegistry`, `RuntimeDeepLinkRegistry`, `CompositeDeepLinkRegistry`, `getDeepLinkRegistry()`

The new API was introduced in `deeplink-api-refactoring-plan.md` and is now the primary interface. All demo app usages have been migrated to the new API.

---

## Deprecated APIs to Remove

### 1. Interfaces

| Item | Location | Replacement |
|------|----------|-------------|
| `DeepLinkHandler` interface | [DeepLink.kt#L104-L135](../quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/DeepLink.kt) | `DeepLinkRegistry` |
| `GeneratedDeepLinkHandler` interface | [GeneratedDeepLinkHandler.kt](../quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/GeneratedDeepLinkHandler.kt) | `DeepLinkRegistry` |

### 2. Classes

| Item | Location | Replacement |
|------|----------|-------------|
| `DefaultDeepLinkHandler` class | [DeepLink.kt#L137-L234](../quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/DeepLink.kt) | `RuntimeDeepLinkRegistry` |
| `CompositeDeepLinkHandler` class | [CompositeDeepLinkHandler.kt](../quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/CompositeDeepLinkHandler.kt) | `CompositeDeepLinkRegistry` |

### 3. Methods

| Item | Location | Replacement |
|------|----------|-------------|
| `Navigator.getDeepLinkHandler()` | [Navigator.kt#L302-L309](../quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/Navigator.kt) | `getDeepLinkRegistry()` |
| `TreeNavigator.getDeepLinkHandler()` | [TreeNavigator.kt#L608-L614](../quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TreeNavigator.kt) | `getDeepLinkRegistry()` |
| `FakeNavigator.getDeepLinkHandler()` | [FakeNavigator.kt#L331-L340](../quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/testing/FakeNavigator.kt) | `getDeepLinkRegistry()` |

### 4. Properties

| Item | Location | Replacement |
|------|----------|-------------|
| `DeepLink.parameters` | [DeepLink.kt#L45-L50](../quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/DeepLink.kt) | `DeepLink.queryParams` |
| `NavigationConfig.deepLinkHandler` | [NavigationConfig.kt#L97](../quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/NavigationConfig.kt) | See Task 5.1 |
| `TreeNavigator.deepLinkHandler` | [TreeNavigator.kt#L73](../quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TreeNavigator.kt) | `deepLinkRegistry` |
| `FakeNavigator.fakeDeepLinkHandler` | [FakeNavigator.kt](../quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/testing/FakeNavigator.kt) | `fakeDeepLinkRegistry` |

---

## Current Usage Analysis

### Internal Usages (to be refactored)

| Location | Usage | Action |
|----------|-------|--------|
| `TreeNavigator.kt:73` | `deepLinkHandler` private property | Remove property, use `deepLinkRegistry` only |
| `TreeNavigator.kt:614` | `getDeepLinkHandler()` method | Remove method |
| `TreeNavigatorTest.kt:1020` | Test for `getDeepLinkHandler()` | Remove test |
| `FakeNavigator.kt:338` | `getDeepLinkHandler()` method | Remove method |
| `NavigationConfig.kt:97` | `deepLinkHandler` property | Rename to `deepLinkRegistry` or remove |
| `CompositeNavigationConfig.kt:91-96` | Uses `deepLinkHandler` | Update to use `DeepLinkRegistry` |
| `DslNavigationConfig.kt:111` | `deepLinkHandler = null` | Update property name |
| `EmptyNavigationConfig.kt:78` | `deepLinkHandler = null` | Update property name |

### KSP Generator (to be updated)

| File | Usage | Action |
|------|-------|--------|
| `DeepLinkHandlerGenerator.kt` | Generates `GeneratedDeepLinkHandler` implementation | Update to generate `DeepLinkRegistry` implementation |
| `NavigationConfigGenerator.kt:252` | Imports `GeneratedDeepLinkHandler` | Update import |
| `NavigationConfigGenerator.kt:307` | `buildDeepLinkHandlerProperty()` | Rename and update |
| `NavigationConfigGenerator.kt:1037` | `DEEP_LINK_HANDLER_CLASS` constant | Update to use `DeepLinkRegistry` |
| `QuoVadisClassNames.kt:10,34` | `GENERATED_DEEP_LINK_HANDLER` | Update to `DEEP_LINK_REGISTRY` |

### External Usages (none found)

No usages of deprecated APIs found in:
- `composeApp` module ✅
- `feature1` module ✅
- `feature2` module ✅

---

## Implementation Tasks

### Phase 1: Update NavigationConfig Property

#### Task 1.1: Rename `deepLinkHandler` to `deepLinkRegistry` in NavigationConfig

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/NavigationConfig.kt`

```kotlin
// Before
val deepLinkHandler: GeneratedDeepLinkHandler?

// After
val deepLinkRegistry: DeepLinkRegistry?
```

#### Task 1.2: Update DslNavigationConfig

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/DslNavigationConfig.kt`

```kotlin
// Before
override val deepLinkHandler: GeneratedDeepLinkHandler? = null

// After
override val deepLinkRegistry: DeepLinkRegistry? = null
```

#### Task 1.3: Update EmptyNavigationConfig

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/EmptyNavigationConfig.kt`

```kotlin
// Before
override val deepLinkHandler: GeneratedDeepLinkHandler? = null

// After
override val deepLinkRegistry: DeepLinkRegistry? = null
```

#### Task 1.4: Update CompositeNavigationConfig

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/CompositeNavigationConfig.kt`

Update to use `DeepLinkRegistry` instead of `GeneratedDeepLinkHandler`:

```kotlin
// Before
override val deepLinkHandler: GeneratedDeepLinkHandler? = when {
    secondary.deepLinkHandler != null && primary.deepLinkHandler != null -> {
        CompositeDeepLinkHandler(primary.deepLinkHandler!!, secondary.deepLinkHandler!!)
    }
    secondary.deepLinkHandler != null -> secondary.deepLinkHandler
    else -> primary.deepLinkHandler
}

// After
override val deepLinkRegistry: DeepLinkRegistry? = when {
    secondary.deepLinkRegistry != null && primary.deepLinkRegistry != null -> {
        CompositeDeepLinkRegistry(
            generated = CompositeDeepLinkRegistry(primary.deepLinkRegistry, RuntimeDeepLinkRegistry()),
            runtime = RuntimeDeepLinkRegistry()
        ).also { 
            // Or use a simpler approach - chain the registries
        }
    }
    secondary.deepLinkRegistry != null -> secondary.deepLinkRegistry
    else -> primary.deepLinkRegistry
}
```

**Note:** This may require rethinking how composite configs combine registries.

---

### Phase 2: Update TreeNavigator

#### Task 2.1: Remove `deepLinkHandler` Property

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TreeNavigator.kt`

Remove line 73:
```kotlin
// Remove this line
@Suppress("DEPRECATION")
private val deepLinkHandler: DeepLinkHandler = config.deepLinkHandler ?: DefaultDeepLinkHandler()
```

Update the `deepLinkRegistry` initialization (lines 76-79) to handle null case:
```kotlin
// Current (keep and simplify)
private val deepLinkRegistry: DeepLinkRegistry = CompositeDeepLinkRegistry(
    generated = config.deepLinkRegistry,
    runtime = RuntimeDeepLinkRegistry()
)
```

#### Task 2.2: Remove `getDeepLinkHandler()` Method

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TreeNavigator.kt`

Remove lines 603-614:
```kotlin
// Remove entire method
@Suppress("DEPRECATION")
@Deprecated(
    message = "Use getDeepLinkRegistry() instead for the new unified API",
    replaceWith = ReplaceWith("getDeepLinkRegistry()"),
    level = DeprecationLevel.WARNING
)
override fun getDeepLinkHandler(): DeepLinkHandler = deepLinkHandler
```

---

### Phase 3: Update Navigator Interface

#### Task 3.1: Remove `getDeepLinkHandler()` from Navigator Interface

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/Navigator.kt`

Remove lines 298-309:
```kotlin
// Remove entire method declaration
@Suppress("DEPRECATION")
@Deprecated(
    message = "Use getDeepLinkRegistry() instead for the new unified API",
    replaceWith = ReplaceWith("getDeepLinkRegistry()"),
    level = DeprecationLevel.WARNING
)
fun getDeepLinkHandler(): DeepLinkHandler
```

---

### Phase 4: Update FakeNavigator

#### Task 4.1: Remove `getDeepLinkHandler()` and `fakeDeepLinkHandler`

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/testing/FakeNavigator.kt`

Remove:
- `fakeDeepLinkHandler` property declaration
- `getDeepLinkHandler()` method (lines 331-340)

---

### Phase 5: Remove Deprecated Types

#### Task 5.1: Remove `DeepLinkHandler` Interface

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/DeepLink.kt`

Remove lines 100-135 (the entire `DeepLinkHandler` interface).

#### Task 5.2: Remove `DefaultDeepLinkHandler` Class

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/DeepLink.kt`

Remove lines 137-234 (the entire `DefaultDeepLinkHandler` class).

#### Task 5.3: Remove `DeepLink.parameters` Property

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/DeepLink.kt`

Remove lines 43-51:
```kotlin
// Remove
@Deprecated(
    message = "Use queryParams or allParams instead",
    replaceWith = ReplaceWith("queryParams"),
    level = DeprecationLevel.WARNING
)
val parameters: Map<String, String>
    get() = queryParams
```

#### Task 5.4: Delete `GeneratedDeepLinkHandler.kt`

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/GeneratedDeepLinkHandler.kt`

Delete the entire file. The `DeepLinkResult` class can be kept if still needed or moved.

**Decision needed:** Keep `DeepLinkResult` in a separate file or inline in generated code?

#### Task 5.5: Delete `CompositeDeepLinkHandler.kt`

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/CompositeDeepLinkHandler.kt`

Delete the entire file. `CompositeDeepLinkRegistry` is the replacement.

---

### Phase 6: Update KSP Generator

#### Task 6.1: Update DeepLinkHandlerGenerator to Implement DeepLinkRegistry

**File:** `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/DeepLinkHandlerGenerator.kt`

Major changes:
1. Rename generated class from `*DeepLinkHandler` to `*DeepLinkHandler` (keep name for now, or rename to `*GeneratedDeepLinkRegistry`)
2. Change `GeneratedDeepLinkHandler` base interface to `DeepLinkRegistry`
3. Remove legacy method generation (`buildLegacyRegisterFunction`, `buildLegacyHandleFunction`)
4. Update class documentation

```kotlin
// Current (line 79)
private val GENERATED_DEEP_LINK_HANDLER = ClassName(
    "com.jermey.quo.vadis.core.navigation.core",
    "GeneratedDeepLinkHandler"
)

// Change to
private val DEEP_LINK_REGISTRY = ClassName(
    "com.jermey.quo.vadis.core.navigation.core",
    "DeepLinkRegistry"
)
```

#### Task 6.2: Update NavigationConfigGenerator

**File:** `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/dsl/NavigationConfigGenerator.kt`

1. Update import (line 252):
```kotlin
// Before
addImport("com.jermey.quo.vadis.core.navigation.core", "GeneratedDeepLinkHandler")

// After
addImport("com.jermey.quo.vadis.core.navigation.core", "DeepLinkRegistry")
```

2. Update property builder method name and type (lines 475-482):
```kotlin
// Rename method
private fun buildDeepLinkRegistryProperty(hasRoutes: Boolean): PropertySpec {
    val propertyType = DEEP_LINK_REGISTRY_CLASS.copy(nullable = true)
    // ...
}
```

3. Update constant (line 1037):
```kotlin
// Before
val DEEP_LINK_HANDLER_CLASS =
    ClassName("com.jermey.quo.vadis.core.navigation.core", "GeneratedDeepLinkHandler")

// After
val DEEP_LINK_REGISTRY_CLASS =
    ClassName("com.jermey.quo.vadis.core.navigation.core", "DeepLinkRegistry")
```

#### Task 6.3: Update QuoVadisClassNames

**File:** `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/QuoVadisClassNames.kt`

```kotlin
// Before (lines 10, 34)
import com.jermey.quo.vadis.core.navigation.core.GeneratedDeepLinkHandler
val GENERATED_DEEP_LINK_HANDLER: ClassName = GeneratedDeepLinkHandler::class.toClassName()

// After
import com.jermey.quo.vadis.core.navigation.core.DeepLinkRegistry
val DEEP_LINK_REGISTRY: ClassName = DeepLinkRegistry::class.toClassName()
```

---

### Phase 7: Update Tests

#### Task 7.1: Remove Deprecated API Tests

**File:** `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/core/TreeNavigatorTest.kt`

Remove test for `getDeepLinkHandler()` (around line 1018-1023):
```kotlin
// Remove this test
@Test
fun `getDeepLinkHandler returns handler`() {
    val navigator = TreeNavigator()
    val handler = navigator.getDeepLinkHandler()
    assertNotNull(handler)
}
```

#### Task 7.2: Update Any Tests Using Deprecated APIs

Search and update any remaining tests that use:
- `deepLinkHandler`
- `getDeepLinkHandler()`
- `DeepLinkHandler`
- `DefaultDeepLinkHandler`
- `GeneratedDeepLinkHandler`
- `.parameters` on DeepLink

---

### Phase 8: Handle DeepLinkResult

#### Task 8.1: Decide on DeepLinkResult Location

**Current:** `GeneratedDeepLinkHandler.kt` (lines 152-193)

Options:
1. **Keep in separate file** - Move to new `DeepLinkResult.kt` file
2. **Inline in generated code** - Generate the sealed class in each module
3. **Keep in DeepLinkRegistry.kt** - Add to the registry file

**Recommendation:** Move to separate `DeepLinkResult.kt` file in `core` package.

---

## Implementation Order

Execute tasks in this order to minimize compilation errors:

1. **Phase 1** - Update NavigationConfig property (Tasks 1.1-1.4)
2. **Phase 8** - Handle DeepLinkResult (Task 8.1) - Create new file if needed
3. **Phase 6** - Update KSP Generator (Tasks 6.1-6.3)
4. **Phase 2** - Update TreeNavigator (Tasks 2.1-2.2)
5. **Phase 3** - Update Navigator Interface (Task 3.1)
6. **Phase 4** - Update FakeNavigator (Task 4.1)
7. **Phase 5** - Remove Deprecated Types (Tasks 5.1-5.5)
8. **Phase 7** - Update Tests (Tasks 7.1-7.2)

---

## Verification Steps

After completing all tasks:

1. **Build all modules:**
   ```bash
   ./gradlew assembleDebug
   ```

2. **Run tests:**
   ```bash
   ./gradlew test
   ```

3. **Verify demo app:**
   ```bash
   ./gradlew :composeApp:desktopRun
   ```

4. **Search for remaining usages:**
   ```bash
   grep -r "DeepLinkHandler" --include="*.kt" .
   grep -r "getDeepLinkHandler" --include="*.kt" .
   grep -r "\.parameters" --include="*.kt" . | grep -i deeplink
   ```

---

## Breaking Changes Summary

| Change | Impact | Migration |
|--------|--------|-----------|
| `DeepLinkHandler` removed | Code using this interface won't compile | Use `DeepLinkRegistry` |
| `GeneratedDeepLinkHandler` removed | Generated code using this won't compile | Regenerate with KSP |
| `getDeepLinkHandler()` removed | Method calls won't compile | Use `getDeepLinkRegistry()` |
| `DeepLink.parameters` removed | Property access won't compile | Use `queryParams` or `allParams` |
| `NavigationConfig.deepLinkHandler` renamed | Property access won't compile | Use `deepLinkRegistry` |

---

## Version Bump Recommendation

Since this is a breaking change, the version should be bumped according to semver:

- Current version: Check `gradle.properties` or `build.gradle.kts`
- New version: Bump **MAJOR** version (e.g., 1.x.x → 2.0.0)

Alternatively, if still in 0.x.x, bump **MINOR** version as per semver pre-1.0 rules.

---

## Rollback Plan

If issues arise:
1. Revert the commits from this cleanup
2. The deprecated APIs with `WARNING` level will remain functional
3. Plan for a more gradual migration in future release

---

## Related Documents

- [Deep Link API Refactoring Plan](./deeplink-api-refactoring-plan.md) - Original refactoring plan that introduced the new API
- [CHANGELOG.md](../../CHANGELOG.md) - Document breaking changes here

---

## Estimated Effort

| Phase | Tasks | Estimated Time |
|-------|-------|----------------|
| Phase 1 | NavigationConfig updates | 30 min |
| Phase 2 | TreeNavigator updates | 30 min |
| Phase 3-4 | Navigator & FakeNavigator | 20 min |
| Phase 5 | Remove deprecated types | 30 min |
| Phase 6 | KSP Generator updates | 1-2 hours |
| Phase 7 | Test updates | 30 min |
| Phase 8 | DeepLinkResult handling | 15 min |
| **Total** | | **3-4 hours** |

---

## Checklist

- [ ] Phase 1: NavigationConfig property renamed
- [ ] Phase 2: TreeNavigator updated
- [ ] Phase 3: Navigator interface updated  
- [ ] Phase 4: FakeNavigator updated
- [ ] Phase 5: Deprecated types removed
- [ ] Phase 6: KSP Generator updated
- [ ] Phase 7: Tests updated
- [ ] Phase 8: DeepLinkResult relocated
- [ ] All modules compile
- [ ] All tests pass
- [ ] Demo app works correctly
- [ ] No remaining usages of deprecated APIs
- [ ] CHANGELOG.md updated
- [ ] Version bumped

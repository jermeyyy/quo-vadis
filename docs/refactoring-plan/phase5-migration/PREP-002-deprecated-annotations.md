# PREP-002: Add @Deprecated Annotations to Legacy APIs

## Overview

| Attribute | Value |
|-----------|-------|
| **Task ID** | PREP-002 |
| **Complexity** | Medium |
| **Estimated Time** | 2 days |
| **Dependencies** | Phase 4 annotations defined |
| **Output** | All legacy APIs marked `@Deprecated` with `replaceWith` guidance |

## Objective

Mark all legacy navigation APIs with `@Deprecated` annotations to:

1. **Signal Migration** - IDE warnings guide developers to new APIs
2. **Provide Guidance** - `replaceWith` shows the replacement
3. **Enable Cleanup** - Future removal becomes straightforward
4. **Support IDEs** - Automatic migration suggestions where possible

## GitHub Reference Base URL

All permalinks reference the main branch:
```
https://github.com/jermeyyy/quo-vadis/blob/main/
```

## Deprecation Strategy

### Deprecation Level

Use `DeprecationLevel.WARNING` for all deprecations:

```kotlin
@Deprecated(
    message = "Replaced by NavNode-based architecture. See migration guide.",
    replaceWith = ReplaceWith(
        expression = "NewApi",
        imports = ["com.jermey.quo.vadis.core.navigation.newapi"]
    ),
    level = DeprecationLevel.WARNING
)
```

### Message Format

```
"[Component] has been replaced by [NewComponent] in the NavNode architecture. 
See: https://github.com/jermeyyy/quo-vadis/blob/main/docs/migration-examples/"
```

---

## APIs to Deprecate

### Module 1: quo-vadis-core - Core APIs

#### File: [NavigationGraph.kt](https://github.com/jermeyyy/quo-vadis/blob/main/quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/NavigationGraph.kt)

| Symbol | Type | Replacement |
|--------|------|-------------|
| `NavigationGraph` | Interface | `NavNode` hierarchy |
| `DestinationConfig` | Data class | KSP-generated `ScreenRegistry` |
| `NavigationGraphBuilder` | Class | KSP-generated builders |
| `navigationGraph()` | Function | `@Stack`, `@Tab`, `@Pane` annotations |
| `ModuleNavigation` | Interface | Direct `NavNode` composition |
| `BaseModuleNavigation` | Abstract class | Direct `NavNode` composition |

**Deprecation Code**:

```kotlin
@Deprecated(
    message = "NavigationGraph has been replaced by NavNode tree structure. " +
              "Use @Stack, @Tab, or @Pane annotations with KSP-generated builders. " +
              "See: https://github.com/jermeyyy/quo-vadis/blob/main/docs/migration-examples/",
    replaceWith = ReplaceWith("NavNode"),
    level = DeprecationLevel.WARNING
)
interface NavigationGraph { ... }

@Deprecated(
    message = "DestinationConfig is replaced by KSP-generated ScreenRegistry. " +
              "Use @Screen annotation to bind composables to destinations.",
    level = DeprecationLevel.WARNING
)
data class DestinationConfig<T : Destination>(...) { ... }

@Deprecated(
    message = "NavigationGraphBuilder is replaced by @Stack/@Tab/@Pane annotations " +
              "and KSP-generated NavNode builders.",
    level = DeprecationLevel.WARNING
)
class NavigationGraphBuilder { ... }

@Deprecated(
    message = "navigationGraph() DSL is replaced by @Stack annotation. " +
              "Define a sealed class with @Stack and @Destination annotations.",
    replaceWith = ReplaceWith("@Stack"),
    level = DeprecationLevel.WARNING
)
inline fun navigationGraph(...) { ... }
```

---

#### File: [BackStack.kt](https://github.com/jermeyyy/quo-vadis/blob/main/quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/BackStack.kt)

| Symbol | Type | Replacement |
|--------|------|-------------|
| `BackStack` | Interface | `Navigator.state: StateFlow<NavNode>` |
| `BackStackEntry` | Data class | `ScreenNode` in NavNode tree |
| `MutableBackStack` | Class | `TreeMutator` operations |
| `EXTRA_SELECTED_TAB_ROUTE` | Constant | `TabNode.activeStackIndex` |

**Deprecation Code**:

```kotlin
@Deprecated(
    message = "BackStack interface is replaced by NavNode tree state. " +
              "Access navigation state via Navigator.state: StateFlow<NavNode>.",
    replaceWith = ReplaceWith("Navigator.state"),
    level = DeprecationLevel.WARNING
)
interface BackStack { ... }

@Deprecated(
    message = "BackStackEntry is replaced by ScreenNode in the NavNode tree. " +
              "Navigation entries are now nodes in a tree structure.",
    replaceWith = ReplaceWith("ScreenNode"),
    level = DeprecationLevel.WARNING
)
data class BackStackEntry(...) { ... }

@Deprecated(
    message = "MutableBackStack is replaced by TreeMutator operations on NavNode. " +
              "Use Navigator methods like navigate(), navigateBack(), etc.",
    level = DeprecationLevel.WARNING
)
class MutableBackStack : BackStack { ... }
```

---

#### File: [Destination.kt](https://github.com/jermeyyy/quo-vadis/blob/main/quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/Destination.kt)

| Symbol | Type | Replacement |
|--------|------|-------------|
| `TypedDestination<T>` | Interface | Route templates `{param}` |
| `RestoredTypedDestination` | Data class | State restoration via `NavNode` |
| `BasicDestination` | Data class | Sealed class members |

**Deprecation Code**:

```kotlin
@Deprecated(
    message = "TypedDestination<T> is replaced by route templates. " +
              "Use @Destination(route = \"path/{param}\") with data class properties. " +
              "Access params directly on the destination instance.",
    level = DeprecationLevel.WARNING
)
interface TypedDestination<T : Any> : Destination { ... }

@Deprecated(
    message = "RestoredTypedDestination is replaced by NavNode state serialization. " +
              "Destination restoration is handled automatically by the NavNode tree.",
    level = DeprecationLevel.WARNING
)
data class RestoredTypedDestination<T : Any>(...) { ... }
```

---

#### File: [TabDefinition.kt](https://github.com/jermeyyy/quo-vadis/blob/main/quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TabDefinition.kt)

| Symbol | Type | Replacement |
|--------|------|-------------|
| `TabDefinition` | Interface | `@TabItem` annotation |
| `TabNavigatorConfig` | Data class | `@Tab` + `@TabItem` annotations |

**Deprecation Code**:

```kotlin
@Deprecated(
    message = "TabDefinition is replaced by @TabItem annotation. " +
              "Define tabs using @Tab on a sealed class and @TabItem on each tab destination.",
    replaceWith = ReplaceWith("@TabItem"),
    level = DeprecationLevel.WARNING
)
interface TabDefinition { ... }

@Deprecated(
    message = "TabNavigatorConfig is replaced by @Tab annotation. " +
              "Use @Tab(name, initialTab) on a sealed class with @TabItem destinations.",
    replaceWith = ReplaceWith("@Tab"),
    level = DeprecationLevel.WARNING
)
data class TabNavigatorConfig<T : TabDefinition>(...) { ... }
```

---

#### File: [TypedDestinations.kt](https://github.com/jermeyyy/quo-vadis/blob/main/quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TypedDestinations.kt)

| Symbol | Type | Replacement |
|--------|------|-------------|
| `typedDestination` | Function | `@Destination(route)` with params |
| `typedDestinationWithScopes` | Function | `@Screen` with scopes |

**Deprecation Code**:

```kotlin
@Deprecated(
    message = "typedDestination DSL is replaced by @Destination annotation. " +
              "Define a data class destination with @Destination(route = \"path/{param}\").",
    level = DeprecationLevel.WARNING
)
inline fun <reified T : Destination, reified D : Any> NavigationGraphBuilder.typedDestination(...) { ... }
```

---

#### File: [Navigator.kt](https://github.com/jermeyyy/quo-vadis/blob/main/quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/Navigator.kt)

| Symbol | Type | Replacement | Notes |
|--------|------|-------------|-------|
| `registerGraph()` | Method | KSP-generated tree | Remove call |
| `setStartDestination()` | Method | Tree has start dest | Remove call |
| `navigateAndClearTo()` (string) | Method | `navigateAndClear(clearUpTo::class)` | Type-safe |

**Deprecation Code**:

```kotlin
@Deprecated(
    message = "registerGraph() is no longer needed. " +
              "Use rememberNavigator(navTree) with KSP-generated tree.",
    level = DeprecationLevel.WARNING
)
fun registerGraph(graph: NavigationGraph)

@Deprecated(
    message = "setStartDestination() is no longer needed. " +
              "Start destination is defined in @Stack/@Tab/@Pane annotations.",
    level = DeprecationLevel.WARNING
)
fun setStartDestination(destination: Destination)

@Deprecated(
    message = "navigateAndClearTo with string route is replaced by type-safe version. " +
              "Use navigateAndClear(destination, clearUpTo::class, inclusive).",
    replaceWith = ReplaceWith("navigateAndClear(destination, clearUpTo, inclusive)"),
    level = DeprecationLevel.WARNING
)
fun navigateAndClearTo(destination: Destination, upToRoute: String, inclusive: Boolean)
```

---

#### File: [TabNavigatorState.kt](https://github.com/jermeyyy/quo-vadis/blob/main/quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TabNavigatorState.kt)

| Symbol | Type | Replacement |
|--------|------|-------------|
| `TabNavigatorState` | Class | `TabNode` in NavNode tree |

**Deprecation Code**:

```kotlin
@Deprecated(
    message = "TabNavigatorState is replaced by TabNode in the NavNode tree. " +
              "Tab state is managed automatically within the unified navigator.",
    replaceWith = ReplaceWith("TabNode"),
    level = DeprecationLevel.WARNING
)
class TabNavigatorState<T : TabDefinition>(...) { ... }
```

---

#### File: [TabScopedNavigator.kt](https://github.com/jermeyyy/quo-vadis/blob/main/quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TabScopedNavigator.kt)

| Symbol | Type | Replacement |
|--------|------|-------------|
| `TabScopedNavigator` | Class | Single unified `Navigator` |

**Deprecation Code**:

```kotlin
@Deprecated(
    message = "TabScopedNavigator is replaced by unified Navigator. " +
              "A single Navigator handles all navigation including tabs. " +
              "Use navigator.switchTab(tabDestination) for tab switching.",
    level = DeprecationLevel.WARNING
)
class TabScopedNavigator<T : TabDefinition>(...) : Navigator { ... }
```

---

### Module 2: quo-vadis-core - Compose APIs

#### File: [NavHost.kt](https://github.com/jermeyyy/quo-vadis/blob/main/quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/NavHost.kt)

| Symbol | Type | Replacement |
|--------|------|-------------|
| `NavHost` | Composable | `QuoVadisHost` |

**Deprecation Code**:

```kotlin
@Deprecated(
    message = "NavHost is replaced by QuoVadisHost. " +
              "Use QuoVadisHost(navigator, screenRegistry) for all navigation types.",
    replaceWith = ReplaceWith(
        "QuoVadisHost(navigator, screenRegistry)",
        "com.jermey.quo.vadis.core.navigation.compose.QuoVadisHost"
    ),
    level = DeprecationLevel.WARNING
)
@Composable
fun NavHost(...) { ... }
```

---

#### File: [GraphNavHost.kt](https://github.com/jermeyyy/quo-vadis/blob/main/quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/GraphNavHost.kt)

| Symbol | Type | Replacement |
|--------|------|-------------|
| `GraphNavHost` | Composable | `QuoVadisHost` |
| `LocalBackStackEntry` | CompositionLocal | `LocalScreenNode` |
| `currentBackStackEntry()` | Function | Access via `LocalScreenNode` |
| `rememberNavigator()` (no args) | Function | `rememberNavigator(navTree)` |

**Deprecation Code**:

```kotlin
@Deprecated(
    message = "GraphNavHost is replaced by QuoVadisHost. " +
              "Use QuoVadisHost(navigator, screenRegistry, animationRegistry).",
    replaceWith = ReplaceWith(
        "QuoVadisHost(navigator = navigator, screenRegistry = screenRegistry)",
        "com.jermey.quo.vadis.core.navigation.compose.QuoVadisHost"
    ),
    level = DeprecationLevel.WARNING
)
@Composable
fun GraphNavHost(...) { ... }

@Deprecated(
    message = "LocalBackStackEntry is replaced by LocalScreenNode. " +
              "Access the current screen via LocalScreenNode.current.",
    replaceWith = ReplaceWith("LocalScreenNode"),
    level = DeprecationLevel.WARNING
)
val LocalBackStackEntry = compositionLocalOf<BackStackEntry?> { null }

@Deprecated(
    message = "rememberNavigator() without arguments is deprecated. " +
              "Use rememberNavigator(navTree) with KSP-generated NavNode tree.",
    replaceWith = ReplaceWith("rememberNavigator(navTree)"),
    level = DeprecationLevel.WARNING
)
@Composable
fun rememberNavigator(): Navigator { ... }
```

---

#### File: [TabbedNavHost.kt](https://github.com/jermeyyy/quo-vadis/blob/main/quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/TabbedNavHost.kt)

| Symbol | Type | Replacement |
|--------|------|-------------|
| `TabbedNavHost` | Composable | `QuoVadisHost` + `tabWrapper` |

**Deprecation Code**:

```kotlin
@Deprecated(
    message = "TabbedNavHost is replaced by QuoVadisHost with tabWrapper parameter. " +
              "Use QuoVadisHost(navigator, screenRegistry, tabWrapper = { tabNode, content -> ... }).",
    replaceWith = ReplaceWith(
        "QuoVadisHost(navigator, screenRegistry, tabWrapper = { tabNode, content -> /* your tab UI */ })",
        "com.jermey.quo.vadis.core.navigation.compose.QuoVadisHost"
    ),
    level = DeprecationLevel.WARNING
)
@Composable
fun <T : TabDefinition> TabbedNavHost(...) { ... }
```

---

#### File: [TabNavHostComposables.kt](https://github.com/jermeyyy/quo-vadis/blob/main/quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/TabNavHostComposables.kt)

| Symbol | Type | Replacement |
|--------|------|-------------|
| `rememberTabNavigatorState()` | Function | NavNode tree state |
| `rememberTabNavigator()` | Function | `rememberNavigator(navTree)` |

**Deprecation Code**:

```kotlin
@Deprecated(
    message = "rememberTabNavigatorState() is replaced by NavNode tree state management. " +
              "Tab state is automatically managed within the unified NavNode tree.",
    level = DeprecationLevel.WARNING
)
@Composable
fun <T : TabDefinition> rememberTabNavigatorState(...): TabNavigatorState<T> { ... }

@Deprecated(
    message = "rememberTabNavigator() is replaced by unified rememberNavigator(navTree). " +
              "Use @Tab annotation and KSP-generated NavNode tree.",
    replaceWith = ReplaceWith("rememberNavigator(navTree)"),
    level = DeprecationLevel.WARNING
)
@Composable
fun <T : TabDefinition> rememberTabNavigator(...): Navigator { ... }
```

---

### Module 3: quo-vadis-annotations

#### File: [Annotations.kt](https://github.com/jermeyyy/quo-vadis/blob/main/quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/Annotations.kt)

| Symbol | Type | Replacement |
|--------|------|-------------|
| `@Graph` | Annotation | `@Stack`, `@Tab`, `@Pane` |
| `@Route` | Annotation | `@Destination` |
| `@Argument` | Annotation | Route templates `{param}` |
| `@Content` | Annotation | `@Screen` |

**Deprecation Code**:

```kotlin
@Deprecated(
    message = "@Graph is replaced by container-type-specific annotations: " +
              "@Stack for linear navigation, @Tab for tabbed navigation, @Pane for adaptive layouts.",
    replaceWith = ReplaceWith("@Stack"),
    level = DeprecationLevel.WARNING
)
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Graph(val name: String, val startDestination: String)

@Deprecated(
    message = "@Route is replaced by @Destination. " +
              "Use @Destination(route = \"path/{param}\") for routes with parameters.",
    replaceWith = ReplaceWith("@Destination", "com.jermey.quo.vadis.annotations.Destination"),
    level = DeprecationLevel.WARNING
)
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Route(val path: String)

@Deprecated(
    message = "@Argument is no longer needed. " +
              "Use route templates @Destination(route = \"path/{param}\") and data class properties. " +
              "Arguments are accessed directly on the destination instance.",
    level = DeprecationLevel.WARNING
)
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Argument(val dataClass: KClass<out Any>)

@Deprecated(
    message = "@Content is replaced by @Screen. " +
              "Use @Screen(DestinationClass::class) to bind composables to destinations.",
    replaceWith = ReplaceWith("@Screen", "com.jermey.quo.vadis.annotations.Screen"),
    level = DeprecationLevel.WARNING
)
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Content(val destination: KClass<out Destination>)
```

---

## Implementation Checklist

### Phase A: Annotation Module (Day 1 Morning)

- [ ] Add `@Deprecated` to `@Graph` annotation
- [ ] Add `@Deprecated` to `@Route` annotation
- [ ] Add `@Deprecated` to `@Argument` annotation
- [ ] Add `@Deprecated` to `@Content` annotation
- [ ] Verify annotation module compiles

### Phase B: Core Module - Core Package (Day 1 Afternoon)

- [ ] Deprecate `NavigationGraph.kt` symbols
- [ ] Deprecate `BackStack.kt` symbols
- [ ] Deprecate `Destination.kt` TypedDestination
- [ ] Deprecate `TabDefinition.kt` symbols
- [ ] Deprecate `TypedDestinations.kt` functions
- [ ] Deprecate `TabNavigatorState.kt`
- [ ] Deprecate `TabScopedNavigator.kt`
- [ ] Update `Navigator.kt` method deprecations

### Phase C: Core Module - Compose Package (Day 2 Morning)

- [ ] Deprecate `NavHost.kt`
- [ ] Deprecate `GraphNavHost.kt` symbols
- [ ] Deprecate `TabbedNavHost.kt`
- [ ] Deprecate `TabNavHostComposables.kt` functions

### Phase D: Verification (Day 2 Afternoon)

- [ ] Full project compiles: `./gradlew build`
- [ ] Demo app shows deprecation warnings
- [ ] Warnings include migration guidance
- [ ] No errors introduced

---

## Deprecation Message Guidelines

### Required Elements

1. **What's deprecated** - Clear identification
2. **What replaces it** - The new API
3. **Migration link** - URL to documentation
4. **ReplaceWith** - When IDE auto-fix is possible

### Example Template

```kotlin
@Deprecated(
    message = "[OldApi] is replaced by [NewApi]. " +
              "[Brief explanation of why/what changed]. " +
              "See: https://github.com/jermeyyy/quo-vadis/blob/main/docs/migration-examples/",
    replaceWith = ReplaceWith(
        expression = "NewApi(params)",
        imports = ["com.jermey.quo.vadis.newpackage.NewApi"]
    ),
    level = DeprecationLevel.WARNING
)
```

---

## Acceptance Criteria

- [ ] All legacy APIs marked with `@Deprecated`
- [ ] All deprecations have meaningful messages
- [ ] `replaceWith` provided where applicable
- [ ] Messages include GitHub permalink
- [ ] Project compiles with warnings only
- [ ] IDE shows migration hints
- [ ] Demo app triggers all deprecation warnings

## Related Tasks

- [PREP-003](./PREP-003-permalink-reference.md) - GitHub permalink reference document
- [MIG-008](./MIG-007-api-change-summary.md) - API change summary with deprecation mapping

# MIG-008: API Change Summary Document

## Overview

| Attribute | Value |
|-----------|-------|
| **Task ID** | MIG-008 |
| **Complexity** | Low |
| **Estimated Time** | 0.5 days |
| **Dependencies** | None |
| **Output** | `docs/migration-examples/API-CHANGES.md` |

## Objective

Create a comprehensive quick-reference document summarizing all API changes from the old navigation system to the new NavNode architecture.

## Document Structure

The API Change Summary should be organized as follows:

---

# Quo Vadis API Changes: v1 → v2

> **Important**: Version 2 is a breaking change. The library is in development stage - no backward compatibility is maintained.

## Quick Reference Table

### Annotations

| Old Annotation | New Annotation | Notes |
|---------------|----------------|-------|
| `@Graph("name", startDestination = "route")` | `@Stack(name = "name", startDestination = "ClassName")` | Container type explicit |
| `@Route("path")` | `@Destination(route = "path")` | Supports `{param}` templates |
| `@Argument(Data::class)` | *(removed)* | Use route template instead |
| `@Content(Dest::class)` | `@Screen(Dest::class)` | Renamed for clarity |
| `@TabGraph(...)` | `@Tab(name, initialTab)` | New declarative tabs |
| *(new)* | `@TabItem(label, icon, rootGraph)` | Tab metadata |
| *(new)* | `@Pane(name, backBehavior)` | Adaptive layouts |
| *(new)* | `@PaneItem(role, adaptStrategy, rootGraph)` | Pane metadata |

### Destination Definitions

| Old Pattern | New Pattern |
|-------------|-------------|
| `sealed class X : Destination` | `sealed class X : Destination` *(unchanged)* |
| `data object Y : X()` | `data object Y : X()` *(unchanged)* |
| `data class Z(val id: String) : X(), TypedDestination<Data>` | `data class Z(val id: String) : X()` |

### Navigation Hosts

| Old Component | New Component | Notes |
|---------------|---------------|-------|
| `GraphNavHost(graph, navigator, ...)` | `QuoVadisHost(navigator, screenRegistry, ...)` | Single unified host |
| `TabbedNavHost(tabState, tabGraphs, tabUI)` | `QuoVadisHost(..., tabWrapper = {...})` | Tab UI via wrapper |
| `NavHost(navigator, ...)` | `QuoVadisHost(...)` | Unified |

### Navigator API

| Old Method | New Method | Notes |
|------------|------------|-------|
| `navigator.backStack` | `navigator.state` | `StateFlow<NavNode>` |
| `navigator.navigate(dest)` | `navigator.navigate(dest)` | *(unchanged)* |
| `navigator.navigate(dest, transition)` | `navigator.navigate(dest)` | Transition via registry |
| `navigator.navigateBack()` | `navigator.navigateBack()` | *(unchanged)* |
| `navigator.popTo(dest, inclusive)` | `navigator.popTo(dest, inclusive)` | Type-safe dest |
| `navigator.navigateAndClearTo(dest, route, inclusive)` | `navigator.navigateAndClear(dest, clearUpTo, inclusive)` | Class ref not string |
| `tabState.switchTab(tab)` | `navigator.switchTab(tab)` | Single navigator |
| *(new)* | `navigator.exitFlow(DestClass)` | Exit multi-step flow |
| `navigator.registerGraph(graph)` | *(removed)* | KSP generates tree |
| `navigator.setStartDestination(dest)` | *(removed)* | Tree has start dest |

### Setup & Initialization

| Old Pattern | New Pattern |
|-------------|-------------|
| `initializeQuoVadisRoutes()` | *(removed)* |
| `val graph = remember { appGraph() }` | `val navTree = remember { buildAppNavNode() }` |
| `val navigator = rememberNavigator()` | `val navigator = rememberNavigator(navTree)` |
| `LaunchedEffect { navigator.registerGraph(...) }` | *(removed)* |

### Typed Arguments

| Old Pattern | New Pattern |
|-------------|-------------|
| `@Argument(ArticleData::class)` | *(removed)* |
| `TypedDestination<ArticleData>` | *(removed)* |
| `override val data = ArticleData(id)` | *(removed)* |
| `fun Content(data: ArticleData, nav)` | `fun Screen(dest: Article, nav)` |
| `data.articleId` | `destination.articleId` |
| `"article"` (static route) | `"article/{articleId}"` (template) |

### Transitions

| Old Pattern | New Pattern |
|-------------|-------------|
| `navigate(dest, NavigationTransitions.Slide)` | `AnimationRegistry { from(A).to(B).uses(Slide) }` |
| `GraphNavHost(defaultTransition = ...)` | `QuoVadisHost(animationRegistry = ...)` |
| Per-call transition specification | Centralized transition registry |

### Tab Navigation

| Old Pattern | New Pattern |
|-------------|-------------|
| `TabbedNavigatorConfig` object | `@Tab` + `@TabItem` annotations |
| `rememberTabNavigator(config, ...)` | *(removed)* - state in NavNode tree |
| `TabbedNavHost(tabState, tabGraphs, tabUI)` | `QuoVadisHost(tabWrapper = { node, content -> ... })` |
| `tabState.activeTab` | `tabNode.activeStackIndex` |
| `tabState.switchTab(tab)` | `navigator.switchTab(tab)` |

### Shared Elements

| Old Pattern | New Pattern |
|-------------|-------------|
| Manual `SharedTransitionLayout` wrapper | Built-in to `QuoVadisHost` |
| Pass `SharedTransitionScope` through params | `LocalSharedTransitionScope.current` |
| Limited to single host boundary | Works across entire app |

### Predictive Back

| Old Pattern | New Pattern |
|-------------|-------------|
| `GraphNavHost(enablePredictiveBack = true)` | Built-in to `QuoVadisHost` |
| Per-host back handling | Unified speculative pop |
| Manual `BackHandler` coordination | Automatic |

---

## Migration Checklist

### Per-File Checklist

- [ ] Replace `@Graph` with `@Stack`, `@Tab`, or `@Pane`
- [ ] Replace `@Route` with `@Destination`
- [ ] Remove `@Argument` annotations
- [ ] Remove `TypedDestination<T>` implementations
- [ ] Replace `@Content` with `@Screen`
- [ ] Update function parameter from `data: T` to `destination: Dest`

### Per-App Checklist

- [ ] Replace `GraphNavHost` with `QuoVadisHost`
- [ ] Replace `TabbedNavHost` with `tabWrapper` parameter
- [ ] Add `screenRegistry` parameter (KSP-generated)
- [ ] Create `AnimationRegistry` for transitions
- [ ] Remove `initializeQuoVadisRoutes()` call
- [ ] Remove `registerGraph()` call
- [ ] Remove `setStartDestination()` call
- [ ] Update `rememberNavigator()` to pass initial tree

### Navigation Call Checklist

- [ ] Remove transition parameter from `navigate()`
- [ ] Update `navigateAndClearTo()` to `navigateAndClear()`
- [ ] Use class references instead of string routes
- [ ] Replace `tabState.switchTab()` with `navigator.switchTab()`

---

## Error Messages & Fixes

| Compilation Error | Likely Cause | Fix |
|-------------------|--------------|-----|
| `Unresolved reference: Graph` | Old annotation | Use `@Stack`, `@Tab`, or `@Pane` |
| `Unresolved reference: Route` | Old annotation | Use `@Destination` |
| `Unresolved reference: Content` | Old annotation | Use `@Screen` |
| `Unresolved reference: GraphNavHost` | Old host | Use `QuoVadisHost` |
| `Unresolved reference: TabbedNavHost` | Old host | Use `QuoVadisHost` with `tabWrapper` |
| `TypedDestination has no type argument` | Removed interface | Delete `TypedDestination<T>` |
| `Property 'data' must be initialized` | Removed pattern | Access props on destination directly |
| `Unresolved reference: backStack` | Property renamed | Use `navigator.state` |
| `Unresolved reference: initializeQuoVadisRoutes` | Removed function | Delete the call |

---

## KSP Generated Files

After migration, KSP generates these files:

| Generated File | Purpose |
|----------------|---------|
| `build{Name}NavNode()` | Creates initial NavNode tree for each container |
| `GeneratedScreenRegistry` | Maps destinations to `@Screen` composables |
| `GeneratedDeepLinkHandler` | Parses URIs to destinations |
| `{Name}TabsMetadata` | Tab labels, icons, indices |
| `NavigatorExtensions.kt` | Convenience navigation methods |

---

## Acceptance Criteria

- [ ] All annotation changes documented in table format
- [ ] All Navigator API changes documented
- [ ] Setup/initialization changes clear
- [ ] Tab navigation migration path clear
- [ ] Common error messages with fixes listed
- [ ] Checklist format for easy migration tracking
- [ ] KSP generated files explained

## Related Tasks

- [MIG-001](./MIG-001-simple-stack-example.md) through [MIG-007](./MIG-007-demo-app-rewrite.md) - Detailed examples
- [ANN-001](../phase4-annotations/ANN-001-graph-type.md) through [ANN-005](../phase4-annotations/ANN-005-screen.md) - Annotation definitions

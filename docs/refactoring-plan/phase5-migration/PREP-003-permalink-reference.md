# PREP-003: Create GitHub Permalink Reference Document

## Overview

| Attribute | Value |
|-----------|-------|
| **Task ID** | PREP-003 |
| **Complexity** | Low |
| **Estimated Time** | 0.5 days |
| **Dependencies** | None |
| **Output** | `docs/migration-examples/LEGACY_API_REFERENCE.md` |

## Objective

Create a centralized reference document that links all legacy APIs to their stable main branch locations on GitHub. This document serves as:

1. **Migration Reference** - Quick lookup for legacy API locations
2. **LLM Context** - Stable URLs for understanding "migrating from" code
3. **Version Control** - Main branch permalinks ensure consistent references

## Base URL

All permalinks use the main branch:
```
https://github.com/jermeyyy/quo-vadis/blob/main/
```

## Document Location

```
docs/
└── migration-examples/
    └── LEGACY_API_REFERENCE.md
```

## Document Content

Create the following markdown document:

```markdown
# Legacy API Reference (Main Branch)

> **Note**: This document provides stable GitHub permalinks to all legacy APIs
> in the Quo Vadis navigation library. These links reference the `main` branch
> and serve as the authoritative "migrating from" reference.

## Base URL

All paths are relative to:
```
https://github.com/jermeyyy/quo-vadis/blob/main/
```

---

## Core Navigation APIs

### Navigation Graph

| File | Description |
|------|-------------|
| [NavigationGraph.kt](https://github.com/jermeyyy/quo-vadis/blob/main/quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/NavigationGraph.kt) | Graph definition, builder DSL |

**Key Symbols**:
- `NavigationGraph` - Interface for navigation graph
- `NavigationGraphBuilder` - DSL builder class
- `navigationGraph()` - Top-level DSL function
- `DestinationConfig` - Destination configuration
- `ModuleNavigation` - Module integration interface

---

### Back Stack

| File | Description |
|------|-------------|
| [BackStack.kt](https://github.com/jermeyyy/quo-vadis/blob/main/quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/BackStack.kt) | Back stack management |

**Key Symbols**:
- `BackStack` - Interface for stack operations
- `BackStackEntry` - Single entry in the stack
- `MutableBackStack` - Mutable implementation

---

### Destinations

| File | Description |
|------|-------------|
| [Destination.kt](https://github.com/jermeyyy/quo-vadis/blob/main/quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/Destination.kt) | Destination interfaces |

**Key Symbols**:
- `Destination` - Base destination interface
- `TypedDestination<T>` - Typed argument interface
- `RestoredTypedDestination` - Restoration data class

---

### Navigator

| File | Description |
|------|-------------|
| [Navigator.kt](https://github.com/jermeyyy/quo-vadis/blob/main/quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/Navigator.kt) | Core navigator interface |

**Key Symbols**:
- `Navigator` - Main navigation interface
- `navigate()`, `navigateBack()`, `navigateUp()`
- `navigateAndClearTo()`, `navigateAndReplace()`
- `registerGraph()`, `setStartDestination()`

---

### Tab Navigation

| File | Description |
|------|-------------|
| [TabDefinition.kt](https://github.com/jermeyyy/quo-vadis/blob/main/quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TabDefinition.kt) | Tab configuration |
| [TabNavigatorState.kt](https://github.com/jermeyyy/quo-vadis/blob/main/quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TabNavigatorState.kt) | Tab state management |
| [TabScopedNavigator.kt](https://github.com/jermeyyy/quo-vadis/blob/main/quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TabScopedNavigator.kt) | Tab-scoped navigator |

**Key Symbols**:
- `TabDefinition` - Tab configuration interface
- `TabNavigatorConfig` - Tab navigator setup
- `TabNavigatorState` - State holder class
- `TabScopedNavigator` - Per-tab navigator

---

### Typed Destinations

| File | Description |
|------|-------------|
| [TypedDestinations.kt](https://github.com/jermeyyy/quo-vadis/blob/main/quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TypedDestinations.kt) | Typed destination DSL |

**Key Symbols**:
- `typedDestination()` - DSL for typed destinations
- `typedDestinationWithScopes()` - With transition scopes

---

## Compose Integration APIs

### Navigation Hosts

| File | Description |
|------|-------------|
| [NavHost.kt](https://github.com/jermeyyy/quo-vadis/blob/main/quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/NavHost.kt) | Basic nav host |
| [GraphNavHost.kt](https://github.com/jermeyyy/quo-vadis/blob/main/quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/GraphNavHost.kt) | Graph-based nav host |
| [TabbedNavHost.kt](https://github.com/jermeyyy/quo-vadis/blob/main/quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/TabbedNavHost.kt) | Tabbed navigation host |

**Key Symbols**:
- `NavHost` - Simple composable host
- `GraphNavHost` - Graph-aware host with transitions
- `TabbedNavHost` - Tab navigation host
- `LocalBackStackEntry` - Composition local

---

### Tab Composables

| File | Description |
|------|-------------|
| [TabNavHostComposables.kt](https://github.com/jermeyyy/quo-vadis/blob/main/quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/TabNavHostComposables.kt) | Tab-related composables |

**Key Symbols**:
- `rememberTabNavigatorState()` - State holder
- `rememberTabNavigator()` - Navigator factory

---

### Navigator Composables

| File | Description |
|------|-------------|
| [GraphNavHost.kt (rememberNavigator)](https://github.com/jermeyyy/quo-vadis/blob/main/quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/GraphNavHost.kt) | Navigator factory |

**Key Symbols**:
- `rememberNavigator()` - Creates/remembers navigator

---

## Annotation APIs

### Legacy Annotations

| File | Description |
|------|-------------|
| [Annotations.kt](https://github.com/jermeyyy/quo-vadis/blob/main/quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/Annotations.kt) | All legacy annotations |

**Key Symbols**:
- `@Graph` - Graph definition annotation
- `@Route` - Route path annotation
- `@Argument` - Typed argument annotation
- `@Content` - Content binding annotation

---

### New Annotations (Reference)

| File | Description |
|------|-------------|
| [Stack.kt](https://github.com/jermeyyy/quo-vadis/blob/main/quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/Stack.kt) | Stack container |
| [Destination.kt](https://github.com/jermeyyy/quo-vadis/blob/main/quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/Destination.kt) | Destination definition |
| [Screen.kt](https://github.com/jermeyyy/quo-vadis/blob/main/quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/Screen.kt) | Screen binding |
| [Tab.kt](https://github.com/jermeyyy/quo-vadis/blob/main/quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/Tab.kt) | Tab navigation |

---

## Demo App (Usage Examples)

### Entry Point

| File | Description |
|------|-------------|
| [App.kt](https://github.com/jermeyyy/quo-vadis/blob/main/composeApp/src/commonMain/kotlin/com/jermey/navplayground/App.kt) | Main app entry |
| [DemoApp.kt](https://github.com/jermeyyy/quo-vadis/blob/main/composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/DemoApp.kt) | Demo setup |

---

### Destinations

| File | Description |
|------|-------------|
| [destinations/](https://github.com/jermeyyy/quo-vadis/tree/main/composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/destinations) | All destination definitions |
| [AppDestination.kt](https://github.com/jermeyyy/quo-vadis/blob/main/composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/destinations/AppDestination.kt) | Root destinations |
| [MainTabsDestination.kt](https://github.com/jermeyyy/quo-vadis/blob/main/composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/destinations/MainTabsDestination.kt) | Tab destinations |
| [HomeDestination.kt](https://github.com/jermeyyy/quo-vadis/blob/main/composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/destinations/HomeDestination.kt) | Home tab graph |

---

### Screens

| File | Description |
|------|-------------|
| [ui/screens/](https://github.com/jermeyyy/quo-vadis/tree/main/composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens) | All screen composables |
| [TabsScreens.kt](https://github.com/jermeyyy/quo-vadis/blob/main/composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/TabsScreens.kt) | Tab screen examples |
| [ProcessScreens.kt](https://github.com/jermeyyy/quo-vadis/blob/main/composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/ProcessScreens.kt) | Wizard/process screens |

---

### Navigation Graphs

| File | Description |
|------|-------------|
| [graphs/](https://github.com/jermeyyy/quo-vadis/tree/main/composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/graphs) | Graph definitions |

---

## Deep Linking

| File | Description |
|------|-------------|
| [DeepLink.kt](https://github.com/jermeyyy/quo-vadis/blob/main/quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/DeepLink.kt) | Deep link handling |
| [DeepLinkDemoScreen.kt](https://github.com/jermeyyy/quo-vadis/blob/main/composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/DeepLinkDemoScreen.kt) | Demo usage |

---

## Migration Mapping Quick Reference

| Old API | New API | Reference |
|---------|---------|-----------|
| `@Graph` | `@Stack`, `@Tab`, `@Pane` | [Stack.kt](https://github.com/jermeyyy/quo-vadis/blob/main/quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/Stack.kt) |
| `@Route` | `@Destination` | [Destination.kt](https://github.com/jermeyyy/quo-vadis/blob/main/quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/Destination.kt) |
| `@Content` | `@Screen` | [Screen.kt](https://github.com/jermeyyy/quo-vadis/blob/main/quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/Screen.kt) |
| `@Argument` | Route templates | N/A - use `{param}` in route |
| `NavigationGraph` | `NavNode` | TBD |
| `BackStack` | `Navigator.state` | TBD |
| `GraphNavHost` | `QuoVadisHost` | TBD |
| `TabbedNavHost` | `QuoVadisHost` + `tabWrapper` | TBD |

---

## Usage Notes for LLM Agents

When referencing legacy code:
1. Use the main branch URLs provided above
2. These URLs are stable and won't change during migration
3. Compare legacy patterns with recipes in `quo-vadis-recipes/`
4. The demo app shows real-world usage of legacy APIs
```

## Implementation Steps

### Step 1: Create Directory

```bash
mkdir -p docs/migration-examples
```

### Step 2: Create Document

Create `LEGACY_API_REFERENCE.md` with the content above.

### Step 3: Verify Links

Manually verify a sample of GitHub links work correctly:
- Open 3-4 random links to confirm they resolve
- Ensure main branch is correctly referenced

## Acceptance Criteria

- [ ] Document created at `docs/migration-examples/LEGACY_API_REFERENCE.md`
- [ ] All major legacy API files linked
- [ ] Links use main branch (not commit SHA)
- [ ] Organized by module/category
- [ ] Migration mapping table included
- [ ] LLM usage notes included
- [ ] Sample links verified working

## Related Tasks

- [PREP-002](./PREP-002-deprecated-annotations.md) - Uses these links in deprecation messages
- [MIG-001](./MIG-001-simple-stack-example.md) through [MIG-006](./MIG-006-deep-linking-recipe.md) - Reference these links
